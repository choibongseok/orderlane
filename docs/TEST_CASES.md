# 테스트 케이스

모든 행은 실제 테스트 하나와 1:1 로 대응합니다. 자동화 테스트 열은 `파일::테스트 이름`(Kotlin 은 테스트 함수 이름, 브라우저 테스트는 Playwright `test` 제목)입니다. 행 수와 실제 테스트 수는 `make verify` 가 `scripts/check-test-cases.mjs` 로 대조합니다(통과하지 않은 테스트, 문서에 없는 테스트, 두 행에 걸친 테스트가 있으면 실패). 도메인 테스트는 같은 이름으로 JVM 과 JavaScript(Node) 양쪽에서 실행되고, 둘의 결과가 같아야 통과합니다.

통합 테스트는 Testcontainers 로 띄운 실제 PostgreSQL·Redis·Redpanda(Kafka 호환)와 모의 결제 게이트웨이를 쓰고, e2e 는 `compose.yml` 의 전체 구성을 바깥에서 두드립니다. `make verify` 가 올리고 끝나면 내립니다.

| 종류 | 개수 | 실행 |
|---|---|---|
| unit (도메인 규칙·게이트웨이 모의·어댑터) | 79 | `make test-unit` |
| integration (실제 PG·Redis·브로커로 주문·결제·이벤트·정산·동시성) | 29 | `make test-int` |
| e2e (compose 전체 구성 / 데모 사이트 브라우저) | 32 | `make e2e`, `make verify` |
| **합계** | **140** | `make verify` |

