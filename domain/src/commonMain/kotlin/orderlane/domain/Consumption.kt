package orderlane.domain

data class RetryPolicy(val maxAttempts: Int = 3, val baseDelayMs: Long = 200) {
    init { require(maxAttempts >= 1) }
}

sealed interface ConsumeDecision {
    data object Process : ConsumeDecision
    data object SkipDuplicate : ConsumeDecision
    data class Retry(val nextAttempt: Int, val delayMs: Long) : ConsumeDecision
    data object DeadLetter : ConsumeDecision
}

/** 이벤트 소비 규칙: 이벤트 ID 로 중복을 걸러내고, 실패하면 정해진 횟수만 재시도한 뒤 dead-letter 로 보낸다. */
object Consumption {
    fun onArrival(alreadyProcessed: Boolean): ConsumeDecision =
        if (alreadyProcessed) ConsumeDecision.SkipDuplicate else ConsumeDecision.Process

    /** [attempt] 는 방금 실패한 시도의 번호(1부터). 지연은 시도마다 두 배. */
    fun onFailure(attempt: Int, policy: RetryPolicy): ConsumeDecision =
        if (attempt >= policy.maxAttempts) ConsumeDecision.DeadLetter
        else ConsumeDecision.Retry(attempt + 1, policy.baseDelayMs shl (attempt - 1))
}
