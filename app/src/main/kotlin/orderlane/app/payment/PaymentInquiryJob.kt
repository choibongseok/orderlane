package orderlane.app.payment

import orderlane.domain.Source
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

/** 결과 불명(UNKNOWN)·보류(HELD) 결제를 게이트웨이에 조회해 확정한다. 확정되지 않으면 간격을 두고 계속 다시 본다. */
@Component
class PaymentInquiryJob(private val jdbc: JdbcClient, private val gateway: GatewayClient, private val payments: PaymentService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${orderlane.payment.poll-ms}")
    fun poll() {
        val due = jdbc.sql("select payment_ref from payment where state in ('UNKNOWN','HELD') and next_inquiry_at <= now() order by next_inquiry_at limit 20")
            .query(String::class.java).list()
        for (ref in due) {
            MDC.put("traceId", "inquiry-" + UUID.randomUUID().toString().take(8))
            try {
                val r = gateway.inquire(ref)
                payments.apply(ref, r.outcome, Source.INQUIRY, r.txId, r.approvedAt)
            } catch (e: Exception) {
                log.warn("결제 재조회 실패 ref={} {}", ref, e.message)
            } finally { MDC.remove("traceId") }
        }
    }
}
