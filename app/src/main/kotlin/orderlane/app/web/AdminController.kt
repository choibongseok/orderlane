package orderlane.app.web

import orderlane.app.consumer.DeadLetterService
import orderlane.app.consumer.FaultInjector
import orderlane.app.inventory.InventoryStrategy
import orderlane.app.settlement.SettlementService
import orderlane.domain.StoreSettlement
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

data class FaultRequest(val consumer: String, val times: Int)
data class StrategyRequest(val name: String)

@RestController
@RequestMapping("/admin")
class AdminController(
    private val deadLetters: DeadLetterService,
    private val faults: FaultInjector,
    private val inventory: InventoryStrategy,
    private val settlements: SettlementService,
) {
    @GetMapping("/dead-letters") fun listDeadLetters() = deadLetters.list()

    @PostMapping("/dead-letters/replay") fun replay() = mapOf("replayed" to deadLetters.replayAll())

    @PostMapping("/faults") fun arm(@RequestBody req: FaultRequest): Map<String, Any> { faults.arm(req.consumer, req.times); return mapOf("armed" to req.consumer, "times" to req.times) }

    @DeleteMapping("/faults") fun clear(): Map<String, Any> { faults.clear(); return mapOf("cleared" to true) }

    @GetMapping("/inventory-strategy") fun strategy() = mapOf("name" to inventory.current.name)

    @PostMapping("/inventory-strategy") fun select(@RequestBody req: StrategyRequest): Map<String, Any> { inventory.select(req.name); return mapOf("name" to inventory.current.name) }

    @PostMapping("/settlements") fun settle(@RequestParam date: LocalDate) = settlements.settle(date).map(::row)

    @GetMapping("/settlements") fun settled(@RequestParam date: LocalDate) = settlements.settlements(date)

    @PostMapping("/reconciliations") fun reconcile(@RequestParam date: LocalDate) = settlements.toJson(date, settlements.reconcile(date))

    private fun row(s: StoreSettlement) = mapOf("storeId" to s.storeId.toLong(), "salesMinor" to s.sales.minor, "cancelsMinor" to s.cancels.minor, "feeMinor" to s.fee.minor, "netMinor" to s.net.minor)
}
