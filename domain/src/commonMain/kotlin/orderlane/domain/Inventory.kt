package orderlane.domain

/** 재고 한 칸. version 은 낙관적 잠금용으로 갱신마다 오른다. */
data class StockCell(val available: Int, val version: Long = 0) {
    init { require(available >= 0) { "재고는 음수가 될 수 없다" } }
}

object StockRules {
    fun canReserve(cell: StockCell, qty: Int) = qty > 0 && cell.available >= qty
    fun reserve(cell: StockCell, qty: Int): StockCell {
        require(canReserve(cell, qty)) { "재고 부족" }
        return StockCell(cell.available - qty, cell.version + 1)
    }
    fun release(cell: StockCell, qty: Int): StockCell {
        require(qty > 0) { "반환 수량은 0 보다 커야 한다" }
        return StockCell(cell.available + qty, cell.version + 1)
    }
}
