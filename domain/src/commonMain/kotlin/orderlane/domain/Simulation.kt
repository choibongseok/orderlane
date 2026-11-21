package orderlane.domain

/** 시뮬레이션 한 단계. [rule] 은 이 단계의 판정을 낸 도메인 함수 호출이다. */
data class Step(
    val atMs: Long,
    val actor: String,
    val action: String,
    val detail: String,
    val tone: String,
    val rule: String? = null,
    val state: String? = null,
)

data class SimResult(
    val scenario: String,
    val variant: String,
    val title: String,
    val steps: List<Step>,
    val facts: List<Pair<String, String>>,
    val verdictOk: Boolean,
    val verdict: String,
) {
    fun toJson(): String = Json.obj(
        "scenario" to scenario,
        "variant" to variant,
        "title" to title,
        "steps" to steps.map {
            RawJson(Json.obj("atMs" to it.atMs, "actor" to it.actor, "action" to it.action, "detail" to it.detail,
                "tone" to it.tone, "rule" to it.rule, "state" to it.state))
        },
        "facts" to facts.map { RawJson(Json.obj("k" to it.first, "v" to it.second)) },
        "verdictOk" to verdictOk,
        "verdict" to verdict,
    )
}

private class Recorder {
    val steps = mutableListOf<Step>()
    var now = 0L
    fun at(ms: Long) { now = ms }
    fun add(actor: String, action: String, detail: String, tone: String = "info", rule: String? = null, state: String? = null) {
        steps += Step(now, actor, action, detail, tone, rule, state)
    }
    fun tick(ms: Long = 10) { now += ms }
}

/**
 * 도메인 규칙(상태 기계, 멱등 판정, 재고 규칙, 소비 정책, 정산·대사)을 실제로 호출해 시나리오를 재생한다.
 * 서버와 같은 코드가 JVM 과 브라우저(JS)에서 돈다. 네트워크·DB 는 없다.
 */
object Simulation {
    val scenarios = listOf("happy", "duplicate-request", "concurrent-order", "payment-timeout", "callbacks", "consumer-failure")

    fun variants(scenario: String): List<String> = when (scenario) {
        "duplicate-request" -> listOf("replay", "in-flight", "mismatch", "reclaim")
        "concurrent-order" -> listOf("optimistic", "pessimistic", "naive")
        "payment-timeout" -> listOf("approved-by-inquiry", "declined-not-found", "held-then-resolved")
        "consumer-failure" -> listOf("recovers", "dead-letter")
        else -> listOf("default")
    }

    fun run(scenario: String, variant: String, params: Map<String, String> = emptyMap()): SimResult = when (scenario) {
        "happy" -> happy()
        "duplicate-request" -> duplicateRequest(variant)
        "concurrent-order" -> concurrentOrder(variant, params["stock"]?.toIntOrNull() ?: 1, params["buyers"]?.toIntOrNull() ?: 5)
        "payment-timeout" -> paymentTimeout(variant)
        "callbacks" -> callbacks()
        "consumer-failure" -> consumerFailure(variant)
        else -> throw IllegalArgumentException("알 수 없는 시나리오: $scenario")
    }

    private val lines = listOf(OrderLine(menuItemId = 1, quantity = 2, unitPrice = Money(4500), optionExtra = Money(500)))

    private fun happy(): SimResult {
        val r = Recorder()
        val total = Pricing.total(lines)
        r.add("client", "POST /orders", "Idempotency-Key: k-1001, 아메리카노 2잔(옵션 +500)", "info")
        r.tick()
        r.add("api", "멱등 판정", "처음 보는 키라 이 요청이 처리한다", "ok", "Idempotency.decide(null, hash, now) = ${Idempotency.decide(null, "h", 0)}")
        r.tick()
        val stock = StockRules.reserve(StockCell(10), 2)
        r.add("inventory", "재고 예약", "재고 10 → ${stock.available}", "ok", "StockRules.reserve(StockCell(10), 2)")
        r.add("api", "금액 계산", "(4500+500)×2 = ${total.minor}원", "info", "Pricing.total(lines)")
        var pay: PaymentState = PaymentMachine.initial(total)
        r.add("payment", "승인 요청", "상태 ${pay.name}", "info", "PaymentMachine.initial($total)", pay.name)
        r.tick(120)
        r.add("gateway", "승인", "승인 응답", "ok")
        pay = (PaymentMachine.on(pay, Outcome.APPROVED, Source.REQUEST) as Transition.Applied).state
        r.add("payment", "전이", "REQUESTED → ${pay.name}", "ok", "PaymentMachine.on(REQUESTED, APPROVED, REQUEST)", pay.name)
        r.add("outbox", "같은 트랜잭션에 기록", "ORDER_PLACED, PAYMENT_APPROVED", "info")
        r.tick(60)
        r.add("relay", "브로커 발행", "아웃박스 2건 발행(적어도 한 번)", "info")
        for (c in listOf("kitchen", "notification", "ledger")) {
            r.add("consumer:$c", "소비", "처음 본 이벤트라 처리", "ok", "Consumption.onArrival(false) = ${Consumption.onArrival(false)}")
        }
        r.add("api", "응답", "201 Created, 주문 PAID", "ok")
        return SimResult("happy", "default", "주문 한 건의 정상 흐름", r.steps,
            listOf("결제 금액" to "${total.minor}원", "결제 상태" to pay.name, "재고" to "${stock.available}"), true, "주문 PAID, 재고 ${stock.available}")
    }

