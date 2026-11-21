package orderlane.domain

/** 주문 한 줄. 옵션 추가금은 단가에 더해진 뒤 수량이 곱해진다. */
data class OrderLine(
    val menuItemId: Long,
    val quantity: Int,
    val unitPrice: Money,
    val optionExtra: Money = Money.ZERO,
) {
    init {
        require(quantity in 1..MAX_QUANTITY) { "수량은 1..$MAX_QUANTITY" }
        require(unitPrice.minor >= 0 && optionExtra.minor >= 0) { "금액은 음수일 수 없음" }
    }

    val subtotal: Money get() = (unitPrice + optionExtra) * quantity

    companion object { const val MAX_QUANTITY = 20 }
}

object Pricing {
    fun total(lines: List<OrderLine>): Money {
        require(lines.isNotEmpty()) { "주문 줄이 비어 있음" }
        return lines.fold(Money.ZERO) { acc, l -> acc + l.subtotal }
    }

    /** 같은 상품이 여러 줄로 들어오면 합쳐서 재고 검사 대상 수량을 만든다. */
    fun quantityByItem(lines: List<OrderLine>): Map<Long, Int> =
        lines.groupBy { it.menuItemId }.entries.sortedBy { it.key }.associate { (k, v) -> k to v.sumOf { it.quantity } }

    /** 수수료: 순매출 × bps / 10000, 원 단위 반올림(0.5 는 올림). 순매출이 0 이하면 0. */
    fun fee(net: Money, bps: Int): Money {
        if (net.minor <= 0) return Money.ZERO
        return Money((net.minor * bps + 5_000) / 10_000)
    }
}
