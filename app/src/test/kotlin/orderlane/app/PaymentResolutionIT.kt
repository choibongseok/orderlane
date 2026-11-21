package orderlane.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class PaymentResolutionIT : IntegrationBase() {
    private fun orderState(id: Long) = jdbc.sql("select status from orders where id=?").params(id).query(String::class.java).single()
    private fun paymentState(id: Long) = jdbc.sql("select state from payment where order_id=?").params(id).query(String::class.java).single()

    @Test fun timedOutPaymentIsPendingThenConfirmedApprovedByInquiry() {
        val r = order(token = "tok_timeout_approved", lines = arrayOf(Triple(3L, 1, null)))
        assertThat(r.status).isEqualTo(202)
        assertThat(r["status"].asText()).isEqualTo("PAYMENT_PENDING")
        assertThat(stock(3)).isEqualTo(0) // 결과가 나기 전까지 재고는 잡아 둔다
        val id = r["orderId"].asLong()
        await(what = "조회로 승인 확정") { orderState(id) == "PAID" }
        assertThat(paymentState(id)).isEqualTo("APPROVED")
        assertThat(count("outbox", "event_type='PAYMENT_APPROVED'")).isEqualTo(1)
    }

    @Test fun requestLostAtGatewayIsConfirmedDeclinedAndStockReturns() {
        val r = order(token = "tok_timeout_lost", lines = arrayOf(Triple(3L, 1, null)))
        assertThat(r.status).isEqualTo(202)
        val id = r["orderId"].asLong()
        await(what = "조회로 거절 확정") { orderState(id) == "PAYMENT_FAILED" }
        assertThat(stock(3)).isEqualTo(1)
        assertThat(count("outbox", "event_type='PAYMENT_APPROVED'")).isEqualTo(0)
    }

    @Test fun unresolvedPaymentIsHeldThenSweptWhenGatewayConfirms() {
        val r = order(token = "tok_hold")
        assertThat(r.status).isEqualTo(202)
        val id = r["orderId"].asLong()
        await(what = "보류로 전환") { paymentState(id) == "HELD" }
        assertThat(orderState(id)).isEqualTo("PAYMENT_PENDING")
        await(what = "보류 건이 승인으로 확정") { paymentState(id) == "APPROVED" }
        assertThat(orderState(id)).isEqualTo("PAID")
    }

    @Test fun duplicateAndReorderedCallbacksApplyExactlyOnce() {
        val r = order(token = "tok_timeout_callbacks")
        val id = r["orderId"].asLong()
        await(what = "콜백으로 승인") { orderState(id) == "PAID" }
        Thread.sleep(500) // 중복·뒤바뀐 콜백이 모두 도착할 시간
        assertThat(count("outbox", "event_type='PAYMENT_APPROVED'")).isEqualTo(1)
        assertThat(paymentState(id)).isEqualTo("APPROVED")
        awaitQuiet()
        assertThat(count("ledger_entry", "kind='SALE'")).isEqualTo(1)
    }

    @Test fun conflictingCallbackIsRejectedAndRecordedWithoutChangingState() {
        val r = order()
        val id = r["orderId"].asLong()
        val ref = jdbc.sql("select payment_ref from payment where order_id=?").params(id).query(String::class.java).single()
        val cb = call("POST", "/callbacks/payments", mapOf("paymentRef" to ref, "seq" to 9, "type" to "DECLINED"), mapOf("X-Callback-Secret" to "demo-callback-secret"), auth = false)
        assertThat(cb.status).isEqualTo(200)
        assertThat(cb["result"].asText()).isEqualTo("rejected")
        assertThat(paymentState(id)).isEqualTo("APPROVED")
        assertThat(count("payment_conflict", "payment_ref='$ref'")).isEqualTo(1)
        assertThat(call("POST", "/callbacks/payments", mapOf("paymentRef" to ref, "seq" to 9, "type" to "APPROVED"), auth = false).status).isEqualTo(401)
    }

    @Test fun partialThenRemainingCancellationEndsCancelledAndReturnsStock() {
        val r = order(lines = arrayOf(Triple(3L, 1, null))) // 5500
        val id = r["orderId"].asLong()
        val c1 = call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 2000), mapOf("Idempotency-Key" to "c-1"))
        assertThat(c1.status).isEqualTo(200)
        assertThat(c1["paymentStatus"].asText()).isEqualTo("PARTIALLY_CANCELLED")
        assertThat(c1["remainingMinor"].asLong()).isEqualTo(3500)
        assertThat(stock(3)).isEqualTo(0) // 부분 취소는 재고를 돌려주지 않는다
        val c2 = call("POST", "/orders/$id/cancellations", null, mapOf("Idempotency-Key" to "c-2"))
        assertThat(c2["paymentStatus"].asText()).isEqualTo("CANCELLED")
        assertThat(c2["appliedMinor"].asLong()).isEqualTo(3500)
        assertThat(orderState(id)).isEqualTo("CANCELLED")
        assertThat(stock(3)).isEqualTo(1)
    }

    @Test fun sameCancelKeyIsAppliedOnlyOnce() {
        val id = order()["orderId"].asLong()
        val a = call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 1000), mapOf("Idempotency-Key" to "c-same"))
        val b = call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 1000), mapOf("Idempotency-Key" to "c-same"))
        assertThat(b.body).isEqualTo(a.body)
        assertThat(count("payment_cancel")).isEqualTo(1)
        assertThat(jdbc.sql("select cancelled_minor from payment where order_id=?").params(id).query(Long::class.java).single()).isEqualTo(1000)
        assertThat(call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 2000), mapOf("Idempotency-Key" to "c-same")).status).isEqualTo(422)
    }

    @Test fun cancellationAboveRemainingOrOnUnpaidOrderIsRejected() {
        val id = order()["orderId"].asLong()
        call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 4000), mapOf("Idempotency-Key" to "c-a"))
        val over = call("POST", "/orders/$id/cancellations", mapOf("amountMinor" to 600), mapOf("Idempotency-Key" to "c-b"))
        assertThat(over.status).isEqualTo(422)
        assertThat(over["code"].asText()).isEqualTo("EXCEEDS_REMAINING")
        val declined = order(token = "tok_decline")["orderId"].asLong()
        val bad = call("POST", "/orders/$declined/cancellations", null, mapOf("Idempotency-Key" to UUID.randomUUID().toString()))
        assertThat(bad.status).isEqualTo(409)
        assertThat(bad["code"].asText()).isEqualTo("NOT_CANCELLABLE")
        assertThat(call("POST", "/orders/9999/cancellations", null, mapOf("Idempotency-Key" to "x")).status).isEqualTo(404)
    }
}
