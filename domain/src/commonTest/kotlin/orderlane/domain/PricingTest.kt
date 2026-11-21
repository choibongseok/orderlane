package orderlane.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PricingTest {
    @Test fun lineSubtotalAddsOptionBeforeMultiplying() {
        assertEquals(Money(10_000), OrderLine(1, 2, Money(4500), Money(500)).subtotal)
    }

    @Test fun totalSumsLines() {
        val total = Pricing.total(listOf(OrderLine(1, 2, Money(4500)), OrderLine(2, 1, Money(3000), Money(300))))
        assertEquals(Money(12_300), total)
    }

    @Test fun emptyOrderIsRejected() {
        assertFailsWith<IllegalArgumentException> { Pricing.total(emptyList()) }
    }

    @Test fun quantityLimitIsEnforced() {
        assertFailsWith<IllegalArgumentException> { OrderLine(1, 0, Money(100)) }
        assertFailsWith<IllegalArgumentException> { OrderLine(1, OrderLine.MAX_QUANTITY + 1, Money(100)) }
    }

    @Test fun sameItemOnTwoLinesIsMergedForStockCheck() {
        val q = Pricing.quantityByItem(listOf(OrderLine(7, 2, Money(1)), OrderLine(3, 1, Money(1)), OrderLine(7, 3, Money(1))))
        assertEquals(mapOf(3L to 1, 7L to 5), q)
    }

    @Test fun feeRoundsHalfUpInIntegerMinorUnits() {
        assertEquals(Money(15), Pricing.fee(Money(1000), 150)) // 1000*1.5% = 15
        assertEquals(Money(2), Pricing.fee(Money(100), 150)) // 1.5 → 2
        assertEquals(Money(1), Pricing.fee(Money(99), 150)) // 1.485 → 1
    }

    @Test fun feeIsZeroWhenNetIsNotPositive() {
        assertEquals(Money.ZERO, Pricing.fee(Money(0), 300))
        assertEquals(Money.ZERO, Pricing.fee(Money(-500), 300))
    }

    @Test fun additionOverflowIsDetected() {
        assertFailsWith<IllegalArgumentException> { Money(Long.MAX_VALUE) + Money(1) }
    }
}
