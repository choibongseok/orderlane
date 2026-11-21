package orderlane.app.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import orderlane.app.OrderlaneProperties
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit

/** 이벤트를 현재 트랜잭션 안에서 아웃박스 테이블에 적는다. 상태 변경과 같이 커밋되거나 같이 사라진다. */
@Component
class OutboxWriter(private val jdbc: JdbcClient, private val mapper: ObjectMapper, private val clock: Clock) {
    fun add(type: String, aggregateId: Any, data: Map<String, Any?>): UUID {
        val id = UUID.randomUUID()
        val envelope = mapOf("eventId" to id.toString(), "type" to type, "aggregateId" to aggregateId.toString(),
            "occurredAt" to clock.instant().toString(), "traceId" to MDC.get("traceId"), "data" to data)
        jdbc.sql("insert into outbox (event_id, aggregate_id, event_type, payload) values (?,?,?,?)")
            .params(id, aggregateId.toString(), type, mapper.writeValueAsString(envelope)).update()
        return id
    }
}

/** 아웃박스 릴레이: 아직 발행하지 않은 행을 순서대로 브로커에 보내고 발행 시각을 적는다. 보낸 뒤 기록 전에 죽으면 다시 보내므로 적어도 한 번이다. */
@Component
class OutboxRelay(
    private val jdbc: JdbcClient,
    private val tx: TransactionTemplate,
    private val kafka: KafkaTemplate<String, String>,
    private val props: OrderlaneProperties,
    meters: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        Gauge.builder("orderlane.outbox.pending") { pending().toDouble() }.description("아직 발행하지 않은 아웃박스 행").register(meters)
    }

    fun pending(): Long = jdbc.sql("select count(*) from outbox where published_at is null").query(Long::class.java).single()

    @Scheduled(fixedDelayString = "\${orderlane.outbox.poll-ms}")
    fun relay() {
        try { while (relayBatch() > 0) { /* 남았으면 계속 */ } } catch (e: Exception) { log.warn("아웃박스 발행 실패, 다음 주기에 다시 시도: {}", e.message) }
    }

    fun relayBatch(): Int = tx.execute {
        val rows = jdbc.sql("select id, event_id, aggregate_id, event_type, payload from outbox where published_at is null order by id limit ? for update skip locked")
            .params(props.outbox.batchSize)
            .query { rs, _ -> listOf(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)) }.list()
        for (r in rows) {
            val rec = ProducerRecord(props.outbox.topic, r[2] as String, r[4] as String)
            rec.headers().add("eventId", (r[1] as String).toByteArray()).add("eventType", (r[3] as String).toByteArray())
            kafka.send(rec).get(10, TimeUnit.SECONDS)
            jdbc.sql("update outbox set published_at = now() where id=?").params(r[0]).update()
        }
        if (rows.isNotEmpty()) log.info("아웃박스 {}건 발행", rows.size)
        rows.size
    } ?: 0

    /** dead-letter 재처리용: 저장해 둔 원본을 같은 이벤트 ID 로 다시 발행한다. */
    fun republish(eventId: String, aggregateKey: String, type: String, payload: String) {
        val rec = ProducerRecord(props.outbox.topic, aggregateKey, payload)
        rec.headers().add("eventId", eventId.toByteArray()).add("eventType", type.toByteArray())
        kafka.send(rec).get(10, TimeUnit.SECONDS)
    }
}
