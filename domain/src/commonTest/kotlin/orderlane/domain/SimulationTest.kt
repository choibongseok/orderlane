package orderlane.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulationTest {
    @Test fun happyPathEndsPaid() {
        val r = Simulation.run("happy", "default")
        assertTrue(r.verdictOk)
        assertTrue(r.steps.any { it.state == "APPROVED" })
    }

    @Test fun duplicateRequestVariantsAllHoldTheirInvariant() {
        for (v in Simulation.variants("duplicate-request")) assertTrue(Simulation.run("duplicate-request", v).verdictOk, v)
    }

    @Test fun lockedStrategiesNeverOversell() {
        for (v in listOf("optimistic", "pessimistic")) {
            val r = Simulation.run("concurrent-order", v, mapOf("stock" to "3", "buyers" to "12"))
            assertTrue(r.verdictOk, v)
            assertEquals("3", r.facts.first { it.first == "판매" }.second)
        }
    }

    @Test fun unlockedReadThenWriteOversells() {
        val r = Simulation.run("concurrent-order", "naive", mapOf("stock" to "1", "buyers" to "5"))
        assertFalse(r.verdictOk)
        assertEquals("4", r.facts.first { it.first == "초과 판매" }.second)
    }

    @Test fun paymentTimeoutVariantsAllResolve() {
        for (v in Simulation.variants("payment-timeout")) assertTrue(Simulation.run("payment-timeout", v).verdictOk, v)
    }

    @Test fun callbackScenarioAppliesOnceIgnoresDuplicatesAndRejectsConflict() {
        val r = Simulation.run("callbacks", "default")
        assertTrue(r.verdictOk)
        assertEquals("1", r.facts.first { it.first == "반영" }.second)
        assertEquals("1", r.facts.first { it.first == "거부" }.second)
    }

    @Test fun consumerFailureRecoversOrDeadLettersThenReplaysOnce() {
        assertTrue(Simulation.run("consumer-failure", "recovers").verdictOk)
        val dl = Simulation.run("consumer-failure", "dead-letter")
        assertTrue(dl.verdictOk)
        assertTrue(dl.steps.any { it.action == "dead-letter" })
    }

    @Test fun resultSerializesToJsonWithEscapes() {
        val j = Simulation.run("happy", "default").toJson()
        assertTrue(j.startsWith("{\"scenario\":\"happy\""))
        assertTrue(j.contains("\"verdictOk\":true"))
        assertEquals("\"a\\\"b\\n\"", Json.str("a\"b\n"))
    }
}
