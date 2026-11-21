package orderlane.app.order

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import orderlane.app.cache.MenuCache
import orderlane.app.idem.IdempotencyService
import orderlane.app.idem.Stored
import orderlane.app.idem.sha256
import orderlane.app.inventory.InventoryStrategy
import orderlane.app.inventory.StockContentionException
import orderlane.app.outbox.OutboxWriter
import orderlane.app.payment.CancelConfirmation
import orderlane.app.payment.GatewayClient
import orderlane.app.payment.GatewayRejectedException
import orderlane.app.payment.GatewayUnavailableException
import orderlane.app.payment.PaymentCodec
import orderlane.app.payment.PaymentService
import orderlane.app.web.ApiError
import orderlane.app.web.ApiException
import orderlane.domain.Money
import orderlane.domain.OrderLine
import orderlane.domain.PaymentMachine
import orderlane.domain.PaymentState
import orderlane.domain.Pricing
import orderlane.domain.Source
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp

data class LineRequest(val menuItemId: Long, val quantity: Int, val optionId: Long? = null)
data class OrderRequest(val storeId: Long, val lines: List<LineRequest>, val paymentToken: String = "tok_approve")
data class CancelRequest(val amountMinor: Long? = null)

data class OrderView(
    val orderId: Long, val storeId: Long, val status: String, val totalMinor: Long,
    val paymentStatus: String, val cancelledMinor: Long, val remainingMinor: Long,
)

class SoldOutException(val itemId: Long) : RuntimeException("재고 부족 item=$itemId")

