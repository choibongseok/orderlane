package orderlane.app.consumer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import orderlane.app.BUSINESS_ZONE
import orderlane.app.OrderlaneProperties
import orderlane.app.outbox.OutboxRelay
import orderlane.app.web.ApiException
import orderlane.domain.ConsumeDecision
import orderlane.domain.Consumption
import orderlane.domain.RetryPolicy
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.TopicBuilder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Configuration
class TopicConfig {
    @Bean fun eventsTopic(props: OrderlaneProperties): NewTopic = TopicBuilder.name(props.outbox.topic).partitions(3).replicas(1).build()
}

/** 소비자 실패 주입(시연·시험용). orderlane.faults.enabled=true 일 때만 동작한다. */
@Component
class FaultInjector(private val props: OrderlaneProperties) {
    private val remaining = ConcurrentHashMap<String, AtomicInteger>()

    fun arm(consumer: String, times: Int) {
        if (!props.faults.enabled) throw ApiException(404, "FAULTS_DISABLED", "실패 주입이 꺼져 있다")
        remaining[consumer] = AtomicInteger(times)
    }

    fun clear() = remaining.clear()

    fun maybeFail(consumer: String) {
        val r = remaining[consumer] ?: return
        if (r.getAndUpdate { if (it > 0) it - 1 else 0 } > 0) throw IllegalStateException("주입된 실패 consumer=$consumer")
    }
}

/** 이벤트 한 건을 소비자 하나가 처리한다. 중복 제거 기록과 효과(조회 모델·알림·원장)는 같은 트랜잭션이다. */
@Component
class EventProcessor(
    private val jdbc: JdbcClient,
    private val tx: TransactionTemplate,
    private val mapper: ObjectMapper,
    private val faults: FaultInjector,
    private val meters: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(consumer: String, env: JsonNode) {
        val eventId = UUID.fromString(env["eventId"].asText())
        val type = env["type"].asText()
        val data = env["data"]
        tx.executeWithoutResult {
            val inserted = jdbc.sql("insert into processed_event (consumer, event_id) values (?,?) on conflict do nothing").params(consumer, eventId).update()
            if (Consumption.onArrival(inserted == 0) == ConsumeDecision.SkipDuplicate) {
                log.info("이미 처리한 이벤트라 건너뜀 consumer={} eventId={} type={}", consumer, eventId, type)
                meters.counter("orderlane.events.consumed", "consumer", consumer, "result", "duplicate").increment()
                return@executeWithoutResult
            }
            faults.maybeFail(consumer)
            when (consumer) {
                "kitchen" -> kitchen(type, data)
                "notification" -> notification(eventId, type, data)
                "ledger" -> ledger(eventId, type, data)
            }
            meters.counter("orderlane.events.consumed", "consumer", consumer, "result", "processed").increment()
            log.info("이벤트 처리 consumer={} eventId={} type={}", consumer, eventId, type)
        }
    }

    private fun kitchen(type: String, d: JsonNode) {
        val orderId = d["orderId"].asLong()
        val storeId = d["storeId"].asLong()
        when (type) {
            "ORDER_PLACED" -> jdbc.sql("""insert into kitchen_ticket (order_id, store_id, status, items) values (?,?,'AWAITING_PAYMENT',?)
                on conflict (order_id) do update set items = excluded.items, updated_at = now() where kitchen_ticket.items = '[]'""")
                .params(orderId, storeId, mapper.writeValueAsString(d["lines"])).update()
            // 승인 이벤트가 주문 접수 이벤트보다 먼저 와도(재처리 순서) 상태가 뒤로 가지 않는다
            "PAYMENT_APPROVED" -> jdbc.sql("""insert into kitchen_ticket (order_id, store_id, status, items) values (?,?,'NEW','[]')
                on conflict (order_id) do update set status = case when kitchen_ticket.status = 'AWAITING_PAYMENT' then 'NEW' else kitchen_ticket.status end, updated_at = now()""")
                .params(orderId, storeId).update()
            "PAYMENT_DECLINED" -> void(orderId, storeId)
            "PAYMENT_CANCELLED" -> if (d["fullyCancelled"].asBoolean()) void(orderId, storeId)
        }
    }

    private fun void(orderId: Long, storeId: Long) {
        jdbc.sql("""insert into kitchen_ticket (order_id, store_id, status, items) values (?,?,'VOID','[]')
            on conflict (order_id) do update set status='VOID', updated_at = now()""").params(orderId, storeId).update()
    }

    private fun notification(eventId: UUID, type: String, d: JsonNode) {
        val orderId = d["orderId"].asLong()
        val msg = when (type) {
            "PAYMENT_APPROVED" -> "주문 #$orderId 결제가 완료되었습니다"
            "PAYMENT_DECLINED" -> "주문 #$orderId 결제가 거절되었습니다"
            "PAYMENT_CANCELLED" -> "주문 #$orderId ${d["amountMinor"].asLong()}원이 취소되었습니다"
            else -> return
        }
        jdbc.sql("insert into notification (event_id, order_id, message) values (?,?,?) on conflict do nothing").params(eventId, orderId, msg).update()
    }

    private fun ledger(eventId: UUID, type: String, d: JsonNode) {
        val (kind, ref, at) = when (type) {
            "PAYMENT_APPROVED" -> Triple("SALE", d["paymentRef"].asText(), d["approvedAt"].asText())
            "PAYMENT_CANCELLED" -> Triple("CANCEL", d["cancelKey"].asText(), d["cancelledAt"].asText())
            else -> return
        }
        val instant = Instant.parse(at)
        jdbc.sql("insert into ledger_entry (event_id, store_id, kind, ref, amount_minor, occurred_at, business_date) values (?,?,?,?,?,?,?) on conflict do nothing")
            .params(eventId, d["storeId"].asLong(), kind, ref, d["amountMinor"].asLong(), Timestamp.from(instant),
                java.sql.Date.valueOf(instant.atZone(BUSINESS_ZONE).toLocalDate())).update()
    }
}

