package orderlane.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.ZoneId

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class OrderlaneApplication

fun main(args: Array<String>) {
    runApplication<OrderlaneApplication>(*args)
}

@Configuration
class CoreConfig {
    @Bean fun clock(): Clock = Clock.systemUTC()
}

val BUSINESS_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

@ConfigurationProperties("orderlane")
data class OrderlaneProperties(
    val auth: Auth = Auth(),
    val gateway: Gateway = Gateway(),
    val inventory: Inventory = Inventory(),
    val idempotency: Idempotency = Idempotency(),
    val payment: Payment = Payment(),
    val outbox: Outbox = Outbox(),
    val consumer: Consumer = Consumer(),
    val settlement: Settlement = Settlement(),
    val faults: Faults = Faults(),
    val cache: Cache = Cache(),
) {
    data class Auth(val token: String = "demo-token", val callbackSecret: String = "demo-callback-secret")
    data class Gateway(val baseUrl: String = "http://localhost:8090", val callbackUrl: String = "http://localhost:8080/callbacks/payments", val timeoutMs: Long = 1500)
    data class Inventory(val strategy: String = "optimistic")
    data class Idempotency(val lockMs: Long = 30_000)
    data class Payment(val maxInquiries: Int = 3, val inquiryIntervalMs: Long = 1000, val heldIntervalMs: Long = 10_000, val pollMs: Long = 500)
    data class Outbox(val topic: String = "orderlane.events", val batchSize: Int = 50, val pollMs: Long = 200)
    data class Consumer(val maxAttempts: Int = 3, val baseDelayMs: Long = 200)
    data class Settlement(val feeBps: Int = 300)
    data class Faults(val enabled: Boolean = false)
    data class Cache(val ttlSeconds: Long = 60)
}