    private fun duplicateRequest(variant: String): SimResult {
        val r = Recorder()
        val hashA = "sha:body-A"
        var ok = true
        var verdict = ""
        val title = when (variant) {
            "replay" -> "같은 키 + 같은 본문: 같은 응답"
            "in-flight" -> "처리 중에 같은 요청이 다시 옴: 충돌 응답"
            "mismatch" -> "같은 키 + 다른 본문: 거부"
            else -> "처리하던 서버가 죽은 뒤 같은 요청: 이어받기"
        }
        r.add("client", "POST /orders", "Idempotency-Key: k-2001, 본문 A", "info")
        r.tick()
        var rec: IdemRecord? = null
        val d1 = Idempotency.decide(rec, hashA, r.now)
        r.add("api", "멱등 판정", "처음 보는 키: ${d1}", "ok", "Idempotency.decide(null, A, ${r.now})")
        rec = IdemRecord(hashA, IdemStatus.IN_PROGRESS, lockedUntilMs = r.now + 30_000)
        r.add("store", "키 선점", "IN_PROGRESS, 잠금 30초", "info")
        when (variant) {
            "replay" -> {
                r.tick(150)
                rec = IdemRecord(hashA, IdemStatus.COMPLETED, 0, 201, "{order:1001,PAID}")
                r.add("api", "처리 완료", "201, 응답을 키와 함께 저장", "ok")
                r.tick(2000)
                r.add("client", "재시도(응답을 못 받았다고 가정)", "같은 키, 같은 본문 A", "info")
                val d = Idempotency.decide(rec, hashA, r.now)
                r.add("api", "멱등 판정", "저장된 응답을 그대로 돌려준다: $d", "ok", "Idempotency.decide(COMPLETED, A, now)")
                ok = d is IdemDecision.Replay
                verdict = "주문 1건, 응답 동일(order 1001)"
            }
            "in-flight" -> {
                r.tick(40)
                r.add("client", "재요청", "같은 키, 같은 본문 A (첫 요청 처리 중)", "info")
                val d = Idempotency.decide(rec, hashA, r.now)
                r.add("api", "멱등 판정", "409 Conflict: $d", "warn", "Idempotency.decide(IN_PROGRESS, A, ${r.now})")
                ok = d is IdemDecision.InProgress
                r.tick(300)
                rec = IdemRecord(hashA, IdemStatus.COMPLETED, 0, 201, "{order:1001,PAID}")
                r.add("api", "첫 요청 완료", "201", "ok")
                val d2 = Idempotency.decide(rec, hashA, r.now)
                r.add("client", "다시 재시도", "$d2", "ok", "Idempotency.decide(COMPLETED, A, now)")
                ok = ok && d2 is IdemDecision.Replay
                verdict = "처리 중에는 409, 완료 뒤에는 같은 응답. 주문 1건"
            }
            "mismatch" -> {
                r.tick(150)
                rec = IdemRecord(hashA, IdemStatus.COMPLETED, 0, 201, "{order:1001,PAID}")
                r.add("api", "처리 완료", "201", "ok")
                r.tick(500)
                r.add("client", "같은 키, 본문 B(수량 변경)", "Idempotency-Key: k-2001", "info")
                val d = Idempotency.decide(rec, "sha:body-B", r.now)
                r.add("api", "멱등 판정", "422 거부: $d", "error", "Idempotency.decide(COMPLETED, B, now)")
                ok = d is IdemDecision.Mismatch
                verdict = "다른 본문은 거부, 주문은 1건 그대로"
            }
            else -> {
                r.add("api", "처리 중 프로세스 종료", "키는 IN_PROGRESS 로 남는다(잠금 30초)", "error")
                r.tick(31_000)
                r.add("client", "재시도", "같은 키, 같은 본문 A", "info")
                val d = Idempotency.decide(rec, hashA, r.now)
                r.add("api", "멱등 판정", "잠금이 지나 이어받는다: $d", "warn", "Idempotency.decide(IN_PROGRESS, A, ${r.now}) // lockedUntil=30000")
                r.add("api", "이어받기", "키로 이미 만든 주문을 찾아 새로 만들지 않고 마무리", "ok")
                ok = d is IdemDecision.Reclaim
                verdict = "새 주문을 만들지 않고 기존 주문을 마무리"
            }
        }
        return SimResult("duplicate-request", variant, title, r.steps, listOf("멱등 키" to "k-2001"), ok, verdict)
    }

