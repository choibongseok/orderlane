package orderlane.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperationsIT : IntegrationBase() {
    private fun payAndSettle(vararg tokens: Pair<Long, Long>) {
        tokens.forEach { (store, item) -> order(store = store, lines = arrayOf(Triple(item, 1, null))) }
    }

    @Test fun dailySettlementIsPerStoreAndRerunGivesTheSameResult() {
        order(lines = arrayOf(Triple(1L, 2, null))) // s1 9000
        val cancelled = order(lines = arrayOf(Triple(2L, 1, null)))["orderId"].asLong() // s1 5000
        order(store = 2, lines = arrayOf(Triple(4L, 1, null))) // s2 3800
        awaitQuiet()
        call("POST", "/orders/$cancelled/cancellations", mapOf("amountMinor" to 2000), mapOf("Idempotency-Key" to "set-c"))
        awaitQuiet()
        val first = call("POST", "/admin/settlements?date=$today")
        assertThat(first.status).isEqualTo(200)
        val s1 = first.json.first { it["storeId"].asLong() == 1L }
        assertThat(s1["salesMinor"].asLong()).isEqualTo(14_000)
        assertThat(s1["cancelsMinor"].asLong()).isEqualTo(2_000)
        assertThat(s1["feeMinor"].asLong()).isEqualTo(360) // (14000-2000)*3%
        assertThat(s1["netMinor"].asLong()).isEqualTo(11_640)
        assertThat(first.json.first { it["storeId"].asLong() == 2L }["feeMinor"].asLong()).isEqualTo(114)
        val second = call("POST", "/admin/settlements?date=$today")
        assertThat(second.body).isEqualTo(first.body)
        assertThat(count("settlement")).isEqualTo(2)
        assertThat(call("GET", "/admin/settlements?date=$today").json.size()).isEqualTo(2)
    }

    @Test fun reconciliationMatchesCleanFilesAndReportsInjectedMismatches() {
        val ids = (1..4).map { order(store = 1, lines = arrayOf(Triple(1L, 1, null)))["orderId"].asLong() }
        awaitQuiet()
        val clean = call("POST", "/admin/reconciliations?date=$today")
        assertThat(clean["clean"].asBoolean()).isTrue()
        assertThat(clean["matched"].asInt()).isEqualTo(4)

        val refs = ids.map { jdbc.sql("select payment_ref from payment where order_id=?").params(it).query(String::class.java).single() }
        gatewayPost("/admin/anomalies", mapper.writeValueAsString(mapOf("missing" to listOf(refs[0]), "amountDiff" to mapOf(refs[1] to 4400), "duplicate" to listOf(refs[2]))))
        val dirty = call("POST", "/admin/reconciliations?date=$today")
        assertThat(dirty["mismatchCount"].asInt()).isEqualTo(3)
        assertThat(dirty["matched"].asInt()).isEqualTo(1)
        val types = dirty["mismatches"].associate { it["ref"].asText() to it["type"].asText() }
        assertThat(types[refs[0]]).isEqualTo("MISSING_AT_PROVIDER")
        assertThat(types[refs[1]]).isEqualTo("AMOUNT_DIFFERENCE")
        assertThat(types[refs[2]]).isEqualTo("DUPLICATE_AT_PROVIDER")
        assertThat(call("POST", "/admin/reconciliations?date=$today").body).isEqualTo(dirty.body) // 다시 돌려도 같다
        assertThat(count("reconciliation_item")).isEqualTo(3)
        gatewayPost("/admin/reset")
    }

    @Test fun menuReadIsCachedAndInvalidatedWhenStockChanges() {
        fun available() = call("GET", "/stores/1/menu", auth = false).json["items"].first { it["id"].asLong() == 3L }["available"].asInt()
        val hitsBefore = counter("hit")
        assertThat(available()).isEqualTo(1)
        assertThat(available()).isEqualTo(1)
        assertThat(counter("hit")).isEqualTo(hitsBefore + 1)
        order(lines = arrayOf(Triple(3L, 1, null)))
        assertThat(available()).isEqualTo(0) // 커밋 뒤 키를 지워서 새 값을 읽는다
        val id = order(store = 1, lines = arrayOf(Triple(1L, 1, null)))["orderId"].asLong()
        assertThat(available()).isEqualTo(0)
        // 취소로 재고가 돌아오면 다시 지워진다
        val limited = jdbc.sql("select o.id from orders o join order_line l on l.order_id=o.id where l.menu_item_id=3").query(Long::class.java).single()
        call("POST", "/orders/$limited/cancellations", null, mapOf("Idempotency-Key" to "cache-c"))
        assertThat(available()).isEqualTo(1)
        assertThat(id).isPositive()
    }

    private fun counter(result: String): Double {
        val line = call("GET", "/actuator/prometheus", auth = false).body.lines().firstOrNull { it.startsWith("orderlane_cache_total") && it.contains("result=\"$result\"") } ?: return 0.0
        return line.substringAfterLast(' ').toDouble()
    }

    @Test fun healthAndMetricsAreExposed() {
        order()
        assertThat(call("GET", "/actuator/health", auth = false).json["status"].asText()).isEqualTo("UP")
        assertThat(call("GET", "/actuator/health/readiness", auth = false).status).isEqualTo(200)
        val m = call("GET", "/actuator/prometheus", auth = false).body
        assertThat(m).contains("orderlane_orders_total{application=\"orderlane\",result=\"paid\"}")
        assertThat(m).contains("orderlane_payments_total").contains("orderlane_outbox_pending").contains("orderlane_events_consumed_total")
    }
}
