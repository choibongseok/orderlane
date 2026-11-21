package orderlane.app.idem

import orderlane.app.OrderlaneProperties
import orderlane.app.web.ApiException
import orderlane.domain.IdemDecision
import orderlane.domain.IdemRecord
import orderlane.domain.IdemStatus
import orderlane.domain.Idempotency
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Clock

data class Stored(val status: Int, val body: String)

fun sha256(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

/** 멱등 키 저장소. 판정 규칙은 도메인의 [Idempotency.decide], 여기서는 DB 에 선점·완료를 기록한다. */
@Service
class IdempotencyService(
    private val jdbc: JdbcClient,
    private val clock: Clock,
    private val props: OrderlaneProperties,
    private val meters: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun load(scope: String, key: String): IdemRecord? =
        jdbc.sql("select request_hash, status, locked_until, response_status, response_body from idempotency_key where scope=? and key=?")
            .params(scope, key)
            .query { rs, _ ->
                IdemRecord(rs.getString(1), IdemStatus.valueOf(rs.getString(2)), rs.getTimestamp(3).time,
                    rs.getObject(4) as Int?, rs.getString(5))
            }.optional().orElse(null)

    /**
     * [block] 은 이 요청이 처리를 맡을 때만 실행된다. 인자는 "이전 처리자가 죽어서 이어받는 중인가".
     * 처리 중 예외가 나면 선점을 풀어 같은 요청이 다시 들어올 수 있게 한다(작업은 키로 다시 찾을 수 있게 만들어 둔다).
     */
    fun execute(scope: String, key: String, requestHash: String, block: (reclaimed: Boolean) -> Stored): Stored {
        var reclaimed = false
        var attempts = 0
        while (true) {
            val now = clock.millis()
            val inserted = jdbc.sql("insert into idempotency_key (scope, key, request_hash, status, locked_until) values (?,?,?,'IN_PROGRESS',?) on conflict do nothing")
                .params(scope, key, requestHash, Timestamp(now + props.idempotency.lockMs)).update()
            if (inserted == 1) break
            val existing = load(scope, key)
            when (val d = Idempotency.decide(existing, requestHash, now)) {
                IdemDecision.Proceed -> { if (++attempts > 3) throw ApiException(409, "IDEMPOTENCY_IN_PROGRESS", "같은 키의 요청이 처리 중이다"); continue }
                is IdemDecision.Replay -> { count("replayed"); return Stored(d.status, d.body) }
                IdemDecision.Mismatch -> { count("key_reused"); throw ApiException(422, "IDEMPOTENCY_KEY_REUSED", "같은 Idempotency-Key 로 다른 본문을 보냈다") }
                IdemDecision.InProgress -> { count("in_progress"); throw ApiException(409, "IDEMPOTENCY_IN_PROGRESS", "같은 키의 요청이 처리 중이다") }
                IdemDecision.Reclaim -> {
                    val took = jdbc.sql("update idempotency_key set locked_until=? where scope=? and key=? and status='IN_PROGRESS' and locked_until<=?")
                        .params(Timestamp(now + props.idempotency.lockMs), scope, key, Timestamp(now)).update()
                    if (took == 0) { count("in_progress"); throw ApiException(409, "IDEMPOTENCY_IN_PROGRESS", "같은 키의 요청이 처리 중이다") }
                    log.info("멱등 키 이어받기 scope={} key={}", scope, key)
                    count("reclaimed")
                    reclaimed = true
                    break
                }
            }
        }
        val stored = try {
            block(reclaimed)
        } catch (e: Throwable) {
            jdbc.sql("delete from idempotency_key where scope=? and key=? and status='IN_PROGRESS'").params(scope, key).update()
            throw e
        }
        jdbc.sql("update idempotency_key set status='COMPLETED', response_status=?, response_body=? where scope=? and key=?")
            .params(stored.status, stored.body, scope, key).update()
        return stored
    }

    private fun count(result: String) = meters.counter("orderlane.idempotency", "result", result).increment()
}
