#!/usr/bin/env bash
# 로컬 검증: 단위(JVM·JS)·통합(실제 PG·Redis·브로커)·e2e(compose 전체 + 실패 주입)·증거 수집·데모 사이트 빌드와 브라우저 시험·테스트 케이스 문서 대조.
# 올린 컨테이너는 끝나면(실패해도) 내린다. compose 호스트 포트: 58632(PG)·58679(Redis)·58680(앱)·58690(게이트웨이). 통합 테스트는 임의 포트.
set -euo pipefail
cd "$(dirname "$0")/.."
step() { printf '\n== %s ==\n' "$*"; }
cleanup() { docker compose down -v --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT
rm -rf build/evidence .test-results
mkdir -p .test-results build/evidence

step "단위·통합 테스트 (도메인 JVM·JS, 게이트웨이, 앱 단위, 앱 통합: 실제 PostgreSQL·Redis·Redpanda)"
bash scripts/gradle.sh :domain:jvmTest :domain:jsNodeTest :gateway:test :app:test --console=plain 2>&1 | grep -E 'PASSED|FAILED|SKIPPED|BUILD|tests completed' | awk '{c[$NF]++} /BUILD|tests completed|FAILED/ {print} END {printf "통과 %d, 실패 %d, 건너뜀 %d (JVM 기준 출력)\n", c["PASSED"], c["FAILED"], c["SKIPPED"]}'
test "${PIPESTATUS[0]}" -eq 0

step "e2e (compose 전체 구성 + 결제 게이트웨이 실패 주입 + 동시성 + 종료 중 요청)"
bash scripts/demo.sh up >/dev/null
bash scripts/gradle.sh :e2e:test --console=plain 2>&1 | grep -E 'PASSED|FAILED|BUILD|expected|but was|Expecting|Error' | tee .test-results/e2e.log
test "${PIPESTATUS[0]}" -eq 0
docker compose down -v --remove-orphans >/dev/null 2>&1

step "증거 수집 (테스트 결과·동시성 반복 결과·e2e 관찰 기록)"
node scripts/evidence.mjs

step "데모 사이트 빌드 (도메인 모듈을 JavaScript 로 컴파일해 싣는다)"
bash scripts/build-web.sh

step "데모 사이트 브라우저 시험 (Playwright, 로컬 Chrome)"
(cd web && npm ci --no-audit --no-fund >/dev/null && npx playwright test 2>&1 | grep -E '✓|✘|passed|failed|Error' || true)
test -f .test-results/web.json
node -e 'const r=require("./.test-results/web.json"); console.log(`브라우저 테스트 ${r.stats.expected} 통과, 실패 ${r.stats.unexpected}`); process.exit(r.stats.unexpected?1:0)'

step "테스트 케이스 문서 대조"
node scripts/check-test-cases.mjs

printf '\nverify 통과\n'
