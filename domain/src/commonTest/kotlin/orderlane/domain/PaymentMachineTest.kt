package orderlane.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaymentMachineTest {
    private val amount = Money(10_000)
    private val requested = PaymentMachine.initial(amount)

    private fun apply(s: PaymentState, o: Outcome, src: Source, p: PaymentPolicy = PaymentPolicy()) = PaymentMachine.on(s, o, src, p)

    @Test fun requestApprovedMovesToApproved() {
        assertEquals(PaymentState.Approved(amount), assertIs<Transition.Applied>(apply(requested, Outcome.APPROVED, Source.REQUEST)).state)
    }

    @Test fun requestDeclinedMovesToDeclined() {
        assertIs<PaymentState.Declined>(assertIs<Transition.Applied>(apply(requested, Outcome.DECLINED, Source.REQUEST)).state)
    }

    @Test fun requestTimeoutMovesToUnknown() {
        assertEquals(PaymentState.Unknown(amount, 0), assertIs<Transition.Applied>(apply(requested, Outcome.TIMEOUT, Source.REQUEST)).state)
    }

    @Test fun zeroAmountPaymentCannotBeCreated() {
        assertFailsWith<IllegalArgumentException> { PaymentMachine.initial(Money.ZERO) }
    }

    @Test fun callbackCanBeatTheSynchronousResponse() {
        val t = assertIs<Transition.Applied>(apply(requested, Outcome.APPROVED, Source.CALLBACK))
        assertIs<PaymentState.Approved>(t.state)
        assertIs<Transition.Ignored>(apply(t.state, Outcome.APPROVED, Source.REQUEST)) // 늦게 온 동기 응답은 중복
    }

    @Test fun inquiryIsNotAllowedWhileStillRequested() {
        assertIs<Transition.Rejected>(apply(requested, Outcome.APPROVED, Source.INQUIRY))
    }

    @Test fun unknownResolvedToApprovedByInquiry() {
        val u = PaymentState.Unknown(amount, 1)
        assertIs<PaymentState.Approved>(assertIs<Transition.Applied>(apply(u, Outcome.APPROVED, Source.INQUIRY)).state)
    }

    @Test fun unknownNotFoundByInquiryMeansDeclined() {
        val u = PaymentState.Unknown(amount, 0)
        assertIs<PaymentState.Declined>(assertIs<Transition.Applied>(apply(u, Outcome.NOT_FOUND, Source.INQUIRY)).state)
    }

    @Test fun notFoundFromACallbackDoesNotDecline() {
        assertIs<Transition.Rejected>(apply(PaymentState.Unknown(amount, 0), Outcome.NOT_FOUND, Source.CALLBACK))
    }

    @Test fun pendingInquiriesCountUpAndEndInHeld() {
        val p = PaymentPolicy(maxInquiries = 3)
        var s: PaymentState = PaymentState.Unknown(amount, 0)
        s = assertIs<Transition.Applied>(apply(s, Outcome.PENDING, Source.INQUIRY, p)).state
        assertEquals(PaymentState.Unknown(amount, 1), s)
        s = assertIs<Transition.Applied>(apply(s, Outcome.PENDING, Source.INQUIRY, p)).state
        assertEquals(PaymentState.Unknown(amount, 2), s)
        s = assertIs<Transition.Applied>(apply(s, Outcome.PENDING, Source.INQUIRY, p)).state
        assertEquals(PaymentState.Held(amount, 3), s)
    }

    @Test fun inquiryTimeoutAlsoCountsTowardHeld() {
        val s = assertIs<Transition.Applied>(apply(PaymentState.Unknown(amount, 2), Outcome.TIMEOUT, Source.INQUIRY, PaymentPolicy(3))).state
        assertIs<PaymentState.Held>(s)
    }

    @Test fun heldStaysHeldWhilePendingAndResolvesLater() {
        val h = PaymentState.Held(amount, 3)
        assertEquals(PaymentState.Held(amount, 4), assertIs<Transition.Applied>(apply(h, Outcome.PENDING, Source.INQUIRY)).state)
        assertIs<PaymentState.Approved>(assertIs<Transition.Applied>(apply(h, Outcome.APPROVED, Source.INQUIRY)).state)
        assertIs<PaymentState.Declined>(assertIs<Transition.Applied>(apply(h, Outcome.DECLINED, Source.CALLBACK)).state)
    }

    @Test fun duplicateApprovalCallbackIsIgnored() {
        val a = PaymentState.Approved(amount)
        val t = assertIs<Transition.Ignored>(apply(a, Outcome.APPROVED, Source.CALLBACK))
        assertEquals("duplicate-approved", t.reason)
        assertEquals(a, t.state)
    }

    @Test fun staleCallbackAfterResolutionIsIgnored() {
        assertEquals("stale-pending", assertIs<Transition.Ignored>(apply(PaymentState.Approved(amount), Outcome.PENDING, Source.CALLBACK)).reason)
        assertEquals("stale-pending", assertIs<Transition.Ignored>(apply(PaymentState.Declined(amount), Outcome.PENDING, Source.CALLBACK)).reason)
        assertEquals("stale-pending", assertIs<Transition.Ignored>(apply(PaymentState.Unknown(amount, 0), Outcome.PENDING, Source.CALLBACK)).reason)
    }

    @Test fun declinedAfterApprovedIsRejectedAsConflict() {
        val t = assertIs<Transition.Rejected>(apply(PaymentState.Approved(amount), Outcome.DECLINED, Source.CALLBACK))
        assertEquals("conflict", t.code)
        assertIs<Transition.Rejected>(apply(PaymentState.FullyCancelled(amount), Outcome.DECLINED, Source.CALLBACK))
    }

    @Test fun approvedAfterDeclinedIsRejectedAsConflict() {
        assertEquals("conflict", assertIs<Transition.Rejected>(apply(PaymentState.Declined(amount), Outcome.APPROVED, Source.CALLBACK)).code)
    }

    @Test fun everyStateOutcomeSourcePairHasADefinedResult() {
        val states = listOf(requested, PaymentState.Unknown(amount, 0), PaymentState.Held(amount, 3), PaymentState.Declined(amount),
            PaymentState.Approved(amount), PaymentState.Approved(amount, Money(1000)), PaymentState.FullyCancelled(amount))
        var combos = 0
        for (s in states) for (o in Outcome.entries) for (src in Source.entries) {
            val t = apply(s, o, src)
            combos++
            // 취소·거절 확정 상태에서 승인 상태로 되돌아가는 전이는 없다
            if (s is PaymentState.Declined || s is PaymentState.FullyCancelled) assertTrue(t !is Transition.Applied, "$s $o $src")
            if (t is Transition.Applied && s is PaymentState.Approved) assertTrue(false, "승인 상태는 통지로 바뀌지 않는다: $o $src")
        }
        assertEquals(states.size * Outcome.entries.size * Source.entries.size, combos)
    }

    @Test fun partialCancelKeepsApprovedWithRemaining() {
        val r = assertIs<PaymentMachine.CancelResult.Applied>(PaymentMachine.cancel(PaymentState.Approved(amount), "c1", Money(3000), emptyMap()))
        val s = assertIs<PaymentState.Approved>(r.state)
        assertEquals(Money(7000), s.remaining)
        assertEquals("PARTIALLY_CANCELLED", s.name)
    }

    @Test fun cancellingTheRemainderBecomesFullyCancelled() {
        val a = PaymentState.Approved(amount, Money(3000))
        assertIs<PaymentState.FullyCancelled>(assertIs<PaymentMachine.CancelResult.Applied>(PaymentMachine.cancel(a, "c2", null, mapOf("c1" to Money(3000)))).state)
        assertIs<PaymentState.FullyCancelled>(assertIs<PaymentMachine.CancelResult.Applied>(PaymentMachine.cancel(a, "c2", Money(7000), emptyMap())).state)
    }

    @Test fun cancelAboveRemainingIsRejected() {
        val a = PaymentState.Approved(amount, Money(9000))
        assertEquals("exceeds-remaining", assertIs<PaymentMachine.CancelResult.Rejected>(PaymentMachine.cancel(a, "c9", Money(1001), emptyMap())).code)
    }

    @Test fun nonPositiveCancelAmountIsRejected() {
        assertEquals("invalid-amount", assertIs<PaymentMachine.CancelResult.Rejected>(PaymentMachine.cancel(PaymentState.Approved(amount), "c0", Money(0), emptyMap())).code)
    }

    @Test fun cancelIsRejectedUnlessApproved() {
        for (s in listOf(requested, PaymentState.Unknown(amount, 0), PaymentState.Held(amount, 3), PaymentState.Declined(amount), PaymentState.FullyCancelled(amount))) {
            assertEquals("not-cancellable", assertIs<PaymentMachine.CancelResult.Rejected>(PaymentMachine.cancel(s, "cx", null, emptyMap())).code, s.name)
        }
    }

    @Test fun sameCancelKeyIsReplayedWithoutApplyingAgain() {
        val a = PaymentState.Approved(amount, Money(3000))
        val r = assertIs<PaymentMachine.CancelResult.Replayed>(PaymentMachine.cancel(a, "c1", Money(3000), mapOf("c1" to Money(3000))))
        assertEquals(Money(3000), r.amount)
        assertEquals(a, r.state)
    }

    @Test fun partialCancelStateRejectsInvalidConstruction() {
        assertFailsWith<IllegalArgumentException> { PaymentState.Approved(amount, amount) }
    }
}
