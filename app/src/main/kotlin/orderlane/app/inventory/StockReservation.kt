package orderlane.app.inventory

import orderlane.app.OrderlaneProperties
import orderlane.domain.StockCell
import orderlane.domain.StockRules
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

/** 재고 예약 방식. 호출하는 쪽은 어느 방식인지 모른다. 반드시 호출자의 트랜잭션 안에서 실행한다. */
interface StockReservation {
    val name: String
    /** 재고가 충분해 예약했으면 true, 부족하면 false. */
    fun reserve(itemId: Long, qty: Int): Boolean
    fun release(itemId: Long, qty: Int)
}

class StockContentionException(itemId: Long) : RuntimeException("재고 경합이 해소되지 않음 item=$itemId")

/** 낙관적: 재고와 버전을 읽고, 버전이 그대로일 때만 갱신한다. 다른 요청이 먼저 바꿨으면 다시 읽어 재시도. */
class OptimisticStockReservation(private val jdbc: JdbcClient, private val maxRetries: Int = 60) : StockReservation {
    override val name = "optimistic"

    override fun reserve(itemId: Long, qty: Int): Boolean {
        repeat(maxRetries) { attempt ->
            val cell = read(itemId)
            if (!StockRules.canReserve(cell, qty)) return false
            val next = StockRules.reserve(cell, qty)
            val updated = jdbc.sql("update stock set available=?, version=? where menu_item_id=? and version=? and available>=?")
                .params(next.available, next.version, itemId, cell.version, qty).update()
            if (updated == 1) return true
            Thread.sleep(ThreadLocalRandom.current().nextLong(1, 3L + attempt))
        }
        throw StockContentionException(itemId)
    }

    override fun release(itemId: Long, qty: Int) {
        jdbc.sql("update stock set available = available + ?, version = version + 1 where menu_item_id=?").params(qty, itemId).update()
    }

    private fun read(itemId: Long): StockCell =
        jdbc.sql("select available, version from stock where menu_item_id=?").params(itemId)
            .query { rs, _ -> StockCell(rs.getInt(1), rs.getLong(2)) }.single()
}

/** 비관적: 행을 잠그고 읽은 뒤 갱신한다. 같은 상품의 요청은 줄을 선다. */
class PessimisticStockReservation(private val jdbc: JdbcClient) : StockReservation {
    override val name = "pessimistic"

    override fun reserve(itemId: Long, qty: Int): Boolean {
        val cell = jdbc.sql("select available, version from stock where menu_item_id=? for update").params(itemId)
            .query { rs, _ -> StockCell(rs.getInt(1), rs.getLong(2)) }.single()
        if (!StockRules.canReserve(cell, qty)) return false
        val next = StockRules.reserve(cell, qty)
        jdbc.sql("update stock set available=?, version=? where menu_item_id=?").params(next.available, next.version, itemId).update()
        return true
    }

    override fun release(itemId: Long, qty: Int) {
        jdbc.sql("update stock set available = available + ?, version = version + 1 where menu_item_id=?").params(qty, itemId).update()
    }
}

/** 설정으로 고르는 방식. 운영 중 바꿀 수 있어 같은 시나리오를 두 방식으로 돌려 비교할 수 있다. */
@Component
class InventoryStrategy(jdbc: JdbcClient, props: OrderlaneProperties) {
    private val all = listOf(OptimisticStockReservation(jdbc), PessimisticStockReservation(jdbc)).associateBy { it.name }

    @Volatile private var selected: StockReservation = all[props.inventory.strategy] ?: error("알 수 없는 재고 방식: ${props.inventory.strategy}")

    val current: StockReservation get() = selected

    fun select(name: String) { selected = all[name] ?: throw IllegalArgumentException("알 수 없는 재고 방식: $name") }
}