| ID | 영역 | 시나리오 | 입력/조건 | 기대 결과 | 자동화 테스트 | 종류 |
|---|---|---|---|---|---|---|
| PRC-001 | 금액·가격 | 옵션 추가금은 단가에 더한 뒤 수량을 곱한다 | 단가 4500, 옵션 500, 수량 2 | 소계 10000 | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::lineSubtotalAddsOptionBeforeMultiplying` | unit |
| PRC-002 | 금액·가격 | 여러 줄의 합계 | (4500×2) + ((3000+300)×1) | 12300 | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::totalSumsLines` | unit |
| PRC-003 | 금액·가격 | 빈 주문은 거부한다 | 주문 줄 0개 | IllegalArgumentException | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::emptyOrderIsRejected` | unit |
| PRC-004 | 금액·가격 | 수량 범위(1..20)를 지킨다 | 수량 0, 21 | 둘 다 거부 | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::quantityLimitIsEnforced` | unit |
| PRC-005 | 금액·가격 | 같은 메뉴가 두 줄이면 합쳐서 재고를 검사한다 | 메뉴 7 이 2줄(2+3), 메뉴 3 이 1줄 | {3:1, 7:5}, 메뉴 번호 순 | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::sameItemOnTwoLinesIsMergedForStockCheck` | unit |
| PRC-006 | 금액·가격 | 수수료는 정수 원 단위로 0.5 올림 | 1.5% 수수료, 순매출 1000·100·99 | 15, 2, 1 | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::feeRoundsHalfUpInIntegerMinorUnits` | unit |
| PRC-007 | 금액·가격 | 순매출이 0 이하면 수수료 0 | 순매출 0, -500 | 0, 0 | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::feeIsZeroWhenNetIsNotPositive` | unit |
| PRC-008 | 금액·가격 | 금액 덧셈 오버플로를 잡는다 | Long.MAX + 1 | IllegalArgumentException | `domain/src/commonTest/kotlin/orderlane/domain/PricingTest.kt::additionOverflowIsDetected` | unit |
| PAY-001 | 결제 상태 기계 | 승인 응답 | REQUESTED + (REQUEST, APPROVED) | APPROVED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::requestApprovedMovesToApproved` | unit |
| PAY-002 | 결제 상태 기계 | 거절 응답 | REQUESTED + (REQUEST, DECLINED) | DECLINED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::requestDeclinedMovesToDeclined` | unit |
| PAY-003 | 결제 상태 기계 | 응답 시간 초과 | REQUESTED + (REQUEST, TIMEOUT) | UNKNOWN(조회 0회) | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::requestTimeoutMovesToUnknown` | unit |
| PAY-004 | 결제 상태 기계 | 0원 결제는 만들 수 없다 | 금액 0 | IllegalArgumentException | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::zeroAmountPaymentCannotBeCreated` | unit |
| PAY-005 | 결제 상태 기계 | 콜백이 동기 응답보다 먼저 도착 | REQUESTED + (CALLBACK, APPROVED) 후 (REQUEST, APPROVED) | 먼저 APPROVED 로 전이, 뒤의 응답은 중복으로 무시 | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::callbackCanBeatTheSynchronousResponse` | unit |
| PAY-006 | 결제 상태 기계 | REQUESTED 에서 조회 결과는 받지 않는다 | REQUESTED + (INQUIRY, APPROVED) | Rejected | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::inquiryIsNotAllowedWhileStillRequested` | unit |
| PAY-007 | 결제 상태 기계 | 결과 불명을 조회로 승인 확정 | UNKNOWN(1) + (INQUIRY, APPROVED) | APPROVED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::unknownResolvedToApprovedByInquiry` | unit |
| PAY-008 | 결제 상태 기계 | 조회 결과 없음은 승인된 적 없음 | UNKNOWN + (INQUIRY, NOT_FOUND) | DECLINED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::unknownNotFoundByInquiryMeansDeclined` | unit |
| PAY-009 | 결제 상태 기계 | 콜백의 NOT_FOUND 는 거절 근거가 아니다 | UNKNOWN + (CALLBACK, NOT_FOUND) | Rejected, 상태 유지 | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::notFoundFromACallbackDoesNotDecline` | unit |
| PAY-010 | 결제 상태 기계 | 처리 중 응답이 이어지면 횟수를 세다가 보류 | 최대 3회, UNKNOWN 에 INQUIRY/PENDING 3번 | UNKNOWN(1) → UNKNOWN(2) → HELD(3) | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::pendingInquiriesCountUpAndEndInHeld` | unit |
| PAY-011 | 결제 상태 기계 | 조회 자체의 시간 초과도 횟수에 센다 | UNKNOWN(2) + (INQUIRY, TIMEOUT), 최대 3회 | HELD | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::inquiryTimeoutAlsoCountsTowardHeld` | unit |
| PAY-012 | 결제 상태 기계 | 보류는 처리 중이면 유지, 확정되면 해소 | HELD + PENDING, APPROVED, DECLINED | HELD(4) 유지, APPROVED, DECLINED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::heldStaysHeldWhilePendingAndResolvesLater` | unit |
| PAY-013 | 결제 상태 기계 | 중복 승인 콜백 | APPROVED + (CALLBACK, APPROVED) | Ignored(duplicate-approved), 상태 그대로 | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::duplicateApprovalCallbackIsIgnored` | unit |
| PAY-014 | 결제 상태 기계 | 확정 뒤에 늦게 온 처리 중 통지 | APPROVED·DECLINED·UNKNOWN + (CALLBACK, PENDING) | 모두 Ignored(stale-pending) | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::staleCallbackAfterResolutionIsIgnored` | unit |
| PAY-015 | 결제 상태 기계 | 승인된 결제에 거절 통지 | APPROVED·CANCELLED + DECLINED | Rejected(conflict) | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::declinedAfterApprovedIsRejectedAsConflict` | unit |
| PAY-016 | 결제 상태 기계 | 거절 확정된 결제에 승인 통지 | DECLINED + APPROVED | Rejected(conflict) | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::approvedAfterDeclinedIsRejectedAsConflict` | unit |
| PAY-017 | 결제 상태 기계 | 모든 (상태, 결과, 출처) 조합에 정의된 결과가 있다 | 7 상태 × 5 결과 × 3 출처 = 105 조합 | 예외 없이 결과를 내고, 거절·취소 확정 상태는 어떤 통지로도 전이하지 않으며 승인 상태는 통지로 바뀌지 않는다 | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::everyStateOutcomeSourcePairHasADefinedResult` | unit |
| PAY-018 | 결제 상태 기계 | 부분 취소 | APPROVED(10000) 에서 3000 취소 | APPROVED, 남은 7000, 이름 PARTIALLY_CANCELLED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::partialCancelKeepsApprovedWithRemaining` | unit |
| PAY-019 | 결제 상태 기계 | 남은 금액을 모두 취소 | 부분 취소 3000 뒤 전체 취소, 또는 7000 지정 | CANCELLED | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::cancellingTheRemainderBecomesFullyCancelled` | unit |
| PAY-020 | 결제 상태 기계 | 남은 금액 초과 취소 | 남은 1000 에 1001 취소 | exceeds-remaining | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::cancelAboveRemainingIsRejected` | unit |
| PAY-021 | 결제 상태 기계 | 0 이하 취소 금액 | 금액 0 | invalid-amount | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::nonPositiveCancelAmountIsRejected` | unit |
| PAY-022 | 결제 상태 기계 | 승인 상태가 아니면 취소할 수 없다 | REQUESTED·UNKNOWN·HELD·DECLINED·CANCELLED | 모두 not-cancellable | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::cancelIsRejectedUnlessApproved` | unit |
| PAY-023 | 결제 상태 기계 | 같은 취소 키는 다시 적용하지 않는다 | 이미 적용된 키 c1 로 재요청 | Replayed(처음 금액), 상태 그대로 | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::sameCancelKeyIsReplayedWithoutApplyingAgain` | unit |
| PAY-024 | 결제 상태 기계 | 잘못된 부분 취소 상태는 만들 수 없다 | 취소액 = 결제액인 Approved | IllegalArgumentException(타입이 막는다) | `domain/src/commonTest/kotlin/orderlane/domain/PaymentMachineTest.kt::partialCancelStateRejectsInvalidConstruction` | unit |
| IDM-001 | 멱등 판정 | 처음 보는 키 | 기록 없음 | Proceed | `domain/src/commonTest/kotlin/orderlane/domain/IdempotencyTest.kt::unseenKeyProceeds` | unit |
| IDM-002 | 멱등 판정 | 완료된 요청의 재전송 | 같은 해시, COMPLETED | Replay(저장한 상태·본문) | `domain/src/commonTest/kotlin/orderlane/domain/IdempotencyTest.kt::completedSameBodyReplaysStoredResponse` | unit |
| IDM-003 | 멱등 판정 | 같은 키, 다른 본문 | 해시가 다름: COMPLETED·IN_PROGRESS 모두 | Mismatch | `domain/src/commonTest/kotlin/orderlane/domain/IdempotencyTest.kt::differentBodyIsMismatchRegardlessOfStatus` | unit |
| IDM-004 | 멱등 판정 | 처리 중 재요청 | IN_PROGRESS, 잠금 시간 안 | InProgress | `domain/src/commonTest/kotlin/orderlane/domain/IdempotencyTest.kt::inProgressWithinLockIsConflict` | unit |
| IDM-005 | 멱등 판정 | 처리하던 쪽이 죽은 뒤 | IN_PROGRESS, 잠금 시간이 지남 | Reclaim | `domain/src/commonTest/kotlin/orderlane/domain/IdempotencyTest.kt::inProgressAfterLockExpiryIsReclaimed` | unit |
| STK-001 | 재고 규칙 | 예약은 재고를 줄이고 버전을 올린다 | 재고 10 에서 3 예약 | 재고 7, 버전 1 | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::reserveDecrementsAndBumpsVersion` | unit |
| STK-002 | 재고 규칙 | 재고보다 많이 예약할 수 없다 | 재고 2 에 3 예약 | canReserve=false, 예약 시 예외 | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::cannotReserveMoreThanAvailable` | unit |
| STK-003 | 재고 규칙 | 마지막 1개는 한 번만 예약된다 | 재고 1 에 1 예약 후 다시 1 | 첫 번째만 성공, 재고 0 | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::lastUnitCanBeReservedExactlyOnce` | unit |
| STK-004 | 재고 규칙 | 반환은 재고를 되돌린다 | 5 에서 2 예약 후 2 반환 | 재고 5, 버전 2 | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::releaseRestoresStock` | unit |
| STK-005 | 재고 규칙 | 음수 재고는 만들 수 없다 | StockCell(-1) | IllegalArgumentException | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::negativeStockCannotBeConstructed` | unit |
| EVT-001 | 이벤트 소비 규칙 | 이미 처리한 이벤트 ID 는 건너뛴다 | 처리 이력 있음/없음 | SkipDuplicate / Process | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::alreadyProcessedEventIsSkipped` | unit |
| EVT-002 | 이벤트 소비 규칙 | 실패하면 지연을 두 배씩 늘려 재시도 | 최대 3회, 기본 200ms, 1·2번째 실패 | Retry(2, 200ms), Retry(3, 400ms) | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::failuresRetryWithDoublingDelay` | unit |
| EVT-003 | 이벤트 소비 규칙 | 마지막 시도에서 실패하면 dead-letter | 3번째·9번째 실패 | DeadLetter | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::lastAttemptFailureGoesToDeadLetter` | unit |
| EVT-004 | 이벤트 소비 규칙 | 시도 1회 정책은 바로 dead-letter | maxAttempts=1 | DeadLetter | `domain/src/commonTest/kotlin/orderlane/domain/InventoryAndEventsTest.kt::singleAttemptPolicyDeadLettersImmediately` | unit |
| SET-001 | 정산 | 매장별 매출·취소·수수료 | 매장 2곳, 수수료 3%, 취소 포함 | 매장 번호 순, 수수료=(매출-취소)×3% 반올림, 순액=매출-취소-수수료 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::settlesPerStoreWithSalesCancelsAndFee` | unit |
| SET-002 | 정산 | 같은 이벤트는 한 번만 센다 | 같은 eventId 두 줄 | 한 줄일 때와 같은 결과 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::sameEventIdIsCountedOnce` | unit |
| SET-003 | 정산 | 입력 순서와 무관 | 입력을 뒤집어 정산 | 같은 결과 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::resultDoesNotDependOnInputOrder` | unit |
| SET-004 | 정산 | 전액 취소된 매출은 수수료가 없다 | 매출 1000, 취소 1000 | 수수료 0, 순액 0 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::fullyCancelledSaleHasNoFee` | unit |
| SET-005 | 정산 | 입력이 없으면 매장도 없다 | 빈 원장 | 빈 목록 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::emptyInputHasNoStores` | unit |
| REC-001 | 대사 | 일치하는 파일 | 원장 2건 = 거래 내역 2건 | 불일치 0, 일치 2 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::matchingFilesHaveNoMismatch` | unit |
| REC-002 | 대사 | 원장에 없는 거래 | 결제사에만 p2 | MISSING_IN_LEDGER | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::transactionMissingInLedgerIsReported` | unit |
| REC-003 | 대사 | 결제사에 없는 거래 | 원장에만 p2 | MISSING_AT_PROVIDER | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::transactionMissingAtProviderIsReported` | unit |
| REC-004 | 대사 | 금액 차이 | 원장 100, 결제사 90 | AMOUNT_DIFFERENCE, 양쪽 금액 보고 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::amountDifferenceIsReportedWithBothAmounts` | unit |
| REC-005 | 대사 | 결제사 파일의 중복 행 | 같은 거래 두 줄 | DUPLICATE_AT_PROVIDER, 합계 200 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::duplicateRowAtProviderIsReported` | unit |
| REC-006 | 대사 | 원장의 중복 | 같은 참조 두 줄 | DUPLICATE_IN_LEDGER | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::duplicateLedgerEntryIsReported` | unit |
| REC-007 | 대사 | 매출과 취소는 같은 참조여도 다른 키 | 같은 ref 의 SALE·CANCEL | 불일치 없음 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::cancelAndSaleWithSameRefAreDifferentKeys` | unit |
| REC-008 | 대사 | 다시 돌려도 같은 보고서 | 입력 순서를 뒤집어 대사 | 같은 보고서, 불일치 3건 | `domain/src/commonTest/kotlin/orderlane/domain/SettlementTest.kt::reportIsStableAcrossReruns` | unit |
| SIM-001 | 도메인 시뮬레이션 | 정상 흐름 | happy 시나리오 | 검증 통과, APPROVED 단계 포함 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::happyPathEndsPaid` | unit |
| SIM-002 | 도메인 시뮬레이션 | 중복 요청 4가지 | replay·in-flight·mismatch·reclaim | 모두 통과 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::duplicateRequestVariantsAllHoldTheirInvariant` | unit |
| SIM-003 | 도메인 시뮬레이션 | 잠금 방식은 초과 판매가 없다 | 재고 3, 12명, 낙관적·비관적 | 판매 3 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::lockedStrategiesNeverOversell` | unit |
| SIM-004 | 도메인 시뮬레이션 | 잠금 없는 구현은 초과 판매 | 재고 1, 5명, naive | 검증 실패, 초과 판매 4 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::unlockedReadThenWriteOversells` | unit |
| SIM-005 | 도메인 시뮬레이션 | 결제 시간 초과 3가지 | 조회 승인·조회 없음·보류 후 확정 | 모두 확정 상태로 끝남 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::paymentTimeoutVariantsAllResolve` | unit |
| SIM-006 | 도메인 시뮬레이션 | 콜백 시나리오 | 승인#2, 승인#2, 처리중#1, 거절#3 | 반영 1, 거부 1 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::callbackScenarioAppliesOnceIgnoresDuplicatesAndRejectsConflict` | unit |
| SIM-007 | 도메인 시뮬레이션 | 소비자 실패 2가지 | 일시 실패 / 계속 실패 | 회복 / dead-letter 후 재처리, 다른 소비자는 중복 건너뜀 | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::consumerFailureRecoversOrDeadLettersThenReplaysOnce` | unit |
| SIM-008 | 도메인 시뮬레이션 | JSON 직렬화 | 따옴표·줄바꿈 이스케이프 | 올바른 JSON | `domain/src/commonTest/kotlin/orderlane/domain/SimulationTest.kt::resultSerializesToJsonWithEscapes` | unit |
| GW-001 | 모의 결제 게이트웨이 | 토큰으로 승인·거절 | tok_approve, tok_decline | APPROVED, DECLINED | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::approvesAndDeclinesByToken` | unit |
| GW-002 | 모의 결제 게이트웨이 | 같은 결제 참조는 처음 결과 | 같은 ref 로 tok_decline 재요청 | 처음의 txId·APPROVED | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::samePaymentRefReturnsTheFirstResult` | unit |
| GW-003 | 모의 결제 게이트웨이 | 늦은 응답이어도 승인은 기록된다 | tok_timeout_approved | 응답 지연 후, 조회는 APPROVED | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::lateResponseStillRecordsTheApprovalForInquiry` | unit |
| GW-004 | 모의 결제 게이트웨이 | 도착하지 않은 요청 | tok_timeout_lost | 응답 504, 조회 404 | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::lostRequestIsNotFoundOnInquiry` | unit |
| GW-005 | 모의 결제 게이트웨이 | 보류 후 승인 | tok_hold | 조회: PENDING 다음 APPROVED | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::holdStaysPendingThenApproves` | unit |
| GW-006 | 모의 결제 게이트웨이 | 취소는 키로 멱등, 남은 금액 한도 | 같은 키 두 번, 초과 금액 | 같은 cancelTxId·남은 600, 초과는 422 | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::cancelIsIdempotentByKeyAndCappedAtRemaining` | unit |
| GW-007 | 모의 결제 게이트웨이 | 거래 내역 파일과 이상 주입 | missing·amountDiff·duplicate 주입 | 누락 행 없음, 금액 변경, 중복 행 | `gateway/src/test/kotlin/orderlane/gateway/GatewayTest.kt::settlementFileListsSalesAndInjectedAnomalies` | unit |
| ADP-001 | 앱 어댑터 | 결제 상태 ↔ DB 컬럼 | 모든 상태를 저장 형식으로 바꿨다 복원 | 같은 상태 | `app/src/test/kotlin/orderlane/app/UnitTests.kt::everyStateSurvivesAColumnRoundTrip` | unit |
| ADP-002 | 앱 어댑터 | 알 수 없는 상태 이름 | REFUNDED | IllegalStateException | `app/src/test/kotlin/orderlane/app/UnitTests.kt::unknownStateNameIsRejected` | unit |
| ADP-003 | 앱 어댑터 | 거래 내역 파일 읽기 | 헤더·빈 줄 포함 CSV | 행 2개 | `app/src/test/kotlin/orderlane/app/UnitTests.kt::parsesRowsAndSkipsHeaderAndBlankLines` | unit |
| ADP-004 | 앱 어댑터 | 깨진 행은 조용히 넘기지 않는다 | 열 부족, 숫자 아님 | 예외 | `app/src/test/kotlin/orderlane/app/UnitTests.kt::malformedRowFailsLoudlyInsteadOfBeingSkipped` | unit |
| ADP-005 | 앱 어댑터 | 요청 해시 | 같은/다른 문자열 | 같으면 같고 다르면 다름(64자) | `app/src/test/kotlin/orderlane/app/UnitTests.kt::hashIsStableAndSensitiveToContent` | unit |
| ORD-001 | 주문·멱등·재고(통합) | 주문 생성, 금액은 서버가 계산 | Americano+Large ×2 | 201 PAID, 10000원, 재고 198, 아웃박스 2건 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::placesOrderAndChargesServerComputedTotal` | integration |
| ORD-002 | 주문·멱등·재고(통합) | 잘못된 요청 | 토큰 없음, 키 없음, 없는 매장·다른 매장 메뉴, 수량 0, 다른 메뉴 옵션 | 401·400, 주문 0건 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::rejectsBadRequests` | integration |
| ORD-003 | 주문·멱등·재고(통합) | 같은 키·같은 본문 재전송 | 같은 요청 두 번 | 같은 응답, 주문 1건, 재고 1만 감소 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::sameKeyAndBodyReplaysTheFirstResponse` | integration |
| ORD-004 | 주문·멱등·재고(통합) | 같은 키·다른 본문 | 수량만 다르게 | 422 IDEMPOTENCY_KEY_REUSED, 주문 1건 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::sameKeyWithDifferentBodyIsRejected` | integration |
| ORD-005 | 주문·멱등·재고(통합) | 처리 중 재요청 | tok_slow_400 요청이 진행 중일 때 재요청 | 409 IN_PROGRESS, 완료 뒤 재요청은 같은 응답, 주문 1건 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::requestRetriedWhileFirstIsInFlightGetsConflictThenReplay` | integration |
| ORD-006 | 주문·멱등·재고(통합) | 처리하던 서버가 죽은 뒤 이어받기 | 주문·결제 행이 있고 키는 잠금이 지난 IN_PROGRESS | 같은 주문으로 마무리, 주문 1건, 재고 한 번만 차감 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::abandonedInProgressKeyIsReclaimedWithoutCreatingASecondOrder` | integration |
| ORD-007 | 주문·멱등·재고(통합) | 마지막 1개 품절과 재요청 | 한정 메뉴 2번 주문, 품절 요청 재전송 | 409 SOLD_OUT, 재전송도 같은 응답 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::lastUnitSoldOnceThenSoldOutIsReplayedConsistently` | integration |
| ORD-008 | 주문·멱등·재고(통합) | 한 줄이 품절이면 다른 줄 예약도 되돌린다 | 1번 메뉴 2개 + 품절 메뉴 | 409, 1번 메뉴 재고 변화 없음 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::soldOutOnSecondLineRollsBackTheFirstReservation` | integration |
| ORD-009 | 주문·멱등·재고(통합) | 거절되면 재고 반환 | tok_decline | 402 PAYMENT_FAILED, 재고 복구, 다시 주문 가능 | `app/src/test/kotlin/orderlane/app/OrderApiIT.kt::declinedPaymentReleasesStockAndMarksOrderFailed` | integration |
| PMT-001 | 결제 해소·콜백·취소(통합) | 시간 초과 후 조회로 승인 확정 | tok_timeout_approved | 202 대기 → 재조회로 PAID, 승인 이벤트 1건 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::timedOutPaymentIsPendingThenConfirmedApprovedByInquiry` | integration |
| PMT-002 | 결제 해소·콜백·취소(통합) | 요청이 사라진 결제 | tok_timeout_lost | 202 → 조회 NOT_FOUND → PAYMENT_FAILED, 재고 반환 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::requestLostAtGatewayIsConfirmedDeclinedAndStockReturns` | integration |
| PMT-003 | 결제 해소·콜백·취소(통합) | 보류 후 재조회로 확정 | tok_hold | HELD 로 전환 → 이후 APPROVED, 주문 PAID | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::unresolvedPaymentIsHeldThenSweptWhenGatewayConfirms` | integration |
| PMT-004 | 결제 해소·콜백·취소(통합) | 중복·뒤바뀐 콜백 | tok_timeout_callbacks | 승인 이벤트 1건, 원장 1건 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::duplicateAndReorderedCallbacksApplyExactlyOnce` | integration |
| PMT-005 | 결제 해소·콜백·취소(통합) | 충돌 콜백은 거부하고 기록 | 승인된 결제에 DECLINED 콜백, 비밀 헤더 없는 콜백 | result=rejected, 상태 유지, 충돌 기록 1건, 헤더 없으면 401 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::conflictingCallbackIsRejectedAndRecordedWithoutChangingState` | integration |
| PMT-006 | 결제 해소·콜백·취소(통합) | 부분 취소 후 나머지 취소 | 5500원 중 2000, 나머지 | PARTIALLY_CANCELLED(남은 3500) → CANCELLED, 전체 취소 시에만 재고 반환 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::partialThenRemainingCancellationEndsCancelledAndReturnsStock` | integration |
| PMT-007 | 결제 해소·콜백·취소(통합) | 같은 취소 키는 한 번만 | 같은 키 두 번, 같은 키 다른 금액 | 같은 응답·취소 1건, 다른 금액은 422 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::sameCancelKeyIsAppliedOnlyOnce` | integration |
| PMT-008 | 결제 해소·콜백·취소(통합) | 취소 불가 조건 | 남은 금액 초과, 거절된 주문, 없는 주문 | 422 EXCEEDS_REMAINING, 409 NOT_CANCELLABLE, 404 | `app/src/test/kotlin/orderlane/app/PaymentResolutionIT.kt::cancellationAboveRemainingOrOnUnpaidOrderIsRejected` | integration |
| EVF-001 | 아웃박스·소비자(통합) | 아웃박스 → 브로커 → 소비자 3개 | 주문 1건 | 주방 NEW 1, 알림 1, 원장 SALE 9000원, 미발행 0 | `app/src/test/kotlin/orderlane/app/EventFlowIT.kt::outboxEventsReachKitchenNotificationAndLedger` | integration |
| EVF-002 | 아웃박스·소비자(통합) | 취소는 주방에서 빼고 원장에 남긴다 | 전체 취소 | 주방 VOID, 원장 CANCEL 1 | `app/src/test/kotlin/orderlane/app/EventFlowIT.kt::cancelledOrderLeavesKitchenAndLedgerRecordsCancel` | integration |
| EVF-003 | 아웃박스·소비자(통합) | 같은 이벤트를 세 번 더 발행 | 같은 eventId 재발행 ×3 | 원장·알림 1건, 처리 이력은 소비자당 1 | `app/src/test/kotlin/orderlane/app/EventFlowIT.kt::redeliveredEventIsProcessedOnceByEveryConsumer` | integration |
| EVF-004 | 아웃박스·소비자(통합) | 일시 실패는 재시도로 회복 | 알림 소비자 2번 실패 주입 | 알림 1건, dead-letter 0 | `app/src/test/kotlin/orderlane/app/EventFlowIT.kt::transientConsumerFailureIsRetriedAndSucceeds` | integration |
| EVF-005 | 아웃박스·소비자(통합) | 계속 실패하면 dead-letter, 재처리 명령 | 알림 소비자 실패 주입 후 해제, 재처리 | 다른 소비자는 영향 없음, 재처리 후 알림 1건, 원장 중복 없음 | `app/src/test/kotlin/orderlane/app/EventFlowIT.kt::persistentConsumerFailureGoesToDeadLetterThenReplayDeliversWithoutDuplicates` | integration |
| EVF-006 | 아웃박스·소비자(통합) | 요청 추적 ID | X-Request-Id 전달 | 응답 헤더에 같은 값, 아웃박스 이벤트에 traceId | `app/src/test/kotlin/orderlane/app/EventFlowIT.kt::requestIdIsEchoedAndCarriedIntoTheEvent` | integration |
| OPS-001 | 정산·대사·캐시·운영성(통합) | 일 정산과 재실행 | 매장 1: 14000 매출·2000 취소, 매장 2: 3800 | 수수료 360·114, 순액 11640, 재실행 결과 동일, 행 2개 | `app/src/test/kotlin/orderlane/app/OperationsIT.kt::dailySettlementIsPerStoreAndRerunGivesTheSameResult` | integration |
| OPS-002 | 정산·대사·캐시·운영성(통합) | 대사 | 깨끗한 파일, 누락·금액 차이·중복 주입 | 일치 4 / 불일치 3종 보고, 재실행 동일 | `app/src/test/kotlin/orderlane/app/OperationsIT.kt::reconciliationMatchesCleanFilesAndReportsInjectedMismatches` | integration |
| OPS-003 | 정산·대사·캐시·운영성(통합) | 메뉴 읽기 캐시와 무효화 | 조회 2회, 주문, 취소 | 두 번째는 hit, 재고 변경 뒤에는 새 값 | `app/src/test/kotlin/orderlane/app/OperationsIT.kt::menuReadIsCachedAndInvalidatedWhenStockChanges` | integration |
| OPS-004 | 정산·대사·캐시·운영성(통합) | 헬스·지표 | 주문 1건 뒤 조회 | UP, readiness 200, 주문·결제·아웃박스·소비 지표 노출 | `app/src/test/kotlin/orderlane/app/OperationsIT.kt::healthAndMetricsAreExposed` | integration |
| CON-001 | 재고 동시성(통합) | 낙관적 잠금, 반복 라운드 | 재고 1·5, 동시 24명, 각 25라운드 | 매 라운드 판매=재고, 나머지 409, 종료 재고 0, 초과 판매 0 | `app/src/test/kotlin/orderlane/app/ConcurrencyIT.kt::optimisticLockingNeverOversellsAcrossRepeatedRounds` | integration |
| CON-002 | 재고 동시성(통합) | 비관적 잠금, 반복 라운드 | 같은 조건 | 같은 결과 | `app/src/test/kotlin/orderlane/app/ConcurrencyIT.kt::pessimisticLockingNeverOversellsAcrossRepeatedRounds` | integration |
| E2E-001 | 전체 구성 e2e | 컨테이너 전체를 지나는 주문 | 주문 1건 | PAID, 주방·알림·원장 소비자 처리 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::orderFlowsThroughEveryContainerToKitchenNotificationAndLedger` | e2e |
| E2E-002 | 전체 구성 e2e | 중복 요청 | 같은 키 재전송, 다른 본문, 처리 중 재요청 | 같은 응답, 422, 409, 완료 후 같은 응답, 주문 2건 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::duplicateRequestsAreSafeAcrossTheWire` | e2e |
| E2E-003 | 전체 구성 e2e | 마지막 1개 동시 주문, 두 방식 | 20명 동시, 방식당 8회 | 매번 판매 1·품절 19·재고 0 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::lastUnitIsSoldOnceUnderConcurrencyWithBothLockingStrategies` | e2e |
| E2E-004 | 전체 구성 e2e | 취소 멱등 | 부분 1500, 같은 키 재요청, 초과, 나머지 | 같은 응답·취소 1건, 422, CANCELLED | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::cancellationsAreIdempotentAndPartialCancelKeepsTheOrderPaid` | e2e |
| E2E-005 | 전체 구성 e2e | 결제 시간 초과 | tok_timeout_approved, tok_timeout_lost | PAID, PAYMENT_FAILED·재고 반환 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::paymentTimeoutIsResolvedByInquiry` | e2e |
| E2E-006 | 전체 구성 e2e | 결제 보류 | tok_hold | HELD 후 APPROVED | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::heldPaymentIsSweptToApproved` | e2e |
| E2E-007 | 전체 구성 e2e | 게이트웨이의 중복·뒤바뀐 콜백 | tok_timeout_callbacks | 승인 이벤트 1, 원장 1 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::duplicateAndReorderedCallbacksFromTheGatewayApplyOnce` | e2e |
| E2E-008 | 전체 구성 e2e | 소비자 실패와 재처리 | 알림 소비자 실패 주입, 재처리 명령 | 알림 1건, 원장 중복 없음 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::consumerFailureGoesToDeadLetterAndReplayCommandRecovers` | e2e |
| E2E-009 | 전체 구성 e2e | 정산·대사 | 매출 4건, 파일 이상 3종 주입 | 수수료 405, 재실행 동일, 대사 3종 보고 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::settlementAndReconciliationAgainstTheGatewayFile` | e2e |
| E2E-010 | 전체 구성 e2e | 종료 중 요청 | 처리 중 주문 + docker stop(SIGTERM) | 201 로 끝남, 종료 코드 143, 로그에 graceful 종료, 재기동 뒤 이벤트 도착 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::inFlightRequestFinishesWhenTheServerIsTerminated` | e2e |
| E2E-011 | 전체 구성 e2e | 운영성 | X-Request-Id 전달 | 헤더·로그에 같은 ID, 헬스 UP, 지표 노출 | `e2e/src/test/kotlin/orderlane/e2e/StackE2E.kt::healthMetricsAndRequestIdAreExposed` | e2e |
| WEB-001 | 데모 사이트 | 엔진 구동 | 페이지 열기 | 다크 기본, 배너 상시 표시, 첫 문장, 정상 흐름·APPROVED, 규칙 호출 표, 콘솔 오류 0 | `web/e2e/demo.spec.ts::도메인 엔진(Kotlin→JS)이 뜨고 첫 화면 문장과 기본 결과가 나오며 콘솔 오류가 없다` | e2e |
| WEB-002 | 데모 사이트 | 같은 주문이 두 번 시나리오 | 4가지 고장 선택 | Replay·409·422·Reclaim | `web/e2e/demo.spec.ts::같은 주문이 두 번: 재전송은 같은 응답, 처리 중은 충돌, 다른 내용은 거부, 죽은 뒤에는 이어받는다` | e2e |
| WEB-003 | 데모 사이트 | 동시 주문 시나리오 | 방식 3가지, 재고·인원 변경 | 잠금은 초과 판매 0, 비교용은 4 / 7 | `web/e2e/demo.spec.ts::마지막 1개 동시 주문: 두 잠금 방식은 초과 판매가 없고 잠금 없는 비교용은 초과 판매가 난다` | e2e |
| WEB-004 | 데모 사이트 | 결제 응답이 안 옴 시나리오 | 3가지 고장 선택 | 승인 확정 / 재고 반환 / HELD 후 APPROVED | `web/e2e/demo.spec.ts::결제 응답이 안 옴: 다시 물어봐서 승인·거절을 확정하고, 확정 못 하면 보류했다가 확정한다` | e2e |
| WEB-005 | 데모 사이트 | 결제사 알림 시나리오 | 실행 | 반영 1·무시 2·거부 1 | `web/e2e/demo.spec.ts::결제사 알림이 중복·뒤바뀜: 한 번만 반영하고 충돌은 불일치로 남긴다` | e2e |
| WEB-006 | 데모 사이트 | 후속 처리 실패 시나리오 | 2가지 고장 선택 | 재시도 예약 / dead-letter 후 중복 건너뜀 2건 | `web/e2e/demo.spec.ts::후속 처리 실패: 재시도로 회복하거나 보관함으로 간 뒤 재처리되고 다른 소비자는 중복을 건너뛴다` | e2e |
| WEB-007 | 데모 사이트 | 증거 표시 | 페이지 열기 | 종류별 통과 수, 테스트 목록, 동시성 4행 초과 판매 0, e2e 시나리오 9개 이상 | `web/e2e/demo.spec.ts::실제 테스트 결과·동시성 반복 결과·e2e 시나리오가 결과 파일에서 읽혀 표시된다` | e2e |
| WEB-008 | 데모 사이트 | 테마 | 열기 → 전환 버튼 | 다크 배경 → 라이트 배경 | `web/e2e/demo.spec.ts::다크가 기본이고 라이트로 전환할 수 있다` | e2e |
| WEB-009 | 데모 사이트 | 접근성(대비) | 본문·보조 글자·제목·버튼·표 머리글·코드의 색을 계산 | 모두 4.5:1 이상 | `web/e2e/demo.spec.ts::글자 대비가 라이트·다크 모두 4.5:1 이상이다` | e2e |
| WEB-010 | 데모 사이트 | 모바일 | 390px | 가로 넘침 없음, 패널이 한 열 | `web/e2e/demo.spec.ts::390px 폭에서 가로 스크롤 없이 1열로 보인다` | e2e |
| WEB-011 | 데모 사이트 | 키보드 접근성 | 방향키 | 초점 윤곽 표시, 상황 전환 | `web/e2e/demo.spec.ts::키보드로 상황을 바꾸고 초점이 보인다` | e2e |
| WEB-012 | 데모 사이트 | 외부 요청 없음 | 페이지 로드 | 모든 요청이 같은 출처 | `web/e2e/demo.spec.ts::외부 요청 없이 같은 출처의 파일만 불러온다` | e2e |
| WEB-013 | 데모 사이트 | 안내 투어 | 시작 → 다음 반복 → 마치기 | 장마다 시나리오 자동 선택·패널 강조, 마지막에 닫힘 | `web/e2e/demo.spec.ts::설명 따라가기: 시작하면 1/8 장이 열리고, 장을 넘기면 시나리오가 자동 선택되고 볼 패널이 강조된다` | e2e |
| WEB-014 | 데모 사이트 | 안내 투어 조작 | 방향키, 접기, 설명 열기 | 장 이동, 접어도 열기 버튼 표시·초점, 이어서 열림 | `web/e2e/demo.spec.ts::설명 따라가기: ← → 키로 넘기고, 접어도 여는 버튼이 보이며 다시 열면 이어진다` | e2e |
| WEB-015 | 데모 사이트 | 안내 투어 자동 진행 | 자동 진행 켜기/끄기(간격 0.6초) | 저절로 넘어가고 끄면 멈춤 | `web/e2e/demo.spec.ts::설명 따라가기: 자동 진행을 켜면 장이 저절로 넘어가고 끄면 멈춘다` | e2e |
| WEB-016 | 데모 사이트 | 안내 투어 모바일 | 390px, 8장 모두 | 시트 높이 ≤ 화면 46%, 본문 하단 여백 확보, 강조 패널 시작이 시트 위에 보임 | `web/e2e/demo.spec.ts::설명 따라가기: 390px 에서 하단 시트가 강조된 패널을 가리지 않는다` | e2e |
| WEB-017 | 데모 사이트 | 첫 방문 자동 시작 | 쿠키 없이 열기 | 1/8 장 자동 열림, 초점 유지, 푸터에 쿠키 안내, 열린 것만으로 쿠키를 남기지 않음, 콘솔 오류 0 | `web/e2e/demo.spec.ts::첫 방문: 쿠키가 없으면 도메인 엔진이 준비된 뒤 설명이 1/8 장부터 자동으로 열리고 초점을 빼앗지 않는다` | e2e |
| WEB-018 | 데모 사이트 | 첫 방문 자동 시작 | 접기 → 새로고침 | 쿠키 ol_tour=seen(path /orderlane/, Lax, 약 180일), 열리지 않음, 시작 버튼으로 다시 열림 | `web/e2e/demo.spec.ts::첫 방문: 접은 뒤 새로고침하면 열리지 않고, 설명 열기 버튼으로 다시 열 수 있다` | e2e |
| WEB-019 | 데모 사이트 | 첫 방문 자동 시작 | 8장 모두 넘기고 마치기 → 새로고침 | 열리지 않음, 다시 열 수 있음 | `web/e2e/demo.spec.ts::첫 방문: 끝까지 본 뒤 새로고침하면 열리지 않고, 다시 열기 버튼이 있다` | e2e |
| WEB-020 | 데모 사이트 | 쿠키 차단 | 쿠키 쓰기가 예외를 던지는 환경 | 오류 없이 열리고, 접고 새로고침해도 다시 열림 | `web/e2e/demo.spec.ts::쿠키를 쓸 수 없으면 오류 없이 매번 자동으로 열린다` | e2e |
| WEB-021 | 데모 사이트 | 첫 방문 모바일 | 390px, 쿠키 없음 | 첫 문장 아래가 시트 위, 스크롤 0 | `web/e2e/demo.spec.ts::390px 에서 자동으로 열린 시트가 첫 화면 문장을 가리지 않는다` | e2e |
