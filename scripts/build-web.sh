#!/usr/bin/env bash
# 데모 사이트(정적)를 web/dist 에 만든다. 판정 규칙은 Kotlin 도메인 모듈을 JavaScript 로 컴파일한 산출물이 실행한다(규칙을 JS 로 다시 쓰지 않는다).
# 모든 URL 이 상대 경로라 GitHub Pages 프로젝트 경로(/orderlane/)에서도, 로컬 어디서든 같게 동작한다.
set -euo pipefail
cd "$(dirname "$0")/.."
for f in docs/evidence/test-results.json docs/evidence/concurrency.json docs/evidence/e2e-scenarios.json; do
  [ -s "$f" ] || { echo "증거 파일이 없음: $f (make verify 를 먼저 실행)" >&2; exit 1; }
done
bash scripts/gradle.sh :domain:jsProductionExecutableCompileSync -q
js=domain/build/compileSync/js/main/productionExecutable/kotlin
[ -d web/dist ] && rm -r web/dist
mkdir -p web/dist/data
cp web/src/index.html web/src/style.css web/src/app.js web/dist/
# 도메인 모듈과 Kotlin 표준 라이브러리 산출물(소스맵 제외)
for f in "$js"/*.mjs; do cp "$f" web/dist/; done
cp docs/evidence/test-results.json docs/evidence/concurrency.json docs/evidence/e2e-scenarios.json web/dist/data/
touch web/dist/.nojekyll
if grep -nE '(src|href)="/[^/]' web/dist/index.html; then echo "절대 경로 참조가 있음(Pages 하위 경로에서 깨짐)" >&2; exit 1; fi
if grep -nE 'https?://' web/dist/index.html web/dist/app.js web/dist/style.css; then echo "외부 URL 참조가 있음" >&2; exit 1; fi
echo "web/dist 생성: $(du -sh web/dist | cut -f1) (도메인 모듈 $(du -h web/dist/orderlane-domain.mjs | cut -f1))"
