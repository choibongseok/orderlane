package orderlane.domain

/** 결제 상태. 상태마다 가질 수 있는 값이 달라서 sealed 로 나눠 잘못된 조합을 만들 수 없게 한다. */
sealed interface PaymentState {
    val amount: Money
    val name: String

    data class Requested(override val amount: Money) : PaymentState { override val name get() = "REQUESTED" }
    data class Unknown(override val amount: Money, val inquiries: Int) : PaymentState { override val name get() = "UNKNOWN" }
    data class Held(override val amount: Money, val inquiries: Int) : PaymentState { override val name get() = "HELD" }
    data class Declined(override val amount: Money) : PaymentState { override val name get() = "DECLINED" }
    data class Approved(override val amount: Money, val cancelled: Money = Money.ZERO) : PaymentState {
        init { require(cancelled.minor in 0 until amount.minor) { "부분 취소 금액이 범위를 벗어남" } }
        val remaining: Money get() = amount - cancelled
        override val name get() = if (cancelled.minor == 0L) "APPROVED" else "PARTIALLY_CANCELLED"
    }
    data class FullyCancelled(override val amount: Money) : PaymentState { override val name get() = "CANCELLED" }

    val terminal: Boolean get() = this is Declined || this is FullyCancelled
    val resolved: Boolean get() = this is Approved || this is Declined || this is FullyCancelled
}

enum class Outcome { APPROVED, DECLINED, PENDING, NOT_FOUND, TIMEOUT }
enum class Source { REQUEST, INQUIRY, CALLBACK }

data class PaymentPolicy(val maxInquiries: Int = 3)

sealed interface Transition {
    val state: PaymentState
    /** 상태가 실제로 바뀌었다. */
    data class Applied(override val state: PaymentState) : Transition
    /** 이미 반영된 통지(중복)이거나 더 오래된 통지(순서 뒤바뀜)라 무시한다. */
    data class Ignored(override val state: PaymentState, val reason: String) : Transition
    /** 허용되지 않는 전이. 상태는 그대로 두고 사람이 봐야 할 불일치로 남긴다. */
    data class Rejected(override val state: PaymentState, val code: String, val message: String) : Transition
}

object PaymentMachine {
    fun initial(amount: Money): PaymentState {
        require(amount.minor > 0) { "결제 금액은 0 보다 커야 함" }
        return PaymentState.Requested(amount)
    }

    fun on(state: PaymentState, outcome: Outcome, source: Source, policy: PaymentPolicy = PaymentPolicy()): Transition {
        val amount = state.amount
        return when (state) {
            is PaymentState.Requested -> when {
                outcome == Outcome.PENDING -> Transition.Ignored(state, "stale-pending")
                outcome == Outcome.APPROVED && source != Source.INQUIRY -> Transition.Applied(PaymentState.Approved(amount))
                outcome == Outcome.DECLINED && source != Source.INQUIRY -> Transition.Applied(PaymentState.Declined(amount))
                outcome == Outcome.TIMEOUT && source == Source.REQUEST -> Transition.Applied(PaymentState.Unknown(amount, 0))
                else -> notApplicable(state, outcome, source)
            }
            is PaymentState.Unknown -> unresolved(state, state.inquiries, outcome, source, policy) { n -> PaymentState.Unknown(amount, n) }
            is PaymentState.Held -> unresolved(state, state.inquiries, outcome, source, policy) { n -> PaymentState.Held(amount, n) }
            is PaymentState.Approved, is PaymentState.FullyCancelled -> when (outcome) {
                Outcome.APPROVED -> Transition.Ignored(state, "duplicate-approved")
                Outcome.PENDING -> Transition.Ignored(state, "stale-pending")
                Outcome.DECLINED -> Transition.Rejected(state, "conflict", "${state.name} 결제에 거절 통지가 왔다")
                else -> notApplicable(state, outcome, source)
            }
            is PaymentState.Declined -> when (outcome) {
                Outcome.DECLINED, Outcome.NOT_FOUND -> Transition.Ignored(state, "duplicate-declined")
                Outcome.PENDING -> Transition.Ignored(state, "stale-pending")
                Outcome.APPROVED -> Transition.Rejected(state, "conflict", "거절 확정된 결제에 승인 통지가 왔다")
                else -> notApplicable(state, outcome, source)
            }
        }
    }

    private fun unresolved(
        state: PaymentState, inquiries: Int, outcome: Outcome, source: Source, policy: PaymentPolicy,
        pendingState: (Int) -> PaymentState,
    ): Transition {
        val amount = state.amount
        return when {
            outcome == Outcome.APPROVED -> Transition.Applied(PaymentState.Approved(amount))
            outcome == Outcome.DECLINED || (outcome == Outcome.NOT_FOUND && source == Source.INQUIRY) ->
                Transition.Applied(PaymentState.Declined(amount))
            outcome == Outcome.PENDING && source == Source.INQUIRY -> {
                val n = inquiries + 1
                if (state is PaymentState.Held || n >= policy.maxInquiries) Transition.Applied(PaymentState.Held(amount, n))
                else Transition.Applied(pendingState(n))
            }
            outcome == Outcome.PENDING -> Transition.Ignored(state, "stale-pending")
            outcome == Outcome.TIMEOUT && source == Source.INQUIRY -> Transition.Applied(
                // 조회 자체가 시간 초과면 결과를 알 수 없는 채로 한 번 더 센다
                if (state is PaymentState.Held || inquiries + 1 >= policy.maxInquiries) PaymentState.Held(amount, inquiries + 1) else pendingState(inquiries + 1))
            outcome == Outcome.TIMEOUT -> Transition.Ignored(state, "duplicate-timeout")
            else -> notApplicable(state, outcome, source)
        }
    }

    private fun notApplicable(state: PaymentState, outcome: Outcome, source: Source) =
        Transition.Rejected(state, "not-applicable", "${state.name} 상태에서 $source/$outcome 는 허용되지 않음")

    sealed interface CancelResult {
        data class Applied(val state: PaymentState, val amount: Money) : CancelResult
        /** 같은 취소 키로 이미 처리됨. 처음 응답과 같은 금액을 돌려준다. */
        data class Replayed(val state: PaymentState, val amount: Money) : CancelResult
        data class Rejected(val code: String, val message: String) : CancelResult
    }

    /**
     * 취소. [amount] 가 null 이면 남은 금액 전부.
     * [appliedCancels] 는 이미 처리한 취소 키 → 금액. 같은 키는 다시 적용하지 않는다.
     */
    fun cancel(state: PaymentState, key: String, amount: Money?, appliedCancels: Map<String, Money>): CancelResult {
        appliedCancels[key]?.let { return CancelResult.Replayed(state, it) }
        if (state !is PaymentState.Approved) {
            return CancelResult.Rejected("not-cancellable", "${state.name} 상태는 취소할 수 없다")
        }
        val take = amount ?: state.remaining
        if (take.minor <= 0) return CancelResult.Rejected("invalid-amount", "취소 금액은 0 보다 커야 한다")
        if (take > state.remaining) {
            return CancelResult.Rejected("exceeds-remaining", "취소 금액 ${take.minor} 이 남은 금액 ${state.remaining.minor} 을 넘는다")
        }
        val next = if (take == state.remaining) PaymentState.FullyCancelled(state.amount)
        else PaymentState.Approved(state.amount, state.cancelled + take)
        return CancelResult.Applied(next, take)
    }
}
