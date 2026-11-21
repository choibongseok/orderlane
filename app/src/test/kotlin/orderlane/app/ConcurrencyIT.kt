package orderlane.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/** 재고 동시성: 같은 시험을 두 가지 잠금 방식으로 여러 번 반복해 초과 판매가 없음을 보인다. 결과는 docs/evidence/concurrency.json 에 남긴다. */
class ConcurrencyIT : IntegrationBase() {
    companion object {
        const val ROUNDS = 25
        const val BUYERS = 24
    }

    private data class Round(val strategy: String, val round: Int, val stock: Int, val buyers: Int, val sold: Int, val soldOut: Int, val other: Int, val finalStock: Int, val ordersCreated: Long)

    private fun runRound(strategy: String, round: Int, stock: Int, item: Long): Round {
        jdbc.sql("update stock set available=?, version=0 where menu_item_id=?").params(stock, item).update()
        jdbc.sql("truncate orders, order_line, payment, payment_cancel, idempotency_key, outbox, processed_event, kitchen_ticket, notification, ledger_entry restart identity cascade").update()
        val pool = Executors.newFixedThreadPool(BUYERS)
        val gate = CountDownLatch(1)
        try {
            val futures = (1..BUYERS).map { i ->
                pool.submit(Callable { gate.await(); order(key = "c-$strategy-$round-$i", lines = arrayOf(Triple(item, 1, null))).status })
            }
            gate.countDown()
            val statuses = futures.map { it.get() }
            val sold = statuses.count { it == 201 }
            val soldOut = statuses.count { it == 409 }
            awaitQuiet()
            return Round(strategy, round, stock, BUYERS, sold, soldOut, statuses.size - sold - soldOut, stock(item), count("orders"))
        } finally { pool.shutdownNow() }
    }

    private fun runStrategy(name: String): List<Round> {
        call("POST", "/admin/inventory-strategy", mapOf("name" to name))
        assertThat(call("GET", "/admin/inventory-strategy").json["name"].asText()).isEqualTo(name)
        val rounds = ArrayList<Round>()
        for (r in 1..ROUNDS) {
            // 마지막 1개, 그리고 재고(5)보다 많은 동시 주문
            rounds += runRound(name, r, 1, 3)
            rounds += runRound(name, r, 5, 1)
        }
        return rounds
    }

    private fun check(rounds: List<Round>) {
        for (r in rounds) {
            assertThat(r.other).describedAs("200 번대·409 밖의 응답 $r").isEqualTo(0)
            assertThat(r.sold).describedAs("초과 판매 없음 $r").isEqualTo(r.stock)
            assertThat(r.soldOut).isEqualTo(r.buyers - r.stock)
            assertThat(r.finalStock).isEqualTo(0)
            assertThat(r.ordersCreated).isEqualTo(r.stock.toLong())
        }
    }

    @Test fun optimisticLockingNeverOversellsAcrossRepeatedRounds() {
        val rounds = runStrategy("optimistic")
        check(rounds)
        record("optimistic", rounds)
    }

    @Test fun pessimisticLockingNeverOversellsAcrossRepeatedRounds() {
        val rounds = runStrategy("pessimistic")
        check(rounds)
        record("pessimistic", rounds)
    }

    private fun record(strategy: String, rounds: List<Round>) {
        val dir = File(System.getProperty("orderlane.evidenceDir") ?: "build/evidence").apply { mkdirs() }
        val summary = mapOf(
            "strategy" to strategy, "rounds" to rounds.size, "buyersPerRound" to BUYERS,
            "cases" to rounds.groupBy { it.stock }.map { (stock, rs) -> mapOf("stock" to stock, "rounds" to rs.size, "buyers" to BUYERS, "oversold" to rs.count { it.sold > it.stock }, "soldPerRound" to rs.map { it.sold }.distinct(), "finalStock" to rs.map { it.finalStock }.distinct()) },
            "totalOversold" to rounds.count { it.sold > it.stock },
        )
        File(dir, "concurrency-$strategy.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary))
    }
}
