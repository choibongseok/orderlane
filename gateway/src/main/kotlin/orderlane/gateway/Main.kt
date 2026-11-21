package orderlane.gateway

import java.util.concurrent.CountDownLatch

fun main() {
    val env = System.getenv()
    val config = GatewayConfig(
        port = env["GATEWAY_PORT"]?.toInt() ?: 8090,
        lateResponseMs = env["GATEWAY_LATE_RESPONSE_MS"]?.toLong() ?: 3_000,
        holdMs = env["GATEWAY_HOLD_MS"]?.toLong() ?: 8_000,
    )
    val server = GatewayServer(config).start()
    println("gateway listening on ${server.port}")
    val done = CountDownLatch(1)
    Runtime.getRuntime().addShutdownHook(Thread { server.stop(); done.countDown() })
    done.await()
}
