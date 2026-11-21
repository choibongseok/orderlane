package orderlane.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class EventFlowIT : IntegrationBase() {
    @Test fun outboxEventsReachKitchenNotificationAndLedger() {
        val id = order(lines = arrayOf(Triple(1L, 2, null)))["orderId"].asLong()
        await(what = "세 소비자 처리") { count("kitchen_ticket", "order_id=$id and status='NEW'") == 1L && count("notification", "order_id=$id") == 1L && count("ledger_entry", "kind='SALE'") == 1L }
        assertThat(count("outbox", "published_at is null")).isEqualTo(0)
        val kitchen = call("GET", "/stores/1/kitchen", auth = false)
        assertThat(kitchen.json.map { it["orderId"].asLong() }).contains(id)
        assertThat(kitchen.json[0]["items"][0]["name"].asText()).isEqualTo("Americano")
        assertThat(jdbc.sql("select amount_minor from ledger_entry").query(Long::class.java).single()).isEqualTo(9000)
    }

    @Test fun cancelledOrderLeavesKitchenAndLedgerRecordsCancel() {
        val id = order()["orderId"].asLong()
        await(what = "주방 표시") { count("kitchen_ticket", "order_id=$id and status='NEW'") == 1L }
        call("POST", "/orders/$id/cancellations", null, mapOf("Idempotency-Key" to "k-cancel"))
        await(what = "주방에서 제외") { count("kitchen_ticket", "order_id=$id and status='VOID'") == 1L && count("ledger_entry", "kind='CANCEL'") == 1L }
        assertThat(call("GET", "/stores/1/kitchen", auth = false).json.map { it["orderId"].asLong() }).doesNotContain(id)
    }

    @Test fun redeliveredEventIsProcessedOnceByEveryConsumer() {
        val id = order()["orderId"].asLong()
        awaitQuiet()
        val row = jdbc.sql("select event_id, aggregate_id, event_type, payload from outbox where event_type='PAYMENT_APPROVED'").query { rs, _ -> listOf(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)) }.single()
        repeat(3) { redeliver(row[0], row[1], row[2], row[3]) }
        Thread.sleep(1500)
        assertThat(count("ledger_entry", "kind='SALE'")).isEqualTo(1)
        assertThat(count("notification", "order_id=$id")).isEqualTo(1)
        assertThat(count("processed_event", "event_id='${row[0]}'")).isEqualTo(3)
    }

    @org.springframework.beans.factory.annotation.Autowired lateinit var relayBean: orderlane.app.outbox.OutboxRelay
    private fun redeliver(eventId: String, key: String, type: String, payload: String) = relayBean.republish(eventId, key, type, payload)

    @Test fun transientConsumerFailureIsRetriedAndSucceeds() {
        assertThat(call("POST", "/admin/faults", mapOf("consumer" to "notification", "times" to 2)).status).isEqualTo(200)
        val id = order()["orderId"].asLong()
        await(what = "재시도 끝에 알림 처리") { count("notification", "order_id=$id") == 1L }
        assertThat(count("dead_letter")).isEqualTo(0)
    }

    @Test fun persistentConsumerFailureGoesToDeadLetterThenReplayDeliversWithoutDuplicates() {
        call("POST", "/admin/faults", mapOf("consumer" to "notification", "times" to 1000))
        val id = order()["orderId"].asLong()
        await(what = "dead-letter 보관") { count("dead_letter", "consumer='notification'") >= 1L }
        await(what = "다른 소비자는 영향 없이 처리") { count("ledger_entry", "kind='SALE'") == 1L && count("kitchen_ticket", "order_id=$id and status='NEW'") == 1L }
        assertThat(count("notification", "order_id=$id")).isEqualTo(0)
        assertThat(call("GET", "/actuator/prometheus", auth = false).body).contains("orderlane_dead_letters_open")

        call("DELETE", "/admin/faults")
        val replay = call("POST", "/admin/dead-letters/replay")
        assertThat(replay["replayed"].asInt()).isGreaterThanOrEqualTo(1)
        await(what = "재처리로 알림 도착") { count("notification", "order_id=$id") == 1L }
        Thread.sleep(500)
        assertThat(count("ledger_entry", "kind='SALE'")).isEqualTo(1) // 이미 처리한 소비자는 중복으로 건너뛴다
        assertThat(count("notification", "order_id=$id")).isEqualTo(1)
        assertThat(call("GET", "/admin/dead-letters").json.all { it["replayed"].asBoolean() }).isTrue()
    }

    @Test fun requestIdIsEchoedAndCarriedIntoTheEvent() {
        val rid = "trace-" + UUID.randomUUID()
        val r = call("POST", "/orders", mapOf("storeId" to 1, "lines" to listOf(mapOf("menuItemId" to 1, "quantity" to 1))),
            mapOf("Idempotency-Key" to UUID.randomUUID().toString(), "X-Request-Id" to rid))
        assertThat(r.headers["x-request-id"]).containsExactly(rid)
        assertThat(jdbc.sql("select payload from outbox where event_type='ORDER_PLACED'").query(String::class.java).single()).contains("\"traceId\":\"$rid\"")
        assertThat(call("GET", "/orders/1").headers["x-request-id"]!![0]).isNotBlank()
    }
}
