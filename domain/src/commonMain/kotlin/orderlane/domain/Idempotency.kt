package orderlane.domain

enum class IdemStatus { IN_PROGRESS, COMPLETED }

data class IdemRecord(
    val requestHash: String,
    val status: IdemStatus,
    /** IN_PROGRESS 일 때 이 시각까지는 다른 요청이 처리 중인 것으로 본다(epoch ms). */
    val lockedUntilMs: Long,
    val responseStatus: Int? = null,
    val responseBody: String? = null,
)

sealed interface IdemDecision {
    /** 처음 보는 키. 이 요청이 처리한다. */
    data object Proceed : IdemDecision
    /** 같은 키 + 같은 본문 + 완료됨. 저장한 응답을 그대로 돌려준다. */
    data class Replay(val status: Int, val body: String) : IdemDecision
    /** 같은 키 + 다른 본문. 거부한다. */
    data object Mismatch : IdemDecision
    /** 같은 키 + 같은 본문이 아직 처리 중. 충돌 응답. */
    data object InProgress : IdemDecision
    /** 처리 중이었는데 잠금 시간이 지났다. 처리하던 쪽이 죽은 것으로 보고 이어받는다. */
    data object Reclaim : IdemDecision
}

object Idempotency {
    fun decide(existing: IdemRecord?, requestHash: String, nowMs: Long): IdemDecision {
        if (existing == null) return IdemDecision.Proceed
        if (existing.requestHash != requestHash) return IdemDecision.Mismatch
        return when (existing.status) {
            IdemStatus.COMPLETED -> IdemDecision.Replay(existing.responseStatus!!, existing.responseBody!!)
            IdemStatus.IN_PROGRESS ->
                if (nowMs < existing.lockedUntilMs) IdemDecision.InProgress else IdemDecision.Reclaim
        }
    }
}