    private data class Buyer(val id: Int, val qty: Int = 1)

    private fun concurrentOrder(variant: String, stockQty: Int, buyerCount: Int): SimResult {
        val r = Recorder()
        val buyers = (1..buyerCount).map { Buyer(it) }
        var cell = StockCell(stockQty)
        val sold = mutableListOf<Int>()
        val soldOut = mutableListOf<Int>()
        r.add("setup", "초기 재고", "재고 $stockQty, 동시에 주문하는 사람 ${buyers.size}명", "info")
        when (variant) {
            "naive" -> {
                val seen = buyers.associate { b -> r.tick(1); r.add("buyer#${b.id}", "재고 읽기", "available=${cell.available}", "info", "SELECT available"); b.id to cell }
                for (b in buyers) {
                    val c = seen.getValue(b.id)
                    r.tick(1)
                    if (StockRules.canReserve(c, b.qty)) {
                        cell = StockCell(c.available - b.qty, cell.version + 1) // 읽은 값을 기준으로 덮어쓴다
                        sold += b.id
                        r.add("buyer#${b.id}", "덮어쓰기", "읽은 ${c.available} 기준으로 ${cell.available} 기록 → 예약됨", "warn", "UPDATE stock SET available = ${cell.available}", "available=${cell.available}")
                    } else {
                        soldOut += b.id
                        r.add("buyer#${b.id}", "품절", "읽은 값이 0", "info")
                    }
                }
            }
            "optimistic" -> {
                var pending = buyers
                var round = 1
                while (pending.isNotEmpty()) {
                    val reads = pending.associate { b -> b.id to cell }
                    val retry = mutableListOf<Buyer>()
                    for (b in pending) {
                        r.tick(1)
                        val seen = reads.getValue(b.id)
                        if (!StockRules.canReserve(seen, b.qty)) {
                            soldOut += b.id
                            r.add("buyer#${b.id}", "품절", "읽은 재고 ${seen.available}, 라운드 $round", "info", "StockRules.canReserve(StockCell(${seen.available}), ${b.qty}) = false")
                            continue
                        }
                        if (cell.version == seen.version) {
                            cell = StockRules.reserve(cell, b.qty)
                            sold += b.id
                            r.add("buyer#${b.id}", "버전 일치: 예약", "v${seen.version} → v${cell.version}, 재고 ${cell.available}", "ok", "UPDATE ... WHERE version=${seen.version} AND available>=${b.qty}", "available=${cell.available}")
                        } else {
                            retry += b
                            r.add("buyer#${b.id}", "버전 불일치: 재시도", "읽은 v${seen.version}, 현재 v${cell.version}", "warn", "UPDATE ... WHERE version=${seen.version} → 0 rows")
                        }
                    }
                    pending = retry
                    round++
                }
            }
            else -> {
                for (b in buyers) {
                    r.tick(1)
                    r.add("buyer#${b.id}", "행 잠금 획득", "SELECT ... FOR UPDATE (앞 사람이 끝날 때까지 대기)", "info")
                    if (StockRules.canReserve(cell, b.qty)) {
                        cell = StockRules.reserve(cell, b.qty)
                        sold += b.id
                        r.add("buyer#${b.id}", "예약", "재고 ${cell.available}", "ok", "StockRules.reserve", "available=${cell.available}")
                    } else {
                        soldOut += b.id
                        r.add("buyer#${b.id}", "품절", "재고 ${cell.available}", "info", "StockRules.canReserve = false")
                    }
                    r.add("buyer#${b.id}", "잠금 해제", "커밋", "info")
                }
            }
        }
        val oversold = (sold.size - stockQty).coerceAtLeast(0)
        val ok = oversold == 0
        val verdict = if (ok) "초과 판매 없음: ${sold.size}건 판매, ${soldOut.size}건 품절" else "초과 판매 ${oversold}건: 재고 ${stockQty}개에 ${sold.size}건 판매"
        val title = when (variant) {
            "naive" -> "잠금 없이 읽고 덮어쓰기(비교용)"
            "optimistic" -> "낙관적 잠금: 버전 비교 후 재시도"
            else -> "비관적 잠금: 행 잠금 후 처리"
        }
        r.add("result", "정리", verdict, if (ok) "ok" else "error")
        return SimResult("concurrent-order", variant, title, r.steps,
            listOf("초기 재고" to "$stockQty", "동시 주문" to "${buyers.size}", "판매" to "${sold.size}", "품절 응답" to "${soldOut.size}", "초과 판매" to "$oversold"),
            ok, verdict)
    }

