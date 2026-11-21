package orderlane.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IdempotencyTest {
    private val inProgress = IdemRecord("h1", IdemStatus.IN_PROGRESS, lockedUntilMs = 1_000)
    private val completed = IdemRecord("h1", IdemStatus.COMPLETED, 0, 201, "{\"id\":1}")

    @Test fun unseenKeyProceeds() = assertEquals(IdemDecision.Proceed, Idempotency.decide(null, "h1", 0))

    @Test fun completedSameBodyReplaysStoredResponse() {
        val d = assertIs<IdemDecision.Replay>(Idempotency.decide(completed, "h1", 5))
        assertEquals(201, d.status)
        assertEquals("{\"id\":1}", d.body)
    }

    @Test fun differentBodyIsMismatchRegardlessOfStatus() {
        assertEquals(IdemDecision.Mismatch, Idempotency.decide(completed, "h2", 5))
        assertEquals(IdemDecision.Mismatch, Idempotency.decide(inProgress, "h2", 5))
    }

    @Test fun inProgressWithinLockIsConflict() = assertEquals(IdemDecision.InProgress, Idempotency.decide(inProgress, "h1", 999))

    @Test fun inProgressAfterLockExpiryIsReclaimed() {
        assertEquals(IdemDecision.Reclaim, Idempotency.decide(inProgress, "h1", 1_000))
        assertEquals(IdemDecision.Reclaim, Idempotency.decide(inProgress, "h1", 5_000))
    }
}
