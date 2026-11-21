# Orderlane 진입점. push 전에는 `make verify` 가 0 으로 끝나야 한다.
.PHONY: up down test test-unit test-int e2e evidence web pages verify replay-dlq

# 5분 실행: jar 빌드 → 이미지 → compose 로 전체 구성 기동 (앱 http://127.0.0.1:58680)
up:
	bash scripts/demo.sh up

down:
	bash scripts/demo.sh down

# 단위(JVM·JS) + 통합(Testcontainers 로 실제 PG·Redis·Redpanda 를 띄운다, Docker 필요)
test: test-unit test-int

test-unit:
	bash scripts/gradle.sh :domain:jvmTest :domain:jsNodeTest :gateway:test :app:test --tests '*Test' --console=plain

test-int:
	bash scripts/gradle.sh :app:test --tests '*IT' --console=plain

# compose 전체 구성을 올려 e2e 를 돌리고 내린다
e2e:
	bash scripts/demo.sh up
	bash scripts/gradle.sh :e2e:test --console=plain; rc=$$?; docker compose down -v --remove-orphans; exit $$rc

evidence:
	node scripts/evidence.mjs

web:
	bash scripts/build-web.sh

verify:
	bash scripts/verify.sh

# dead-letter 재처리 명령(앱이 떠 있어야 한다)
replay-dlq:
	bash scripts/replay-dlq.sh

# 데모 사이트를 gh-pages 브랜치에 로컬 커밋한다(push 는 하지 않는다).
pages:
	bash scripts/deploy-pages.sh