    private fun paymentTimeout(variant: String): SimResult {
        val r = Recorder()
        val policy = PaymentPolicy(maxInquiries = 3)
        var s: PaymentState = PaymentMachine.initial(Money(10_000))
        r.add("api", "승인 요청", "10000원, 응답 대기 1.5초", "info", "PaymentMachine.initial(10000)", s.name)
        r.tick(1500)
        fun step(o: Outcome, src: Source, note: String, tone: String = "info") {
            val before = s.name
            val t = PaymentMachine.on(s, o, src, policy)
            s = t.state
            val kind = when (t) { is Transition.Applied -> "전이"; is Transition.Ignored -> "무시(${t.reason})"; is Transition.Rejected -> "거부(${t.code})" }
            r.add("payment", kind, "$note: $before → ${s.name}", tone, "PaymentMachine.on($before, $o, $src)", s.name)
        }
        r.add("gateway", "응답 없음", "시간 초과: 승인됐는지 모른다", "warn")
        step(Outcome.TIMEOUT, Source.REQUEST, "결과 불명", "warn")
        var stockReleased = false
        when (variant) {
            "approved-by-inquiry" -> {
                r.tick(1000); r.add("job", "재조회 #1", "게이트웨이: 처리 중", "info"); step(Outcome.PENDING, Source.INQUIRY, "아직 처리 중")
                r.tick(1000); r.add("job", "재조회 #2", "게이트웨이: 승인", "ok"); step(Outcome.APPROVED, Source.INQUIRY, "조회로 확정", "ok")
            }
            "declined-not-found" -> {
                r.tick(1000); r.add("job", "재조회 #1", "게이트웨이: 해당 거래 없음(요청이 도착하지 않았다)", "info")
                step(Outcome.NOT_FOUND, Source.INQUIRY, "없음 → 승인된 적 없다고 확정", "warn")
                stockReleased = true
                r.add("inventory", "재고 반환", "예약했던 수량을 돌려놓는다", "ok", "StockRules.release")
            }
            else -> {
                for (i in 1..3) { r.tick(1000); r.add("job", "재조회 #$i", "게이트웨이: 처리 중", "info"); step(Outcome.PENDING, Source.INQUIRY, "아직 처리 중") }
                r.add("payment", "보류", "횟수를 넘겨 HELD 로 두고 주문은 결제 대기로 응답", "warn", state = s.name)
                r.tick(10_000)
                r.add("job", "보류 재조회", "게이트웨이: 승인", "ok"); step(Outcome.APPROVED, Source.INQUIRY, "보류 건 확정", "ok")
            }
        }
        val ok = s.resolved
        val verdict = "결제 ${s.name}" + if (stockReleased) ", 재고 반환" else ""
        r.add("order", "주문 상태", if (s is PaymentState.Approved) "PAID" else "PAYMENT_FAILED", if (ok) "ok" else "error")
        return SimResult("payment-timeout", variant, "결제 승인 응답이 시간 초과: 결과 불명을 조회로 확정", r.steps, listOf("최종" to s.name), ok, verdict)
    }

