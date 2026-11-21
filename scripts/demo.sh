#!/usr/bin/env bash
# 5분 실행: 앱·결제 게이트웨이 jar 빌드 → 이미지 → compose 로 전체 구성 기동. 앱은 http://127.0.0.1:58680
set -euo pipefail
cd "$(dirname "$0")/.."
case "${1:-up}" in
  up)
    bash scripts/gradle.sh :app:bootJar :gateway:installDist -q
    docker compose build
    docker compose up -d --wait
    cat <<'MSG'

구성이 올라왔습니다.
  앱         http://127.0.0.1:58680   (Authorization: Bearer demo-token)
  결제 게이트웨이(모의) http://127.0.0.1:58690
  지표       http://127.0.0.1:58680/actuator/prometheus

주문 한 건:
  curl -s -XPOST localhost:58680/orders -H 'Authorization: Bearer demo-token' -H 'Idempotency-Key: demo-1' \
    -H 'Content-Type: application/json' -d '{"storeId":1,"lines":[{"menuItemId":1,"quantity":2,"optionId":101}]}'
같은 명령을 다시 실행하면 같은 응답이 돌아옵니다(주문은 1건). 결제 실패 주입은 paymentToken 으로:
  tok_decline, tok_timeout_approved, tok_timeout_lost, tok_timeout_callbacks, tok_hold, tok_slow_<ms>
MSG
    ;;
  down) docker compose down -v --remove-orphans ;;
  *) echo "사용: demo.sh up|down" >&2; exit 2 ;;
esac
