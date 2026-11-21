package orderlane.app.payment

import com.fasterxml.jackson.databind.JsonNode
import orderlane.app.OrderlaneProperties
import orderlane.domain.Outcome
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.net.http.HttpClient
import java.time.Duration
import java.time.Instant

data class GatewayResult(val outcome: Outcome, val txId: String? = null, val approvedAt: Instant? = null)

class GatewayUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class GatewayRejectedException(val status: Int, message: String) : RuntimeException(message)

data class CancelConfirmation(val txId: String, val cancelledAt: Instant, val remaining: Long)

/** 모의 결제 게이트웨이 클라이언트. 응답을 못 받으면 "모른다"(TIMEOUT)로 돌려주고, 판단은 상태 기계가 한다. */
@Component
class GatewayClient(private val props: OrderlaneProperties) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()

    private fun client(readTimeoutMs: Long): RestClient {
        val factory = JdkClientHttpRequestFactory(http).apply { setReadTimeout(Duration.ofMillis(readTimeoutMs)) }
        return RestClient.builder().baseUrl(props.gateway.baseUrl).requestFactory(factory).build()
    }

    private val payClient = client(props.gateway.timeoutMs)
    private val inquiryClient = client(props.gateway.timeoutMs)
    private val fileClient = client(10_000)

    fun authorize(ref: String, amount: Long, token: String): GatewayResult = try {
        val body = payClient.post().uri("/payments").header("X-Request-Id", MDC.get("traceId") ?: "")
            .body(mapOf("paymentRef" to ref, "amount" to amount, "cardToken" to token, "callbackUrl" to props.gateway.callbackUrl))
            .retrieve().body(JsonNode::class.java)!!
        parse(body)
    } catch (e: RestClientException) {
        log.warn("승인 요청 응답 없음(결과 불명) ref={} cause={}", ref, e.javaClass.simpleName)
        GatewayResult(Outcome.TIMEOUT)
    }

    fun inquire(ref: String): GatewayResult = try {
        parse(inquiryClient.get().uri("/payments/{ref}", ref).header("X-Request-Id", MDC.get("traceId") ?: "").retrieve().body(JsonNode::class.java)!!)
    } catch (e: HttpClientErrorException.NotFound) {
        GatewayResult(Outcome.NOT_FOUND)
    } catch (e: RestClientException) {
        GatewayResult(Outcome.TIMEOUT)
    }

    private fun parse(n: JsonNode): GatewayResult {
        val outcome = when (n["status"].asText()) {
            "APPROVED" -> Outcome.APPROVED
            "DECLINED" -> Outcome.DECLINED
            "PENDING" -> Outcome.PENDING
            "NOT_FOUND" -> Outcome.NOT_FOUND
            else -> Outcome.TIMEOUT
        }
        return GatewayResult(outcome, n["txId"]?.asText(), n["approvedAt"]?.takeIf { !it.isNull }?.asText()?.let(Instant::parse))
    }

    fun cancel(ref: String, cancelKey: String, amount: Long): CancelConfirmation = try {
        val n = payClient.post().uri("/payments/{ref}/cancel", ref).header("X-Request-Id", MDC.get("traceId") ?: "")
            .body(mapOf("cancelKey" to cancelKey, "amount" to amount)).retrieve().body(JsonNode::class.java)!!
        CancelConfirmation(n["cancelTxId"].asText(), Instant.parse(n["cancelledAt"].asText()), n["remaining"].asLong())
    } catch (e: RestClientResponseException) {
        if (e.statusCode.is4xxClientError) throw GatewayRejectedException(e.statusCode.value(), "게이트웨이가 취소를 거부했다: ${e.statusCode}")
        throw GatewayUnavailableException("게이트웨이 오류 ${e.statusCode}", e)
    } catch (e: RestClientException) {
        throw GatewayUnavailableException("게이트웨이 응답 없음", e)
    }

    fun settlementFile(date: String): String =
        fileClient.get().uri("/settlement-file?date={d}", date).retrieve().body(String::class.java) ?: ""
}
