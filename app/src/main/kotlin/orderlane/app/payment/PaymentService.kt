package orderlane.app.payment

import io.micrometer.core.instrument.MeterRegistry
import orderlane.app.OrderlaneProperties
import orderlane.app.cache.MenuCache
import orderlane.app.inventory.InventoryStrategy
import orderlane.app.outbox.OutboxWriter
import orderlane.domain.Money
import orderlane.domain.Outcome
import orderlane.domain.PaymentMachine
import orderlane.domain.PaymentPolicy
import orderlane.domain.PaymentState
import orderlane.domain.Source
import orderlane.domain.Transition
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant

/** DB 컬럼 ↔ 도메인 결제 상태. state 컬럼에는 기본 이름만 두고, 부분 취소 여부는 cancelled_minor 로 안다. */
object PaymentCodec {
    fun encode(s: PaymentState): String = when (s) {
        is PaymentState.Requested -> "REQUESTED"
        is PaymentState.Unknown -> "UNKNOWN"
        is PaymentState.Held -> "HELD"
        is PaymentState.Declined -> "DECLINED"
        is PaymentState.Approved -> "APPROVED"
        is PaymentState.FullyCancelled -> "CANCELLED"
    }

    fun decode(state: String, amount: Long, cancelled: Long, inquiries: Int): PaymentState = when (state) {
        "REQUESTED" -> PaymentState.Requested(Money(amount))
        "UNKNOWN" -> PaymentState.Unknown(Money(amount), inquiries)
        "HELD" -> PaymentState.Held(Money(amount), inquiries)
        "DECLINED" -> PaymentState.Declined(Money(amount))
        "APPROVED" -> PaymentState.Approved(Money(amount), Money(cancelled))
        "CANCELLED" -> PaymentState.FullyCancelled(Money(amount))
        else -> throw IllegalStateException("알 수 없는 결제 상태: $state")
    }

    fun inquiries(s: PaymentState): Int = when (s) { is PaymentState.Unknown -> s.inquiries; is PaymentState.Held -> s.inquiries; else -> 0 }
    fun cancelled(s: PaymentState): Long = when (s) { is PaymentState.Approved -> s.cancelled.minor; is PaymentState.FullyCancelled -> s.amount.minor; else -> 0 }
}

/** 결제 결과(요청 응답·콜백·재조회)를 모두 이 한 곳에서 상태 기계에 먹여 반영한다. 행 잠금으로 직렬화한다. */
@Service
class PaymentService(
    private val jdbc: JdbcClient,
    private val tx: TransactionTemplate,
    private val props: OrderlaneProperties,
    private val outbox: OutboxWriter,
    private val inventory: InventoryStrategy,
    private val cache: MenuCache,
    private val meters: MeterRegistry,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun apply(ref: String, outcome: Outcome, source: Source, txId: String? = null, approvedAt: Instant? = null): Transition =
        tx.execute { applyLocked(ref, outcome, source, txId, approvedAt) }!!

    private fun applyLocked(ref: String, outcome: Outcome, source: Source, txId: String?, approvedAt: Instant?): Transition {
        val row = jdbc.sql("select p.id, p.order_id, p.state, p.amount_minor, p.cancelled_minor, p.inquiries, o.store_id from payment p join orders o on o.id = p.order_id where p.payment_ref=? for update of p")
            .params(ref).query { rs, _ ->
                listOf(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), rs.getLong(7))
            }.optional().orElseThrow { PaymentNotFoundException(ref) }
        val orderId = row[1] as Long
        val storeId = row[6] as Long
        val before = PaymentCodec.decode(row[2] as String, row[3] as Long, row[4] as Long, row[5] as Int)
        val t = PaymentMachine.on(before, outcome, source, PaymentPolicy(props.payment.maxInquiries))
        meters.counter("orderlane.payments", "outcome", outcome.name.lowercase(), "source", source.name.lowercase(),
            "result", t::class.simpleName!!.lowercase()).increment()
        when (t) {
            is Transition.Ignored -> log.info("결제 통지 무시 ref={} {}/{} reason={}", ref, source, outcome, t.reason)
            is Transition.Rejected -> {
                log.warn("허용되지 않는 결제 전이 ref={} {}/{} code={}", ref, source, outcome, t.code)
                jdbc.sql("insert into payment_conflict (payment_ref, code, detail) values (?,?,?)").params(ref, t.code, t.message).update()
            }
            is Transition.Applied -> {
                val after = t.state
                val now = clock.instant()
                val next = when (after) {
                    is PaymentState.Unknown -> Timestamp.from(now.plusMillis(props.payment.inquiryIntervalMs))
                    is PaymentState.Held -> Timestamp.from(now.plusMillis(props.payment.heldIntervalMs))
                    else -> null
                }
                jdbc.sql("update payment set state=?, cancelled_minor=?, inquiries=?, gateway_tx_id=coalesce(?, gateway_tx_id), approved_at=coalesce(?, approved_at), next_inquiry_at=?, updated_at=now() where id=?")
                    .params(PaymentCodec.encode(after), PaymentCodec.cancelled(after), PaymentCodec.inquiries(after), txId,
                        if (after is PaymentState.Approved) Timestamp.from(approvedAt ?: now) else null, next, row[0]).update()
                log.info("결제 상태 전이 ref={} {} -> {}", ref, before.name, after.name)
                if (after is PaymentState.Approved && before !is PaymentState.Approved) {
                    jdbc.sql("update orders set status='PAID', updated_at=now() where id=?").params(orderId).update()
                    outbox.add("PAYMENT_APPROVED", orderId, mapOf("orderId" to orderId, "storeId" to storeId, "paymentRef" to ref,
                        "amountMinor" to after.amount.minor, "approvedAt" to (approvedAt ?: now).toString()))
                }
                if (after is PaymentState.Declined) {
                    jdbc.sql("update orders set status='PAYMENT_FAILED', updated_at=now() where id=?").params(orderId).update()
                    releaseStock(orderId)
                    outbox.add("PAYMENT_DECLINED", orderId, mapOf("orderId" to orderId, "storeId" to storeId, "paymentRef" to ref))
                    cache.invalidateAfterCommit(storeId)
                }
            }
        }
        return t
    }

    fun releaseStock(orderId: Long) {
        val lines = jdbc.sql("select menu_item_id, sum(quantity) from order_line where order_id=? group by menu_item_id order by menu_item_id").params(orderId)
            .query { rs, _ -> rs.getLong(1) to rs.getInt(2) }.list()
        lines.forEach { (item, qty) -> inventory.current.release(item, qty) }
    }
}

class PaymentNotFoundException(ref: String) : RuntimeException("결제를 찾을 수 없음: $ref")
