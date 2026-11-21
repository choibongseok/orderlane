package orderlane.e2e

import orderlane.e2e.Stack.await
import orderlane.e2e.Stack.awaitQuiet
import orderlane.e2e.Stack.call
import orderlane.e2e.Stack.count
import orderlane.e2e.Stack.note
import orderlane.e2e.Stack.order
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StackE2E {
    @BeforeEach fun reset() = Stack.reset()
    @AfterAll fun record() = Stack.writeNotes()

    private fun orderState(id: Long) = Stack.scalar("select status from orders where id=$id")
    private fun paymentState(id: Long) = Stack.scalar("select state from payment where order_id=$id")

    @Test fun orderFlowsThroughEveryContainerToKitchenNotificationAndLedger() {
        val r = order(lines = arrayOf(Triple(1L, 2, 101L)))
        assertThat(r.status).isEqualTo(201)
        val id = r["orderId"].asLong()
        await(what = "세 소비자가 처리") { count("kitchen_ticket", "order_id=$id and status='NEW'") == 1L && count("notification", "order_id=$id") == 1L && count("ledger_entry", "kind='SALE'") == 1L }
        assertThat(call("GET", "/stores/1/kitchen", auth = false).json.map { it["orderId"].asLong() }).contains(id)
        note("정상 주문 흐름", "POST /orders → 201 PAID, 합계 ${r["totalMinor"]}원(서버 계산)")
        note("정상 주문 흐름", "아웃박스 → 브로커 → 주방 표시·알림·원장 소비자 3개가 각각 1건 처리")
    }

    @Test fun duplicateRequestsAreSafeAcrossTheWire() {
        val key = UUID.randomUUID().toString()
        val a = order(key = key)
        val b = order(key = key)
        assertThat(b.body).isEqualTo(a.body)
        assertThat(order(key = key, lines = arrayOf(Triple(1L, 3, null))).status).isEqualTo(422)
        val slow = UUID.randomUUID().toString()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit(Callable { order(token = "tok_slow_700", key = slow) })
            Thread.sleep(250)
            val during = order(token = "tok_slow_700", key = slow)
            assertThat(during.status).isEqualTo(409)
            assertThat(first.get().status).isEqualTo(201)
            assertThat(order(token = "tok_slow_700", key = slow).body).isEqualTo(first.get().body)
            note("중복 요청", "같은 키+같은 본문 → 같은 응답(주문 1건), 다른 본문 → 422, 처리 중 재요청 → 409, 완료 후 재요청 → 같은 응답")
        } finally { pool.shutdownNow() }
        assertThat(count("orders")).isEqualTo(2)
    }

    @Test fun lastUnitIsSoldOnceUnderConcurrencyWithBothLockingStrategies() {
        for (strategy in listOf("optimistic", "pessimistic")) {
            call("POST", "/admin/inventory-strategy", mapOf("name" to strategy))
            repeat(8) { round ->
                Stack.exec("update stock set available=1, version=0 where menu_item_id=5")
                val pool = Executors.newFixedThreadPool(20)
                val gate = CountDownLatch(1)
                try {
                    val results = (1..20).map { i -> pool.submit(Callable { gate.await(); order(store = 2, key = "e2e-$strategy-$round-$i", lines = arrayOf(Triple(5L, 1, null))).status }) }
                    gate.countDown()
                    val statuses = results.map { it.get() }
                    assertThat(statuses.count { it == 201 }).describedAs("$strategy 라운드 $round").isEqualTo(1)
                    assertThat(statuses.count { it == 409 }).isEqualTo(19)
                } finally { pool.shutdownNow() }
                assertThat(Stack.scalar("select available from stock where menu_item_id=5")).isEqualTo("0")
            }
            note("재고 동시성", "$strategy: 마지막 1개에 20명 동시 주문 × 8회 — 매번 판매 1건, 품절 19건, 재고 0")
            awaitQuiet()
        }
    }

    @Test fun cancellationsAreIdempotentAndPartialCancelKeepsTheOrderPaid() {
        val id = order(lines = arrayOf(Triple(2L, 1, null)))["orderId"].asLong()
        val c1 = call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 1500), mapOf("Idempotency-Key" to "e2e-c1"))
        val c1again = call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 1500), mapOf("Idempotency-Key" to "e2e-c1"))
        assertThat(c1again.body).isEqualTo(c1.body)
        assertThat(c1["remainingMinor"].asLong()).isEqualTo(3500)
        assertThat(count("payment_cancel")).isEqualTo(1)
        assertThat(call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 9999), mapOf("Idempotency-Key" to "e2e-c2")).status).isEqualTo(422)
        val full = call("POST", "/orders/$id/cancellations", null, mapOf("Idempotency-Key" to "e2e-c3"))
        assertThat(full["paymentStatus"].asText()).isEqualTo("CANCELLED")
        note("취소", "부분 취소 1500원 → 같은 키 재요청은 같은 응답·취소 1건, 초과 취소 422, 나머지 전체 취소 → CANCELLED")
    }

    @Test fun paymentTimeoutIsResolvedByInquiry() {
        val approved = order(token = "tok_timeout_approved")
        assertThat(approved.status).isEqualTo(202)
        val lost = order(token = "tok_timeout_lost", lines = arrayOf(Triple(3L, 1, null)))
        assertThat(lost.status).isEqualTo(202)
        await(what = "조회로 확정") { orderState(approved["orderId"].asLong()) == "PAID" && orderState(lost["orderId"].asLong()) == "PAYMENT_FAILED" }
        assertThat(Stack.scalar("select available from stock where menu_item_id=3")).isEqualTo("1")
        note("결제 시간 초과", "승인 응답이 늦은 건 → 202 대기 후 재조회로 승인 확정(PAID), 요청이 사라진 건 → 재조회 NOT_FOUND 로 거절 확정·재고 반환")
    }

    @Test fun heldPaymentIsSweptToApproved() {
        val r = order(token = "tok_hold")
        val id = r["orderId"].asLong()
        await(what = "보류") { paymentState(id) == "HELD" }
        await(what = "보류 건 확정") { paymentState(id) == "APPROVED" && orderState(id) == "PAID" }
        note("결제 보류", "처리 중 상태가 이어져 HELD 로 전환 → 재조회 작업이 승인 확정")
    }

    @Test fun duplicateAndReorderedCallbacksFromTheGatewayApplyOnce() {
        val id = order(token = "tok_timeout_callbacks")["orderId"].asLong()
        await(what = "콜백으로 승인") { orderState(id) == "PAID" }
        Thread.sleep(800)
        awaitQuiet()
        assertThat(count("outbox", "event_type='PAYMENT_APPROVED'")).isEqualTo(1)
        assertThat(count("ledger_entry", "kind='SALE'")).isEqualTo(1)
        note("중복·뒤바뀐 콜백", "게이트웨이가 승인#2, 승인#2(중복), 처리중#1(뒤늦게)을 보냄 → 승인 이벤트 1건, 원장 1건")
    }

    @Test fun consumerFailureGoesToDeadLetterAndReplayCommandRecovers() {
        call("POST", "/admin/faults", mapOf("consumer" to "notification", "times" to 1000))
        val id = order()["orderId"].asLong()
        await(what = "dead-letter") { count("dead_letter") >= 1L }
        await(what = "다른 소비자 처리") { count("ledger_entry", "kind='SALE'") == 1L }
        assertThat(count("notification", "order_id=$id")).isEqualTo(0)
        call("DELETE", "/admin/faults")
        assertThat(call("POST", "/admin/dead-letters/replay")["replayed"].asInt()).isGreaterThanOrEqualTo(1)
        await(what = "재처리로 알림") { count("notification", "order_id=$id") == 1L }
        Thread.sleep(500)
        assertThat(count("ledger_entry", "kind='SALE'")).isEqualTo(1)
        note("소비자 실패", "알림 소비자 실패 → 재시도 후 dead-letter 보관, 다른 소비자는 정상 → 재처리 명령으로 알림 1건, 원장은 중복 없음")
    }

    @Test fun settlementAndReconciliationAgainstTheGatewayFile() {
        val ids = (1..3).map { order(lines = arrayOf(Triple(1L, 1, null)))["orderId"].asLong() }
        order(store = 2, lines = arrayOf(Triple(4L, 1, null)))
        awaitQuiet()
        val s = call("POST", "/admin/settlements?date=${Stack.today}")
        assertThat(s.json.first { it["storeId"].asLong() == 1L }["feeMinor"].asLong()).isEqualTo(405) // 13500*3%
        assertThat(call("POST", "/admin/settlements?date=${Stack.today}").body).isEqualTo(s.body)
        assertThat(call("POST", "/admin/reconciliations?date=${Stack.today}")["clean"].asBoolean()).isTrue()
        val refs = ids.map { Stack.scalar("select payment_ref from payment where order_id=$it") }
        call("POST", "/admin/anomalies", mapOf("missing" to listOf(refs[0]), "amountDiff" to mapOf(refs[1] to 1), "duplicate" to listOf(refs[2])), base = Stack.GATEWAY, auth = false)
        val dirty = call("POST", "/admin/reconciliations?date=${Stack.today}")
        assertThat(dirty["mismatches"].map { it["type"].asText() }).containsExactlyInAnyOrder("MISSING_AT_PROVIDER", "AMOUNT_DIFFERENCE", "DUPLICATE_AT_PROVIDER")
        note("정산·대사", "매장별 일 정산(수수료 3%) 재실행 시 같은 결과, 깨끗한 파일은 대사 일치, 누락·금액 차이·중복 3종 주입 시 모두 보고")
    }

    @Test fun inFlightRequestFinishesWhenTheServerIsTerminated() {
        val pool = Executors.newFixedThreadPool(1)
        try {
            val inflight = pool.submit(Callable { order(token = "tok_slow_800", lines = arrayOf(Triple(1L, 1, null))) })
            Thread.sleep(300)
            val started = System.currentTimeMillis()
            Stack.docker("compose", "-p", "orderlane", "stop", "app")
            val stopMs = System.currentTimeMillis() - started
            val res = inflight.get()
            assertThat(res.status).describedAs("종료 신호 중이던 요청").isEqualTo(201)
            // JVM 은 SIGTERM 으로 끝나면 143(128+15)을 돌려준다. SIGKILL(137)이면 유예 시간을 넘겨 강제 종료된 것이다.
            val exit = Stack.docker("inspect", "--format", "{{.State.ExitCode}}", "orderlane-app-1").trim()
            assertThat(exit).describedAs("종료 코드(143=SIGTERM 로 정상 종료)").isEqualTo("143")
            val logs = Stack.docker("logs", "orderlane-app-1")
            assertThat(logs).contains("Commencing graceful shutdown").contains("Graceful shutdown complete")
            Stack.ensureAppUp()
            val id = res["orderId"].asLong()
            assertThat(orderState(id)).isEqualTo("PAID")
            await(what = "재기동 후 이벤트가 소비자까지 도착") { count("kitchen_ticket", "order_id=$id and status='NEW'") == 1L }
            note("우아한 종료", "처리 중(0.8초짜리) 주문 요청이 SIGTERM 중에도 201 로 끝남(종료 ${stopMs}ms, 강제 종료 아님), 재기동 뒤 아웃박스 이벤트가 주방 표시까지 도착")
        } finally { pool.shutdownNow(); Stack.ensureAppUp() }
    }

    @Test fun healthMetricsAndRequestIdAreExposed() {
        val rid = "e2e-" + UUID.randomUUID()
        val r = call("POST", "/orders", mapOf("storeId" to 1, "lines" to listOf(mapOf("menuItemId" to 1, "quantity" to 1))), mapOf("Idempotency-Key" to UUID.randomUUID().toString(), "X-Request-Id" to rid))
        assertThat(r.headers["x-request-id"]).containsExactly(rid)
        assertThat(call("GET", "/actuator/health", auth = false).json["status"].asText()).isEqualTo("UP")
        val m = call("GET", "/actuator/prometheus", auth = false).body
        assertThat(m).contains("orderlane_orders_total").contains("orderlane_outbox_pending").contains("orderlane_payments_total")
        assertThat(Stack.docker("logs", "--tail", "400", "orderlane-app-1")).contains("\"traceId\":\"$rid\"")
        note("운영성", "헬스 UP, 지표(주문·결제·아웃박스 적체) 노출, 요청 추적 ID 가 응답 헤더와 구조화 로그에 같이 찍힘")
    }
}
