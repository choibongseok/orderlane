package orderlane.gateway

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * 모의 결제 게이트웨이. 카드 토큰으로 동작을 고른다(실제 결제사의 테스트 토큰과 같은 방식).
 *
 *  tok_approve                 즉시 승인
 *  tok_decline                 즉시 거절
 *  tok_slow_<ms>               <ms> 뒤 승인 응답
 *  tok_timeout_approved        승인해 두고 응답만 늦게 보낸다(콜백 없음) → 조회로만 알 수 있다
 *  tok_timeout_callbacks       승인해 두고 응답이 늦다. 콜백은 승인#2, 승인#2(중복), 처리중#1(뒤늦게) 순
 *  tok_timeout_lost            요청을 받지 못한 것처럼 기록 없이 응답만 늦다 → 조회는 NOT_FOUND
 *  tok_hold                    한동안 처리 중으로 두었다가 승인 → 조회는 PENDING 이다가 APPROVED
 */
data class GatewayConfig(
    val port: Int = 8090,
    val lateResponseMs: Long = 3_000,
    val holdMs: Long = 8_000,
    val callbackDelayMs: Long = 200,
)

class GatewayServer(private val config: GatewayConfig = GatewayConfig()) {
    private data class Cancel(val key: String, val amount: Long, val txId: String, val at: Instant)

    private class Payment(
        val ref: String, val amount: Long, val txId: String,
        @Volatile var status: String, val at: Instant, @Volatile var pendingUntil: Long = 0,
    ) {
        val cancels = ArrayList<Cancel>()
        val cancelled get() = cancels.sumOf { it.amount }
    }

    private val mapper = jacksonObjectMapper()
    private val payments = ConcurrentHashMap<String, Payment>()
    private val seq = AtomicLong(1000)
    private val anomalies = ConcurrentHashMap<String, JsonNode>()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    private val zone = ZoneId.of("Asia/Seoul")
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private lateinit var server: HttpServer

    val port: Int get() = server.address.port

    fun start(): GatewayServer {
        server = HttpServer.create(InetSocketAddress(config.port), 0)
        server.executor = executor
        server.createContext("/") { ex -> safely(ex) { route(ex) } }
        server.start()
        return this
    }

    fun stop() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun safely(ex: HttpExchange, block: () -> Unit) {
        try { block() } catch (e: Exception) {
            runCatching { reply(ex, 500, mapOf("error" to (e.message ?: e.javaClass.simpleName))) }
        } finally { ex.close() }
    }

    private fun route(ex: HttpExchange) {
        val path = ex.requestURI.path
        val m = ex.requestMethod
        when {
            m == "GET" && path == "/health" -> reply(ex, 200, mapOf("status" to "UP"))
            m == "POST" && path == "/payments" -> authorize(ex)
            m == "GET" && path.startsWith("/payments/") -> inquire(ex, path.removePrefix("/payments/"))
            m == "POST" && path.matches(Regex("/payments/[^/]+/cancel")) -> cancel(ex, path.split('/')[2])
            m == "GET" && path == "/settlement-file" -> settlementFile(ex)
            m == "POST" && path == "/admin/anomalies" -> { val b = mapper.readTree(ex.requestBody); b.fields().forEach { anomalies[it.key] = it.value }; reply(ex, 200, mapOf("ok" to true)) }
            m == "POST" && path == "/admin/reset" -> { payments.clear(); anomalies.clear(); reply(ex, 200, mapOf("ok" to true)) }
            else -> reply(ex, 404, mapOf("error" to "not found"))
        }
    }

    private fun authorize(ex: HttpExchange) {
        val body = mapper.readTree(ex.requestBody)
        val ref = body["paymentRef"].asText()
        val amount = body["amount"].asLong()
        val token = body["cardToken"]?.asText() ?: "tok_approve"
        val callbackUrl = body["callbackUrl"]?.asText()
        payments[ref]?.let { // 같은 참조로 다시 오면 처음 결과를 그대로 준다
            reply(ex, 200, view(it)); return
        }
        val now = Instant.now()
        val txId = "tx-${seq.incrementAndGet()}"
        when {
            token == "tok_decline" -> { payments[ref] = Payment(ref, amount, txId, "DECLINED", now); reply(ex, 200, view(payments[ref]!!)) }
            token.startsWith("tok_slow_") -> {
                Thread.sleep(token.removePrefix("tok_slow_").toLong())
                payments[ref] = Payment(ref, amount, txId, "APPROVED", Instant.now()); reply(ex, 200, view(payments[ref]!!))
            }
            token == "tok_timeout_approved" -> { payments[ref] = Payment(ref, amount, txId, "APPROVED", now); late(ex, ref) }
            token == "tok_timeout_callbacks" -> {
                payments[ref] = Payment(ref, amount, txId, "APPROVED", now)
                if (callbackUrl != null) executor.execute {
                    Thread.sleep(config.callbackDelayMs)
                    callback(callbackUrl, ref, 2, "APPROVED"); callback(callbackUrl, ref, 2, "APPROVED"); callback(callbackUrl, ref, 1, "PENDING")
                }
                late(ex, ref)
            }
            token == "tok_timeout_lost" -> { Thread.sleep(config.lateResponseMs); reply(ex, 504, mapOf("error" to "upstream timeout")) }
            token == "tok_hold" -> {
                payments[ref] = Payment(ref, amount, txId, "PENDING", now, pendingUntil = System.currentTimeMillis() + config.holdMs)
                late(ex, ref)
            }
            else -> { payments[ref] = Payment(ref, amount, txId, "APPROVED", now); reply(ex, 200, view(payments[ref]!!)) }
        }
    }

