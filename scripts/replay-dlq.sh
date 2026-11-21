#!/usr/bin/env bash
# dead-letter 재처리 명령: 보관된 이벤트를 원래 ID 로 다시 발행한다. 이미 처리한 소비자는 중복으로 건너뛴다.
set -euo pipefail
base="${ORDERLANE_URL:-http://127.0.0.1:58680}"
curl -fsS "$base/admin/dead-letters" -H 'Authorization: Bearer demo-token'; echo
curl -fsS -XPOST "$base/admin/dead-letters/replay" -H 'Authorization: Bearer demo-token'; echo
