package orderlane.domain

enum class EntryKind { SALE, CANCEL }

/** 내부 원장 한 줄. [ref] 는 매출이면 결제 참조, 취소면 취소 키. */
data class LedgerEntry(val eventId: String, val storeId: String, val kind: EntryKind, val ref: String, val amount: Money)

data class StoreSettlement(val storeId: String, val sales: Money, val cancels: Money, val fee: Money) {
    val net: Money get() = sales - cancels - fee
}

object Settlement {
    /** 같은 이벤트 ID 는 한 번만 센다. 입력 순서와 상관없이 같은 결과가 나온다. */
    fun settle(entries: List<LedgerEntry>, feeBps: Int): List<StoreSettlement> =
        entries.distinctBy { it.eventId }.groupBy { it.storeId }.entries.sortedBy { it.key }.map { (store, list) ->
            val sales = list.filter { it.kind == EntryKind.SALE }.fold(Money.ZERO) { a, e -> a + e.amount }
            val cancels = list.filter { it.kind == EntryKind.CANCEL }.fold(Money.ZERO) { a, e -> a + e.amount }
            StoreSettlement(store, sales, cancels, Pricing.fee(sales - cancels, feeBps))
        }
}

/** 결제사 거래 내역 한 줄. */
data class ProviderTxn(val txId: String, val kind: EntryKind, val ref: String, val amount: Money)

enum class MismatchType { MISSING_IN_LEDGER, MISSING_AT_PROVIDER, AMOUNT_DIFFERENCE, DUPLICATE_AT_PROVIDER, DUPLICATE_IN_LEDGER }

data class Mismatch(
    val type: MismatchType, val kind: EntryKind, val ref: String,
    val ledgerAmount: Money?, val providerAmount: Money?,
)

data class ReconciliationReport(val matched: Int, val mismatches: List<Mismatch>) {
    val clean: Boolean get() = mismatches.isEmpty()
}

object Reconciliation {
    fun reconcile(ledger: List<LedgerEntry>, provider: List<ProviderTxn>): ReconciliationReport {
        val ledgerBy = ledger.groupBy { it.kind to it.ref }
        val providerBy = provider.groupBy { it.kind to it.ref }
        var matched = 0
        val out = mutableListOf<Mismatch>()
        val keys = (ledgerBy.keys + providerBy.keys).sortedWith(compareBy({ it.first.name }, { it.second }))
        for (k in keys) {
            val l = ledgerBy[k].orEmpty()
            val p = providerBy[k].orEmpty()
            val (kind, ref) = k
            when {
                l.isEmpty() -> out += Mismatch(MismatchType.MISSING_IN_LEDGER, kind, ref, null, p.first().amount)
                p.isEmpty() -> out += Mismatch(MismatchType.MISSING_AT_PROVIDER, kind, ref, l.first().amount, null)
                else -> {
                    if (l.size > 1) out += Mismatch(MismatchType.DUPLICATE_IN_LEDGER, kind, ref, l.fold(Money.ZERO) { a, e -> a + e.amount }, p.first().amount)
                    else if (p.size > 1) out += Mismatch(MismatchType.DUPLICATE_AT_PROVIDER, kind, ref, l.first().amount, p.fold(Money.ZERO) { a, e -> a + e.amount })
                    else if (l.first().amount != p.first().amount) out += Mismatch(MismatchType.AMOUNT_DIFFERENCE, kind, ref, l.first().amount, p.first().amount)
                    else matched++
                }
            }
        }
        return ReconciliationReport(matched, out)
    }
}