    private fun callbacks(): SimResult {
        val r = Recorder()
        var s: PaymentState = PaymentMachine.initial(Money(10_000))
        s = PaymentMachine.on(s, Outcome.TIMEOUT, Source.REQUEST).state
        r.add("payment", "시간 초과", "결과 불명으로 시작: ${s.name}", "warn", "PaymentMachine.on(REQUESTED, TIMEOUT, REQUEST)", s.name)
        var applied = 0
        var ignored = 0
        var rejected = 0
        fun cb(seq: Int, o: Outcome, note: String) {
            r.tick(30)
            val before = s.name
            val t = PaymentMachine.on(s, o, Source.CALLBACK)
            s = t.state
            when (t) {
                is Transition.Applied -> { applied++; r.add("callback", "콜백 #$seq $o", "$note: $before → ${s.name}", "ok", "PaymentMachine.on($before, $o, CALLBACK)", s.name) }
                is Transition.Ignored -> { ignored++; r.add("callback", "콜백 #$seq $o", "$note: 무시(${t.reason})", "info", "PaymentMachine.on($before, $o, CALLBACK)", s.name) }
                is Transition.Rejected -> { rejected++; r.add("callback", "콜백 #$seq $o", "$note: 거부(${t.code}) — 불일치로 기록", "error", "PaymentMachine.on($before, $o, CALLBACK)", s.name) }
            }
        }
        cb(2, Outcome.APPROVED, "순서가 뒤바뀌어 승인이 먼저 도착")
        cb(2, Outcome.APPROVED, "같은 콜백 재전송")
        cb(1, Outcome.PENDING, "처리 중 통지가 뒤늦게 도착")
        cb(3, Outcome.DECLINED, "이미 승인된 결제에 거절 통지")
        val ok = s is PaymentState.Approved && applied == 1 && rejected == 1
        return SimResult("callbacks", "default", "중복·순서 뒤바뀐 콜백", r.steps,
            listOf("반영" to "$applied", "무시" to "$ignored", "거부" to "$rejected", "최종" to s.name), ok,
            "결제 ${s.name}: 반영 $applied, 무시 $ignored, 거부(불일치 기록) $rejected")
    }

    private fun consumerFailure(variant: String): SimResult {
        val r = Recorder()
        val policy = RetryPolicy(maxAttempts = 3, baseDelayMs = 200)
        val failures = if (variant == "recovers") 2 else 99
        r.add("relay", "발행", "PAYMENT_APPROVED(event e-77)를 브로커로", "info")
        for (c in listOf("kitchen", "ledger")) {
            r.tick(5); r.add("consumer:$c", "소비", "처리 완료", "ok", "Consumption.onArrival(false) = ${Consumption.onArrival(false)}")
        }
        var attempt = 1
        var deadLettered = false
        var processed = false
        while (true) {
            r.tick(20)
            if (attempt > failures) {
                processed = true
                r.add("consumer:notification", "시도 #$attempt", "처리 완료", "ok")
                break
            }
            r.add("consumer:notification", "시도 #$attempt", "알림 전송 실패", "error")
            when (val d = Consumption.onFailure(attempt, policy)) {
                is ConsumeDecision.Retry -> { r.add("consumer:notification", "재시도 예약", "${d.delayMs}ms 뒤 #${d.nextAttempt}", "warn", "Consumption.onFailure($attempt, ${policy.maxAttempts}회) = Retry"); r.tick(d.delayMs); attempt = d.nextAttempt }
                else -> { deadLettered = true; r.add("consumer:notification", "dead-letter", "$attempt 회 실패, 이벤트를 보관하고 소비를 이어간다", "error", "Consumption.onFailure($attempt, ${policy.maxAttempts}회) = $d"); break }
            }
        }
        var replayOk = true
        if (deadLettered) {
            r.tick(5000)
            r.add("ops", "dead-letter 재처리 명령", "원인을 고친 뒤 event e-77 을 다시 발행", "info")
            for (c in listOf("kitchen", "ledger")) {
                val d = Consumption.onArrival(true)
                replayOk = replayOk && d is ConsumeDecision.SkipDuplicate
                r.add("consumer:$c", "재전달 수신", "이미 처리한 이벤트 ID 라 건너뜀: $d", "info", "Consumption.onArrival(true)")
            }
            r.add("consumer:notification", "재전달 수신", "처음 처리 → 완료", "ok", "Consumption.onArrival(false)")
            processed = true
        }
        val ok = processed && replayOk
        return SimResult("consumer-failure", variant,
            if (variant == "recovers") "소비자가 일시 실패 후 재시도로 회복" else "소비자 실패가 계속되어 dead-letter 로 간 뒤 재처리",
            r.steps, listOf("재시도 정책" to "최대 ${policy.maxAttempts}회, 200ms 부터 2배", "dead-letter" to if (deadLettered) "보관 후 재처리" else "없음"),
            ok, if (deadLettered) "dead-letter 보관 → 재처리로 알림 1건 처리, 다른 소비자는 중복 건너뜀" else "재시도 ${attempt}번째에 처리, 알림 1건")
    }
}