/** 실패하면 정책대로 재시도하고, 끝내 안 되면 dead-letter 에 보관한 뒤 다음 이벤트로 넘어간다. */
@Component
class ConsumerRunner(
    private val processor: EventProcessor,
    private val jdbc: JdbcClient,
    private val mapper: ObjectMapper,
    private val props: OrderlaneProperties,
    private val meters: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val policy get() = RetryPolicy(props.consumer.maxAttempts, props.consumer.baseDelayMs)

    fun run(consumer: String, rec: ConsumerRecord<String, String>) {
        val env = mapper.readTree(rec.value())
        env["traceId"]?.takeIf { !it.isNull }?.let { MDC.put("traceId", it.asText()) }
        try {
            var attempt = 1
            while (true) {
                try {
                    processor.process(consumer, env)
                    return
                } catch (e: Exception) {
                    log.warn("이벤트 처리 실패 consumer={} eventId={} attempt={} error={}", consumer, env["eventId"].asText(), attempt, e.message)
                    meters.counter("orderlane.events.consumed", "consumer", consumer, "result", "failed").increment()
                    when (val d = Consumption.onFailure(attempt, policy)) {
                        is ConsumeDecision.Retry -> { Thread.sleep(d.delayMs); attempt = d.nextAttempt }
                        else -> {
                            jdbc.sql("insert into dead_letter (consumer, event_id, event_type, payload, error, attempts) values (?,?,?,?,?,?)")
                                .params(consumer, UUID.fromString(env["eventId"].asText()), env["type"].asText(), rec.value(), e.message ?: e.javaClass.simpleName, attempt).update()
                            meters.counter("orderlane.events.dead_lettered", "consumer", consumer).increment()
                            log.error("dead-letter 보관 consumer={} eventId={} attempts={}", consumer, env["eventId"].asText(), attempt)
                            return
                        }
                    }
                }
            }
        } finally { MDC.remove("traceId") }
    }
}

@Component
class EventListeners(private val runner: ConsumerRunner) {
    @KafkaListener(topics = ["\${orderlane.outbox.topic}"], groupId = "orderlane-kitchen")
    fun kitchen(rec: ConsumerRecord<String, String>) = runner.run("kitchen", rec)

    @KafkaListener(topics = ["\${orderlane.outbox.topic}"], groupId = "orderlane-notification")
    fun notification(rec: ConsumerRecord<String, String>) = runner.run("notification", rec)

    @KafkaListener(topics = ["\${orderlane.outbox.topic}"], groupId = "orderlane-ledger")
    fun ledger(rec: ConsumerRecord<String, String>) = runner.run("ledger", rec)
}

/** dead-letter 조회와 재처리. 재처리는 원본 이벤트를 같은 ID 로 다시 발행한다 — 이미 처리한 소비자는 중복으로 건너뛴다. */
@Component
class DeadLetterService(
    private val jdbc: JdbcClient,
    private val relay: OutboxRelay,
    private val mapper: ObjectMapper,
    meters: MeterRegistry,
) {
    init {
        Gauge.builder("orderlane.dead_letters.open") { open().toDouble() }.description("재처리하지 않은 dead-letter").register(meters)
    }

    fun open(): Long = jdbc.sql("select count(*) from dead_letter where replayed_at is null").query(Long::class.java).single()

    fun list(): List<Map<String, Any?>> = jdbc.sql("select id, consumer, event_id, event_type, error, attempts, replayed_at from dead_letter order by id")
        .query { rs, _ -> mapOf("id" to rs.getLong(1), "consumer" to rs.getString(2), "eventId" to rs.getString(3), "type" to rs.getString(4),
            "error" to rs.getString(5), "attempts" to rs.getInt(6), "replayed" to (rs.getTimestamp(7) != null)) }.list()

    fun replayAll(): Int {
        val rows = jdbc.sql("select id, event_id, event_type, payload from dead_letter where replayed_at is null order by id")
            .query { rs, _ -> listOf(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)) }.list()
        // 같은 이벤트가 여러 소비자에서 실패했어도 발행은 한 번만 한다
        val published = HashSet<String>()
        for (r in rows) {
            val eventId = r[1] as String
            if (published.add(eventId)) {
                val payload = r[3] as String
                relay.republish(eventId, mapper.readTree(payload)["aggregateId"].asText(), r[2] as String, payload)
            }
            jdbc.sql("update dead_letter set replayed_at = now() where id=?").params(r[0]).update()
        }
        return rows.size
    }
}