@Service
class OrderService(
    private val jdbc: JdbcClient,
    private val tx: TransactionTemplate,
    private val mapper: ObjectMapper,
    private val idem: IdempotencyService,
    private val inventory: InventoryStrategy,
    private val gateway: GatewayClient,
    private val payments: PaymentService,
    private val outbox: OutboxWriter,
    private val cache: MenuCache,
    private val meters: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ---------- 주문 생성 ----------

    fun place(key: String, req: OrderRequest): Stored {
        val lines = validate(req)
        val hash = sha256(mapper.writeValueAsString(req))
        return idem.execute("order", key, hash) { reclaimed ->
            val existing = findOrderByKey(key)
            val orderId = existing ?: try {
                createOrder(key, req, lines)
            } catch (e: SoldOutException) {
                meters.counter("orderlane.orders", "result", "sold_out").increment()
                return@execute stored(409, ApiError("SOLD_OUT", "재고가 부족하다 (menuItemId=${e.itemId})"))
            } catch (e: StockContentionException) {
                throw ApiException(503, "STOCK_BUSY", "재고 경합이 길어져 처리하지 못했다. 같은 키로 다시 시도해도 된다")
            }
            if (existing != null) log.info("이어받기: 이미 만든 주문 {} 을 마무리한다 reclaimed={}", orderId, reclaimed)
            finishPayment(orderId)
            val v = view(orderId)
            meters.counter("orderlane.orders", "result", v.status.lowercase()).increment()
            storedView(v)
        }
    }

    private fun validate(req: OrderRequest): List<OrderLine> {
        require(req.lines.isNotEmpty() && req.lines.size <= 20) { "주문 줄은 1..20개" }
        val exists = jdbc.sql("select count(*) from store where id=?").params(req.storeId).query(Long::class.java).single()
        if (exists == 0L) throw ApiException(400, "UNKNOWN_STORE", "없는 매장이다")
        val items = jdbc.sql("select id, price from menu_item where store_id=?").params(req.storeId)
            .query { rs, _ -> rs.getLong(1) to rs.getLong(2) }.list().toMap()
        return req.lines.map { l ->
            val price = items[l.menuItemId] ?: throw ApiException(400, "UNKNOWN_ITEM", "이 매장의 메뉴가 아니다: ${l.menuItemId}")
            val extra = l.optionId?.let { opt ->
                jdbc.sql("select extra_price from menu_option where id=? and menu_item_id=?").params(opt, l.menuItemId)
                    .query(Long::class.java).optional().orElseThrow { ApiException(400, "UNKNOWN_OPTION", "이 메뉴의 옵션이 아니다: $opt") }
            } ?: 0L
            OrderLine(l.menuItemId, l.quantity, Money(price), Money(extra))
        }
    }

    private fun findOrderByKey(key: String): Long? =
        jdbc.sql("select id from orders where idempotency_key=?").params(key).query(Long::class.java).optional().orElse(null)

    /** 재고 예약·주문·결제 행·아웃박스를 한 트랜잭션으로 만든다. 하나라도 실패하면 예약도 함께 되돌아간다. */
    private fun createOrder(key: String, req: OrderRequest, lines: List<OrderLine>): Long = tx.execute {
        for ((item, qty) in Pricing.quantityByItem(lines)) {
            if (!inventory.current.reserve(item, qty)) throw SoldOutException(item)
        }
        val total = Pricing.total(lines)
        val orderId = jdbc.sql("insert into orders (store_id, status, total_minor, idempotency_key) values (?, 'PAYMENT_PENDING', ?, ?) returning id")
            .params(req.storeId, total.minor, key).query(Long::class.java).single()
        req.lines.zip(lines).forEach { (r, l) ->
            jdbc.sql("insert into order_line (order_id, menu_item_id, option_id, quantity, unit_price, option_extra) values (?,?,?,?,?,?)")
                .params(orderId, l.menuItemId, r.optionId, l.quantity, l.unitPrice.minor, l.optionExtra.minor).update()
        }
        val ref = "pay-" + java.util.UUID.randomUUID()
        jdbc.sql("insert into payment (order_id, payment_ref, state, amount_minor, card_token) values (?,?, 'REQUESTED', ?, ?)")
            .params(orderId, ref, total.minor, req.paymentToken).update()
        val names = jdbc.sql("select id, name from menu_item where store_id=?").params(req.storeId).query { rs, _ -> rs.getLong(1) to rs.getString(2) }.list().toMap()
        outbox.add("ORDER_PLACED", orderId, mapOf("orderId" to orderId, "storeId" to req.storeId, "totalMinor" to total.minor,
            "lines" to lines.map { mapOf("menuItemId" to it.menuItemId, "name" to names[it.menuItemId], "quantity" to it.quantity) }))
        cache.invalidateAfterCommit(req.storeId)
        orderId
    }!!

    /** 승인 요청은 트랜잭션 밖에서 보낸다(DB 연결·잠금을 쥐고 외부 호출을 기다리지 않는다). */
    private fun finishPayment(orderId: Long) {
        val p = jdbc.sql("select payment_ref, state, amount_minor, card_token from payment where order_id=?").params(orderId)
            .query { rs, _ -> listOf(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4)) }.single()
        if (p[1] == "REQUESTED") {
            val r = gateway.authorize(p[0] as String, p[2] as Long, p[3] as String)
            payments.apply(p[0] as String, r.outcome, Source.REQUEST, r.txId, r.approvedAt)
        }
    }

    // ---------- 조회 ----------

    fun view(orderId: Long): OrderView = jdbc.sql(
        "select o.id, o.store_id, o.status, o.total_minor, p.state, p.cancelled_minor, p.amount_minor from orders o join payment p on p.order_id = o.id where o.id=?")
        .params(orderId).query { rs, _ ->
            val state = PaymentCodec.decode(rs.getString(5), rs.getLong(7), rs.getLong(6), 0)
            OrderView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), state.name, rs.getLong(6),
                if (state is PaymentState.Approved) state.remaining.minor else 0)
        }.optional().orElseThrow { ApiException(404, "ORDER_NOT_FOUND", "없는 주문이다") }

    private fun storedView(v: OrderView) = stored(when (v.status) { "PAID" -> 201; "PAYMENT_PENDING" -> 202; "PAYMENT_FAILED" -> 402; else -> 200 }, v)

    private fun stored(status: Int, body: Any) = Stored(status, mapper.writeValueAsString(body))

    // ---------- 취소 ----------

    fun cancel(orderId: Long, key: String, req: CancelRequest): Stored {
        view(orderId)
        val hash = sha256("amount=" + (req.amountMinor ?: "full"))
        return idem.execute("cancel:$orderId", key, hash) { _ ->
            tx.execute { cancelLocked(orderId, key, req.amountMinor) }!!
        }
    }

    private fun cancelLocked(orderId: Long, key: String, amount: Long?): Stored {
        val row = jdbc.sql("select p.id, p.payment_ref, p.state, p.amount_minor, p.cancelled_minor, o.store_id from payment p join orders o on o.id=p.order_id where p.order_id=? for update of p")
            .params(orderId).query { rs, _ -> listOf(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5), rs.getLong(6)) }.single()
        val ref = row[1] as String
        val storeId = row[5] as Long
        val state = PaymentCodec.decode(row[2] as String, row[3] as Long, row[4] as Long, 0)
        val applied = jdbc.sql("select cancel_key, amount_minor from payment_cancel where payment_id=?").params(row[0])
            .query { rs, _ -> rs.getString(1) to Money(rs.getLong(2)) }.list().toMap()
        return when (val r = PaymentMachine.cancel(state, key, amount?.let(::Money), applied)) {
            is PaymentMachine.CancelResult.Rejected -> {
                meters.counter("orderlane.cancellations", "result", r.code).increment()
                stored(if (r.code == "not-cancellable") 409 else 422, ApiError(r.code.uppercase().replace('-', '_'), r.message))
            }
            is PaymentMachine.CancelResult.Replayed -> {
                meters.counter("orderlane.cancellations", "result", "replayed").increment()
                stored(200, cancelBody(view(orderId), r.amount.minor))
            }
            is PaymentMachine.CancelResult.Applied -> {
                val c: CancelConfirmation = try {
                    gateway.cancel(ref, key, r.amount.minor)
                } catch (e: GatewayUnavailableException) {
                    throw ApiException(502, "GATEWAY_UNAVAILABLE", "결제사 응답이 없어 취소하지 못했다. 같은 키로 다시 시도하면 된다")
                } catch (e: GatewayRejectedException) {
                    throw ApiException(409, "GATEWAY_REJECTED", e.message ?: "결제사가 취소를 거부했다")
                }
                jdbc.sql("insert into payment_cancel (payment_id, cancel_key, amount_minor, gateway_tx_id, cancelled_at) values (?,?,?,?,?)")
                    .params(row[0], key, r.amount.minor, c.txId, Timestamp.from(c.cancelledAt)).update()
                jdbc.sql("update payment set state=?, cancelled_minor=?, updated_at=now() where id=?")
                    .params(PaymentCodec.encode(r.state), PaymentCodec.cancelled(r.state), row[0]).update()
                val full = r.state is PaymentState.FullyCancelled
                outbox.add("PAYMENT_CANCELLED", orderId, mapOf("orderId" to orderId, "storeId" to storeId, "paymentRef" to ref, "cancelKey" to key,
                    "amountMinor" to r.amount.minor, "cancelledAt" to c.cancelledAt.toString(), "fullyCancelled" to full))
                if (full) {
                    jdbc.sql("update orders set status='CANCELLED', updated_at=now() where id=?").params(orderId).update()
                    payments.releaseStock(orderId)
                    cache.invalidateAfterCommit(storeId)
                }
                meters.counter("orderlane.cancellations", "result", if (full) "full" else "partial").increment()
                stored(200, cancelBody(view(orderId), r.amount.minor))
            }
        }
    }

    private fun cancelBody(v: OrderView, applied: Long) =
        mapOf("orderId" to v.orderId, "status" to v.status, "paymentStatus" to v.paymentStatus, "appliedMinor" to applied,
            "cancelledMinor" to v.cancelledMinor, "remainingMinor" to v.remainingMinor)
}
