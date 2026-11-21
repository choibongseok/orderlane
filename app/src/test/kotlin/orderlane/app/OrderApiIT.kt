package orderlane.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class OrderApiIT : IntegrationBase() {
    @org.springframework.beans.factory.annotation.Autowired lateinit var appMapper: com.fasterxml.jackson.databind.ObjectMapper

    @Test fun placesOrderAndChargesServerComputedTotal() {
        val r = order(lines = arrayOf(Triple(1L, 2, 101L))) // (4500+500)*2
        assertThat(r.status).isEqualTo(201)
        assertThat(r["status"].asText()).isEqualTo("PAID")
        assertThat(r["totalMinor"].asLong()).isEqualTo(10_000)
        assertThat(stock(1)).isEqualTo(198)
        assertThat(count("outbox", "event_type='ORDER_PLACED'")).isEqualTo(1)
        assertThat(count("outbox", "event_type='PAYMENT_APPROVED'")).isEqualTo(1)
    }

    @Test fun rejectsBadRequests() {
        assertThat(call("POST", "/orders", mapOf("storeId" to 1, "lines" to listOf(mapOf("menuItemId" to 1, "quantity" to 1))), auth = false).status).isEqualTo(401)
        assertThat(call("POST", "/orders", mapOf("storeId" to 1, "lines" to listOf(mapOf("menuItemId" to 1, "quantity" to 1)))).status).isEqualTo(400) // 키 없음
        assertThat(order(store = 99).status).isEqualTo(400)
        assertThat(order(lines = arrayOf(Triple(4L, 1, null))).status).isEqualTo(400) // 다른 매장 메뉴
        assertThat(order(lines = arrayOf(Triple(1L, 0, null))).status).isEqualTo(400)
        assertThat(order(lines = arrayOf(Triple(1L, 1, 104L))).status).isEqualTo(400) // 다른 메뉴 옵션
        assertThat(count("orders")).isEqualTo(0)
    }

    @Test fun sameKeyAndBodyReplaysTheFirstResponse() {
        val key = UUID.randomUUID().toString()
        val a = order(key = key)
        val b = order(key = key)
        assertThat(b.status).isEqualTo(a.status)
        assertThat(b.body).isEqualTo(a.body)
        assertThat(count("orders")).isEqualTo(1)
        assertThat(stock(1)).isEqualTo(199)
    }

    @Test fun sameKeyWithDifferentBodyIsRejected() {
        val key = UUID.randomUUID().toString()
        order(key = key, lines = arrayOf(Triple(1L, 1, null)))
        val b = order(key = key, lines = arrayOf(Triple(1L, 2, null)))
        assertThat(b.status).isEqualTo(422)
        assertThat(b["code"].asText()).isEqualTo("IDEMPOTENCY_KEY_REUSED")
        assertThat(count("orders")).isEqualTo(1)
    }

    @Test fun requestRetriedWhileFirstIsInFlightGetsConflictThenReplay() {
        val key = UUID.randomUUID().toString()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit(Callable { order(token = "tok_slow_400", key = key) })
            Thread.sleep(150)
            val second = order(token = "tok_slow_400", key = key)
            assertThat(second.status).isEqualTo(409)
            assertThat(second["code"].asText()).isEqualTo("IDEMPOTENCY_IN_PROGRESS")
            val firstRes = first.get()
            assertThat(firstRes.status).isEqualTo(201)
            val third = order(token = "tok_slow_400", key = key)
            assertThat(third.body).isEqualTo(firstRes.body)
            assertThat(count("orders")).isEqualTo(1)
        } finally { pool.shutdownNow() }
    }

    @Test fun abandonedInProgressKeyIsReclaimedWithoutCreatingASecondOrder() {
        val key = UUID.randomUUID().toString()
        val body = mapOf("storeId" to 1, "lines" to listOf(mapOf("menuItemId" to 1, "quantity" to 1, "optionId" to null)), "paymentToken" to "tok_approve")
        // 처리하던 서버가 죽은 상태를 만든다: 주문·결제 행은 있고(승인 요청 전), 멱등 키는 잠금이 지난 IN_PROGRESS
        val orderId = jdbc.sql("insert into orders (store_id, status, total_minor, idempotency_key) values (1,'PAYMENT_PENDING',4500,?) returning id").params(key).query(Long::class.java).single()
        jdbc.sql("insert into order_line (order_id, menu_item_id, quantity, unit_price, option_extra) values (?,1,1,4500,0)").params(orderId).update()
        jdbc.sql("insert into payment (order_id, payment_ref, state, amount_minor, card_token) values (?, 'pay-crashed', 'REQUESTED', 4500, 'tok_approve')").params(orderId).update()
        jdbc.sql("update stock set available = available - 1 where menu_item_id=1").update()
        val hash = orderlane.app.idem.sha256(appMapper.writeValueAsString(appMapper.readValue(mapper.writeValueAsString(body), orderlane.app.order.OrderRequest::class.java)))
        jdbc.sql("insert into idempotency_key (scope, key, request_hash, status, locked_until) values ('order', ?, ?, 'IN_PROGRESS', now() - interval '1 minute')").params(key, hash).update()

        val r = order(key = key)
        assertThat(r.status).isEqualTo(201)
        assertThat(r["orderId"].asLong()).isEqualTo(orderId)
        assertThat(count("orders")).isEqualTo(1)
        assertThat(stock(1)).isEqualTo(199) // 재고를 두 번 깎지 않았다
        assertThat(count("payment", "state='APPROVED'")).isEqualTo(1)
    }

    @Test fun lastUnitSoldOnceThenSoldOutIsReplayedConsistently() {
        val first = order(lines = arrayOf(Triple(3L, 1, null)))
        assertThat(first.status).isEqualTo(201)
        val key = UUID.randomUUID().toString()
        val second = order(key = key, lines = arrayOf(Triple(3L, 1, null)))
        assertThat(second.status).isEqualTo(409)
        assertThat(second["code"].asText()).isEqualTo("SOLD_OUT")
        assertThat(order(key = key, lines = arrayOf(Triple(3L, 1, null))).body).isEqualTo(second.body)
        assertThat(stock(3)).isEqualTo(0)
    }

    @Test fun soldOutOnSecondLineRollsBackTheFirstReservation() {
        order(lines = arrayOf(Triple(3L, 1, null))) // 한정 메뉴 소진
        val r = order(lines = arrayOf(Triple(1L, 2, null), Triple(3L, 1, null)))
        assertThat(r.status).isEqualTo(409)
        assertThat(stock(1)).isEqualTo(200) // 1번 메뉴 예약이 남지 않았다
        assertThat(count("orders")).isEqualTo(1)
    }

    @Test fun declinedPaymentReleasesStockAndMarksOrderFailed() {
        val r = order(token = "tok_decline", lines = arrayOf(Triple(3L, 1, null)))
        assertThat(r.status).isEqualTo(402)
        assertThat(r["status"].asText()).isEqualTo("PAYMENT_FAILED")
        assertThat(stock(3)).isEqualTo(1)
        assertThat(order(lines = arrayOf(Triple(3L, 1, null))).status).isEqualTo(201) // 반환된 재고를 다시 팔 수 있다
    }
}
