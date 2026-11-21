package orderlane.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementTest {
    private fun sale(id: String, store: String, ref: String, amt: Long) = LedgerEntry(id, store, EntryKind.SALE, ref, Money(amt))
    private fun cancel(id: String, store: String, ref: String, amt: Long) = LedgerEntry(id, store, EntryKind.CANCEL, ref, Money(amt))

    @Test fun settlesPerStoreWithSalesCancelsAndFee() {
        val r = Settlement.settle(listOf(
            sale("e1", "s-b", "p1", 10_000), sale("e2", "s-a", "p2", 20_000), cancel("e3", "s-a", "c1", 5_000),
        ), feeBps = 300)
        assertEquals(listOf("s-a", "s-b"), r.map { it.storeId })
        val a = r[0]
        assertEquals(Money(20_000), a.sales)
        assertEquals(Money(5_000), a.cancels)
        assertEquals(Money(450), a.fee) // (20000-5000)*3%
        assertEquals(Money(14_550), a.net)
        assertEquals(Money(300), r[1].fee)
    }

    @Test fun sameEventIdIsCountedOnce() {
        val once = Settlement.settle(listOf(sale("e1", "s", "p", 1000)), 300)
        val twice = Settlement.settle(listOf(sale("e1", "s", "p", 1000), sale("e1", "s", "p", 1000)), 300)
        assertEquals(once, twice)
    }

    @Test fun resultDoesNotDependOnInputOrder() {
        val entries = listOf(sale("e1", "s", "p1", 1234), sale("e2", "s", "p2", 777), cancel("e3", "s", "c", 100), sale("e4", "t", "p4", 50))
        assertEquals(Settlement.settle(entries, 250), Settlement.settle(entries.reversed(), 250))
    }

    @Test fun fullyCancelledSaleHasNoFee() {
        val r = Settlement.settle(listOf(sale("e1", "s", "p", 1000), cancel("e2", "s", "c", 1000)), 300).single()
        assertEquals(Money.ZERO, r.fee)
        assertEquals(Money.ZERO, r.net)
    }

    @Test fun emptyInputHasNoStores() = assertTrue(Settlement.settle(emptyList(), 300).isEmpty())
}

class ReconciliationTest {
    private fun ls(id: String, ref: String, amt: Long) = LedgerEntry("e-$id", "s", EntryKind.SALE, ref, Money(amt))
    private fun ps(id: String, ref: String, amt: Long) = ProviderTxn("t-$id", EntryKind.SALE, ref, Money(amt))

    @Test fun matchingFilesHaveNoMismatch() {
        val r = Reconciliation.reconcile(listOf(ls("1", "p1", 100), ls("2", "p2", 200)), listOf(ps("1", "p1", 100), ps("2", "p2", 200)))
        assertTrue(r.clean)
        assertEquals(2, r.matched)
    }

    @Test fun transactionMissingInLedgerIsReported() {
        val r = Reconciliation.reconcile(listOf(ls("1", "p1", 100)), listOf(ps("1", "p1", 100), ps("2", "p2", 200)))
        assertEquals(listOf(Mismatch(MismatchType.MISSING_IN_LEDGER, EntryKind.SALE, "p2", null, Money(200))), r.mismatches)
        assertEquals(1, r.matched)
    }

    @Test fun transactionMissingAtProviderIsReported() {
        val r = Reconciliation.reconcile(listOf(ls("1", "p1", 100), ls("2", "p2", 200)), listOf(ps("1", "p1", 100)))
        assertEquals(MismatchType.MISSING_AT_PROVIDER, r.mismatches.single().type)
        assertEquals("p2", r.mismatches.single().ref)
    }

    @Test fun amountDifferenceIsReportedWithBothAmounts() {
        val m = Reconciliation.reconcile(listOf(ls("1", "p1", 100)), listOf(ps("1", "p1", 90))).mismatches.single()
        assertEquals(MismatchType.AMOUNT_DIFFERENCE, m.type)
        assertEquals(Money(100), m.ledgerAmount)
        assertEquals(Money(90), m.providerAmount)
    }

    @Test fun duplicateRowAtProviderIsReported() {
        val m = Reconciliation.reconcile(listOf(ls("1", "p1", 100)), listOf(ps("1", "p1", 100), ps("1b", "p1", 100))).mismatches.single()
        assertEquals(MismatchType.DUPLICATE_AT_PROVIDER, m.type)
        assertEquals(Money(200), m.providerAmount)
    }

    @Test fun duplicateLedgerEntryIsReported() {
        val m = Reconciliation.reconcile(listOf(ls("1", "p1", 100), ls("1b", "p1", 100)), listOf(ps("1", "p1", 100))).mismatches.single()
        assertEquals(MismatchType.DUPLICATE_IN_LEDGER, m.type)
    }

    @Test fun cancelAndSaleWithSameRefAreDifferentKeys() {
        val ledger = listOf(ls("1", "x", 100), LedgerEntry("e-2", "s", EntryKind.CANCEL, "x", Money(40)))
        val provider = listOf(ps("1", "x", 100), ProviderTxn("t-2", EntryKind.CANCEL, "x", Money(40)))
        assertTrue(Reconciliation.reconcile(ledger, provider).clean)
    }

    @Test fun reportIsStableAcrossReruns() {
        val l = listOf(ls("1", "p1", 100), ls("2", "p2", 200), ls("3", "p3", 300))
        val p = listOf(ps("3", "p3", 301), ps("1", "p1", 100), ps("9", "p9", 5))
        assertEquals(Reconciliation.reconcile(l, p), Reconciliation.reconcile(l.reversed(), p.reversed()))
        assertEquals(3, Reconciliation.reconcile(l, p).mismatches.size)
    }
}
