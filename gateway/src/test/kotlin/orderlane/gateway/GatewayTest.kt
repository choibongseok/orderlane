package orderlane.gateway

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayTest {
    private val mapper = jacksonObjectMapper()
    private val http = HttpClient.newHttpClient()
    private lateinit var gw: GatewayServer
    private val base get() = "http://127.0.0.1:${gw.port}"

    @BeforeAll fun up() { gw = GatewayServer(GatewayConfig(port = 0, lateResponseMs = 300, holdMs = 600, callbackDelayMs = 10)).start() }
    @AfterAll fun down() { gw.stop() }

    private fun post(path: String, body: Map<String, Any?>): Pair<Int, Map<String, Any?>> {
        val r = http.send(HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString())
        return r.statusCode() to mapper.readValue(r.body(), Map::class.java).mapKeys { it.key.toString() }
    }

    private fun get(path: String): Pair<Int, String> {
        val r = http.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString())
        return r.statusCode() to r.body()
    }

    private fun pay(token: String, amount: Long = 1000): Pair<String, Pair<Int, Map<String, Any?>>> {
        val ref = UUID.randomUUID().toString()
        return ref to post("/payments", mapOf("paymentRef" to ref, "amount" to amount, "cardToken" to token))
    }

    @Test fun approvesAndDeclinesByToken() {
        assertThat(pay("tok_approve").second.second["status"]).isEqualTo("APPROVED")
        assertThat(pay("tok_decline").second.second["status"]).isEqualTo("DECLINED")
    }

    @Test fun samePaymentRefReturnsTheFirstResult() {
        val (ref, first) = pay("tok_approve")
        val again = post("/payments", mapOf("paymentRef" to ref, "amount" to 1000, "cardToken" to "tok_decline"))
        assertThat(again.second["txId"]).isEqualTo(first.second["txId"])
        assertThat(again.second["status"]).isEqualTo("APPROVED")
    }

    @Test fun lateResponseStillRecordsTheApprovalForInquiry() {
        val start = System.currentTimeMillis()
        val (ref, _) = pay("tok_timeout_approved")
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(300)
        assertThat(mapper.readValue(get("/payments/$ref").second, Map::class.java)["status"]).isEqualTo("APPROVED")
    }

    @Test fun lostRequestIsNotFoundOnInquiry() {
        val (ref, r) = pay("tok_timeout_lost")
        assertThat(r.first).isEqualTo(504)
        assertThat(get("/payments/$ref").first).isEqualTo(404)
    }

    @Test fun holdStaysPendingThenApproves() {
        val ref = UUID.randomUUID().toString()
        val t = Thread { post("/payments", mapOf("paymentRef" to ref, "amount" to 500, "cardToken" to "tok_hold")) }.also { it.start() }
        Thread.sleep(100)
        assertThat(get("/payments/$ref").second).contains("PENDING")
        t.join()
        Thread.sleep(400)
        assertThat(get("/payments/$ref").second).contains("APPROVED")
    }

    @Test fun cancelIsIdempotentByKeyAndCappedAtRemaining() {
        val (ref, _) = pay("tok_approve", 1000)
        val c1 = post("/payments/$ref/cancel", mapOf("cancelKey" to "k1", "amount" to 400))
        val c1b = post("/payments/$ref/cancel", mapOf("cancelKey" to "k1", "amount" to 400))
        assertThat(c1b.second["cancelTxId"]).isEqualTo(c1.second["cancelTxId"])
        assertThat(c1b.second["remaining"]).isEqualTo(600)
        assertThat(post("/payments/$ref/cancel", mapOf("cancelKey" to "k2", "amount" to 700)).first).isEqualTo(422)
    }

    @Test fun settlementFileListsSalesAndInjectedAnomalies() {
        val (ref, _) = pay("tok_approve", 7777)
        val (ref2, _) = pay("tok_approve", 8888)
        val date = LocalDate.now(ZoneId.of("Asia/Seoul")).toString()
        val clean = get("/settlement-file?date=$date").second
        assertThat(clean).contains(",SALE,$ref,7777").contains(",SALE,$ref2,8888")
        post("/admin/anomalies", mapOf("missing" to listOf(ref), "amountDiff" to mapOf(ref2 to 1), "duplicate" to listOf(ref2)))
        val dirty = get("/settlement-file?date=$date").second
        assertThat(dirty).doesNotContain(ref).contains(",SALE,$ref2,1")
        assertThat(dirty.lines().count { it.contains(ref2) }).isEqualTo(2)
        post("/admin/reset", emptyMap<String, Any>())
    }
}
