package orderlane.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import orderlane.gateway.GatewayConfig
import orderlane.gateway.GatewayServer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.postgresql.PostgreSQLContainer
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.wait.strategy.Wait
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

// Infra 의 초기화 중 다른 스레드(컨테이너 생성 콜백)가 Infra 필드를 읽으면 교착하므로 포트는 파일 수준 값으로 둔다
private val KAFKA_PORT: Int = ServerSocket(0).use { it.localPort }

// 브로커가 알리는 주소와 호스트 포트를 같은 값으로 고정해 클라이언트가 항상 같은 주소로 붙게 한다(compose 구성과 같은 설정).
// 콜백이 Infra 의 정적 메서드가 되면 Infra 초기화 중에 교착하므로 파일 수준 함수로 만든다.
private fun newBroker(): GenericContainer<*> = GenericContainer<Nothing>("redpandadata/redpanda:v24.2.7").apply {
    withCommand("redpanda", "start", "--smp=1", "--memory=512M", "--overprovisioned", "--node-id=0", "--check=false",
        "--kafka-addr=0.0.0.0:$KAFKA_PORT", "--advertise-kafka-addr=127.0.0.1:$KAFKA_PORT")
    withExposedPorts(KAFKA_PORT)
    withCreateContainerCmdModifier { it.hostConfig!!.withPortBindings(PortBinding(Ports.Binding.bindPort(KAFKA_PORT), ExposedPort(KAFKA_PORT))) }
    waitingFor(Wait.forListeningPort())
}

/** 실제 PostgreSQL·Redis·Redpanda(Kafka 호환)와 모의 결제 게이트웨이를 JVM 하나에서 한 번만 띄운다. */
object Infra {
    val pg = PostgreSQLContainer("postgres:16-alpine").withDatabaseName("orderlane").withUsername("orderlane").withPassword("orderlane")
    val redis = GenericContainer("redis:7-alpine").withExposedPorts(6379)
    val kafkaPort: Int = KAFKA_PORT
    val broker = newBroker()
    val gateway: GatewayServer
    val appPort: Int = ServerSocket(0).use { it.localPort }

    init {
        Startables.deepStart(pg, redis, broker).join()
        gateway = GatewayServer(GatewayConfig(port = 0, lateResponseMs = 1500, holdMs = 3000, callbackDelayMs = 50)).start()
        Runtime.getRuntime().addShutdownHook(Thread { gateway.stop() })
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
abstract class IntegrationBase {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(r: DynamicPropertyRegistry) {
            r.add("spring.datasource.url") { Infra.pg.jdbcUrl }
            r.add("spring.datasource.username") { "orderlane" }
            r.add("spring.datasource.password") { "orderlane" }
            r.add("spring.data.redis.host") { Infra.redis.host }
            r.add("spring.data.redis.port") { Infra.redis.getMappedPort(6379) }
            r.add("spring.kafka.bootstrap-servers") { "127.0.0.1:${Infra.kafkaPort}" }
            r.add("server.port") { Infra.appPort }
            r.add("orderlane.gateway.base-url") { "http://127.0.0.1:${Infra.gateway.port}" }
            r.add("orderlane.gateway.callback-url") { "http://127.0.0.1:${Infra.appPort}/callbacks/payments" }
            r.add("orderlane.gateway.timeout-ms") { 600 }
            r.add("orderlane.payment.inquiry-interval-ms") { 300 }
            r.add("orderlane.payment.held-interval-ms") { 500 }
            r.add("orderlane.payment.poll-ms") { 150 }
            r.add("orderlane.outbox.poll-ms") { 100 }
            r.add("orderlane.consumer.base-delay-ms") { 50 }
            r.add("orderlane.faults.enabled") { true }
            r.add("orderlane.idempotency.lock-ms") { 30000 }
        }
    }

    @Autowired lateinit var jdbc: JdbcClient
    @Autowired lateinit var redis: StringRedisTemplate

    val mapper: ObjectMapper = jacksonObjectMapper()
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
    val base get() = "http://127.0.0.1:${Infra.appPort}"
    val today: LocalDate get() = LocalDate.now(BUSINESS_ZONE)

    class Res(val status: Int, val body: String, val headers: Map<String, List<String>>) {
        val json: JsonNode by lazy { jacksonObjectMapper().readTree(body) }
        operator fun get(k: String): JsonNode = json[k]
    }

    fun call(method: String, path: String, body: Any? = null, headers: Map<String, String> = emptyMap(), auth: Boolean = true): Res {
        val b = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20))
        if (auth) b.header("Authorization", "Bearer demo-token")
        headers.forEach { (k, v) -> b.header(k, v) }
        if (body != null) b.header("Content-Type", "application/json")
        b.method(method, if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(if (body is String) body else mapper.writeValueAsString(body)))
        val r = http.send(b.build(), HttpResponse.BodyHandlers.ofString())
        return Res(r.statusCode(), r.body(), r.headers().map())
    }

    fun order(token: String = "tok_approve", key: String = UUID.randomUUID().toString(), store: Long = 1, vararg lines: Triple<Long, Int, Long?> = arrayOf(Triple(1L, 1, null))): Res =
        call("POST", "/orders", mapOf("storeId" to store, "lines" to lines.map { mapOf("menuItemId" to it.first, "quantity" to it.second, "optionId" to it.third) }, "paymentToken" to token),
            mapOf("Idempotency-Key" to key))

    fun stock(item: Long): Int = jdbc.sql("select available from stock where menu_item_id=?").params(item).query(Int::class.java).single()
    fun count(table: String, where: String = "true"): Long = jdbc.sql("select count(*) from $table where $where").query(Long::class.java).single()

    fun await(timeoutMs: Long = 15_000, what: String = "조건", cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) { if (runCatching(cond).getOrDefault(false)) return; Thread.sleep(50) }
        throw AssertionError("시간 안에 충족되지 않음: $what")
    }

    /** 발행되지 않은 아웃박스가 없고, 모든 소비자가 모든 이벤트를 처리하거나 dead-letter 로 보관할 때까지 기다린다. */
    fun awaitQuiet() = await(40_000, "이벤트 흐름이 잠잠해짐") {
        val published = count("outbox", "published_at is not null")
        count("outbox", "published_at is null") == 0L &&
            count("processed_event") + count("dead_letter", "replayed_at is null") == published * 3 &&
            count("payment", "state in ('UNKNOWN','HELD')") == 0L
    }

    @BeforeEach
    fun resetState() {
        runCatching { call("DELETE", "/admin/faults") }
        awaitQuiet()
        jdbc.sql("""truncate orders, order_line, payment, payment_cancel, payment_conflict, idempotency_key, outbox, processed_event,
            kitchen_ticket, notification, ledger_entry, dead_letter, settlement, reconciliation_report, reconciliation_item restart identity cascade""").update()
        jdbc.sql("update stock set available = case menu_item_id when 1 then 200 when 2 then 200 when 3 then 1 when 4 then 100 else 1 end, version = 0").update()
        redis.connectionFactory!!.connection.serverCommands().flushAll()
        val http = HttpClient.newHttpClient()
        http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:${Infra.gateway.port}/admin/reset")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding())
        call("POST", "/admin/inventory-strategy", mapOf("name" to "optimistic"))
    }

    fun gatewayPost(path: String, body: String = "{}") {
        HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:${Infra.gateway.port}$path"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding())
    }
}
