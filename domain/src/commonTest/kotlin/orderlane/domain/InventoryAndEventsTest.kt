package orderlane.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InventoryTest {
    @Test fun reserveDecrementsAndBumpsVersion() {
        assertEquals(StockCell(7, 1), StockRules.reserve(StockCell(10, 0), 3))
    }

    @Test fun cannotReserveMoreThanAvailable() {
        assertTrue(!StockRules.canReserve(StockCell(2), 3))
        assertFailsWith<IllegalArgumentException> { StockRules.reserve(StockCell(2), 3) }
    }

    @Test fun lastUnitCanBeReservedExactlyOnce() {
        val after = StockRules.reserve(StockCell(1), 1)
        assertEquals(0, after.available)
        assertTrue(!StockRules.canReserve(after, 1))
    }

    @Test fun releaseRestoresStock() {
        assertEquals(StockCell(5, 2), StockRules.release(StockRules.reserve(StockCell(5), 2), 2))
    }

    @Test fun negativeStockCannotBeConstructed() {
        assertFailsWith<IllegalArgumentException> { StockCell(-1) }
    }
}

class ConsumptionTest {
    private val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 200)

    @Test fun alreadyProcessedEventIsSkipped() {
        assertEquals(ConsumeDecision.SkipDuplicate, Consumption.onArrival(true))
        assertEquals(ConsumeDecision.Process, Consumption.onArrival(false))
    }

    @Test fun failuresRetryWithDoublingDelay() {
        assertEquals(ConsumeDecision.Retry(2, 200), Consumption.onFailure(1, policy))
        assertEquals(ConsumeDecision.Retry(3, 400), Consumption.onFailure(2, policy))
    }

    @Test fun lastAttemptFailureGoesToDeadLetter() {
        assertEquals(ConsumeDecision.DeadLetter, Consumption.onFailure(3, policy))
        assertIs<ConsumeDecision.DeadLetter>(Consumption.onFailure(9, policy))
    }

    @Test fun singleAttemptPolicyDeadLettersImmediately() {
        assertEquals(ConsumeDecision.DeadLetter, Consumption.onFailure(1, RetryPolicy(1)))
    }
}
