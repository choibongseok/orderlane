package orderlane.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/** compose 로 올라온 전체 구성(app·gateway·postgres·redis·broker)에 바깥에서 붙는다. */
object Stack {
    const val APP = "http://127.0.0.1:58680"
    const val GATEWAY = "http://127.0.0.1:58690"
    val mapper: ObjectMapper = jacksonObjectMapper()
    val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
    val today: LocalDate get() = LocalDate.now(ZoneId.of("Asia/Seoul"))

    class Res(val status: Int, val body: String, val headers: Map<String, List<String>>) {
        val json: JsonNode by lazy { mapper.readTree(body) }
        operator fun get(k: String): JsonNode = json[k]
    }

    fun call(method: String, path: String, body: Any? = null, headers: Map<String, String> = emptyMap(), auth: Boolean = true, base: String = APP, timeoutSec: Long = 30): Res {
        val b = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(timeoutSec))
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

    fun <T> sql(block: (Connection) -> T): T = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:58632/orderlane", "orderlane", "orderlane").use(block)

    fun count(table: String, where: String = "true"): Long = sql { c -> c.createStatement().use { s -> s.executeQuery("select count(*) from $table where $where").use { it.next(); it.getLong(1) } } }
    fun scalar(query: String): String = sql { c -> c.createStatement().use { s -> s.executeQuery(query).use { it.next(); it.getString(1) } } }
    fun exec(query: String) = sql { c -> c.createStatement().use { it.executeUpdate(query) } }

    fun await(timeoutMs: Long = 30_000, what: String, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) { if (runCatching(cond).getOrDefault(false)) return; Thread.sleep(100) }
        throw AssertionError("시간 안에 충족되지 않음: $what")
    }

    fun awaitQuiet() = await(60_000, "이벤트 흐름이 잠잠해짐") {
        val published = count("outbox", "published_at is not null")
        count("outbox", "published_at is null") == 0L &&
            count("processed_event") + count("dead_letter", "replayed_at is null") == published * 3 &&
            count("payment", "state in ('UNKNOWN','HELD')") == 0L
    }

    /** 앱 컨테이너가 내려가 있으면 올리고 준비될 때까지 기다린다. */
    fun ensureAppUp() {
        if (runCatching { call("GET", "/actuator/health/readiness", auth = false, timeoutSec = 3).status == 200 }.getOrDefault(false)) return
        docker("compose", "-p", "orderlane", "start", "app")
        await(120_000, "앱 준비") { call("GET", "/actuator/health/readiness", auth = false, timeoutSec = 3).status == 200 }
    }

    fun reset() {
        ensureAppUp()
        runCatching { call("DELETE", "/admin/faults") }
        awaitQuiet()
        exec("""truncate orders, order_line, payment, payment_cancel, payment_conflict, idempotency_key, outbox, processed_event,
            kitchen_ticket, notification, ledger_entry, dead_letter, settlement, reconciliation_report, reconciliation_item restart identity cascade""")
        exec("update stock set available = case menu_item_id when 1 then 200 when 2 then 200 when 3 then 1 when 4 then 100 else 1 end, version = 0")
        call("POST", "/admin/reset", base = GATEWAY, auth = false)
        call("POST", "/admin/inventory-strategy", mapOf("name" to "optimistic"))
        ProcessBuilder("docker", "exec", "orderlane-redis-1", "redis-cli", "flushall").redirectErrorStream(true).start().waitFor()
    }

    fun docker(vararg args: String): String {
        val p = ProcessBuilder("docker", *args).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        return out
    }

    // ---- 시나리오 기록: e2e 가 실제로 확인한 내용을 evidence 로 남긴다 ----
    private val notes = ConcurrentLinkedQueue<Pair<String, String>>()
    fun note(scenario: String, what: String) { notes.add(scenario to what) }

    fun writeNotes() {
        val dir = File(System.getProperty("orderlane.evidenceDir") ?: "build/evidence").apply { mkdirs() }
        val grouped = notes.groupBy({ it.first }, { it.second }).map { (k, v) -> mapOf("scenario" to k, "observed" to v) }
        File(dir, "e2e-scenarios.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(grouped))
    }
}