    private fun late(ex: HttpExchange, ref: String) {
        Thread.sleep(config.lateResponseMs)
        runCatching { reply(ex, 200, view(payments.getValue(ref))) } // 클라이언트는 이미 떠났을 수 있다
    }

    private fun callback(url: String, ref: String, seq: Int, type: String) {
        runCatching {
            http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(mapOf("paymentRef" to ref, "seq" to seq, "type" to type)))).build(),
                HttpResponse.BodyHandlers.discarding())
        }
    }

    private fun current(p: Payment): String =
        if (p.status == "PENDING" && System.currentTimeMillis() >= p.pendingUntil) "APPROVED" else p.status

    private fun view(p: Payment): Map<String, Any?> {
        val s = current(p)
        return mapOf("paymentRef" to p.ref, "txId" to p.txId, "status" to s, "amount" to p.amount, "cancelledAmount" to p.cancelled,
            "approvedAt" to if (s == "APPROVED") p.at.toString() else null)
    }

    private fun inquire(ex: HttpExchange, ref: String) {
        val p = payments[ref] ?: return reply(ex, 404, mapOf("paymentRef" to ref, "status" to "NOT_FOUND"))
        reply(ex, 200, view(p))
    }

    private fun cancel(ex: HttpExchange, ref: String) {
        val body = mapper.readTree(ex.requestBody)
        val key = body["cancelKey"].asText()
        val amount = body["amount"].asLong()
        val p = payments[ref] ?: return reply(ex, 404, mapOf("error" to "NOT_FOUND"))
        synchronized(p) {
            p.cancels.firstOrNull { it.key == key }?.let { return reply(ex, 200, cancelView(p, it)) }
            if (current(p) != "APPROVED") return reply(ex, 409, mapOf("error" to "NOT_APPROVED"))
            if (amount <= 0 || amount > p.amount - p.cancelled) return reply(ex, 422, mapOf("error" to "AMOUNT_EXCEEDS_REMAINING"))
            val c = Cancel(key, amount, "tx-${seq.incrementAndGet()}", Instant.now())
            p.cancels += c
            reply(ex, 200, cancelView(p, c))
        }
    }

    private fun cancelView(p: Payment, c: Cancel) =
        mapOf("paymentRef" to p.ref, "cancelKey" to c.key, "cancelTxId" to c.txId, "amount" to c.amount, "cancelledAt" to c.at.toString(), "remaining" to p.amount - p.cancelled)

    private data class Row(val txId: String, val kind: String, val ref: String, val amount: Long)

    /** 거래 내역 파일(CSV). 이상 주입은 /admin/anomalies 로: missing=[ref], amountDiff={ref: 금액}, duplicate=[ref]. */
    private fun settlementFile(ex: HttpExchange) {
        val date = ex.requestURI.query?.split('&')?.firstOrNull { it.startsWith("date=") }?.removePrefix("date=") ?: error("date 필요")
        val rows = ArrayList<Row>()
        for (p in payments.values.sortedBy { it.txId }) {
            if (current(p) == "APPROVED" && p.at.atZone(zone).toLocalDate().toString() == date) rows += Row(p.txId, "SALE", p.ref, p.amount)
            for (c in p.cancels) if (c.at.atZone(zone).toLocalDate().toString() == date) rows += Row(c.txId, "CANCEL", c.key, c.amount)
        }
        val missing = anomalies["missing"]?.map { it.asText() }?.toSet() ?: emptySet()
        val diff = anomalies["amountDiff"]?.fields()?.asSequence()?.associate { it.key to it.value.asLong() } ?: emptyMap()
        val dup = anomalies["duplicate"]?.map { it.asText() }?.toSet() ?: emptySet()
        val out = ArrayList<Row>()
        for (r in rows) {
            if (r.ref in missing) continue
            val row = diff[r.ref]?.let { r.copy(amount = it) } ?: r
            out += row
            if (r.ref in dup) out += row.copy(txId = row.txId + "-dup")
        }
        val csv = buildString {
            append("txId,kind,ref,amount\n")
            out.forEach { append("${it.txId},${it.kind},${it.ref},${it.amount}\n") }
        }
        val bytes = csv.toByteArray()
        ex.responseHeaders.add("Content-Type", "text/csv; charset=utf-8")
        ex.sendResponseHeaders(200, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    private fun reply(ex: HttpExchange, status: Int, body: Any) {
        val bytes = mapper.writeValueAsBytes(body)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(status, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }
}
