package orderlane.app.web

import com.fasterxml.jackson.databind.ObjectMapper
import orderlane.app.cache.MenuCache
import orderlane.app.idem.Stored
import orderlane.app.order.CancelRequest
import orderlane.app.order.OrderRequest
import orderlane.app.order.OrderService
import orderlane.app.payment.PaymentNotFoundException
import orderlane.app.payment.PaymentService
import orderlane.domain.Outcome
import orderlane.domain.Source
import orderlane.domain.Transition
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

private fun Stored.response(): ResponseEntity<String> =
    ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)

private fun requireKey(key: String) {
    if (key.isBlank() || key.length > 128) throw ApiException(400, "BAD_IDEMPOTENCY_KEY", "Idempotency-Key 는 1..128자")
}

@RestController
class OrderController(private val orders: OrderService, private val mapper: ObjectMapper) {
    @PostMapping("/orders", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun place(@RequestHeader("Idempotency-Key") key: String, @RequestBody req: OrderRequest): ResponseEntity<String> {
        requireKey(key)
        return orders.place(key, req).response()
    }

    @GetMapping("/orders/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(@PathVariable id: Long) = orders.view(id)

    @PostMapping("/orders/{id}/cancellations", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun cancel(@PathVariable id: Long, @RequestHeader("Idempotency-Key") key: String, @RequestBody(required = false) req: CancelRequest?): ResponseEntity<String> {
        requireKey(key)
        return orders.cancel(id, key, req ?: CancelRequest()).response()
    }
}

@RestController
class StoreController(private val jdbc: JdbcClient, private val cache: MenuCache, private val mapper: ObjectMapper) {
    @GetMapping("/stores/{id}/menu", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun menu(@PathVariable id: Long): String = cache.get(id) {
        val items = jdbc.sql("select m.id, m.name, m.price, s.available from menu_item m join stock s on s.menu_item_id=m.id where m.store_id=? order by m.id").params(id)
            .query { rs, _ -> mapOf("id" to rs.getLong(1), "name" to rs.getString(2), "priceMinor" to rs.getLong(3), "available" to rs.getInt(4)) }.list()
        val opts = jdbc.sql("select o.id, o.menu_item_id, o.name, o.extra_price from menu_option o join menu_item m on m.id=o.menu_item_id where m.store_id=? order by o.id").params(id)
            .query { rs, _ -> rs.getLong(2) to mapOf("id" to rs.getLong(1), "name" to rs.getString(3), "extraMinor" to rs.getLong(4)) }.list()
        mapper.writeValueAsString(mapOf("storeId" to id, "items" to items.map { it + ("options" to opts.filter { o -> o.first == it["id"] }.map { o -> o.second }) }))
    }

    /** 주방 표시용 조회 모델: 결제가 끝난(NEW) 주문만 오래된 순으로 보인다. */
    @GetMapping("/stores/{id}/kitchen", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun kitchen(@PathVariable id: Long): List<Map<String, Any?>> =
        jdbc.sql("select order_id, status, items from kitchen_ticket where store_id=? and status='NEW' order by order_id").params(id)
            .query { rs, _ -> mapOf("orderId" to rs.getLong(1), "status" to rs.getString(2), "items" to mapper.readTree(rs.getString(3))) }.list()
}

data class CallbackRequest(val paymentRef: String, val seq: Int, val type: String)

@RestController
class CallbackController(private val payments: PaymentService) {
    @PostMapping("/callbacks/payments", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun callback(@RequestBody req: CallbackRequest): ResponseEntity<Map<String, Any?>> {
        val outcome = when (req.type) {
            "APPROVED" -> Outcome.APPROVED
            "DECLINED" -> Outcome.DECLINED
            "PENDING" -> Outcome.PENDING
            else -> throw ApiException(400, "BAD_CALLBACK", "알 수 없는 type: ${req.type}")
        }
        return try {
            val t = payments.apply(req.paymentRef, outcome, Source.CALLBACK)
            val (result, detail) = when (t) {
                is Transition.Applied -> "applied" to t.state.name
                is Transition.Ignored -> "ignored" to t.reason
                is Transition.Rejected -> "rejected" to t.code
            }
            // 거부(불일치)도 200: 재전송해도 결과가 달라지지 않으니 게이트웨이가 계속 보내지 않게 한다
            ResponseEntity.ok(mapOf("result" to result, "detail" to detail, "seq" to req.seq))
        } catch (e: PaymentNotFoundException) {
            ResponseEntity.status(404).body(mapOf("result" to "unknown-payment"))
        }
    }
}
