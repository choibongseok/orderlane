// docs/TEST_CASES.md 와 실제 테스트 결과(JUnit XML, Playwright json)를 대조한다.
// 행 수 = 실행된 테스트 수, 모든 행이 실제로 통과한 테스트 하나와 1:1, 종류 열이 파일 위치와 일치, 요약 표의 개수가 맞는지,
// 도메인 테스트가 JVM 과 JavaScript 에서 같은 이름으로 모두 통과했는지 본다.
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const problems = [];
const doc = readFileSync('docs/TEST_CASES.md', 'utf8');
const actual = new Map(); // "파일::테스트" -> status

function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(join(dir, e.name)) : [join(dir, e.name)]));
}
// 클래스 이름 -> 소스 파일
const classFile = new Map();
for (const root of ['domain/src/commonTest', 'gateway/src/test', 'app/src/test', 'e2e/src/test']) {
  for (const f of walk(root).filter((x) => x.endsWith('.kt'))) {
    for (const m of readFileSync(f, 'utf8').matchAll(/^(?:abstract )?class (\w+)/gm)) classFile.set(m[1], f);
  }
}

function load(dir) {
  const r = new Map();
  if (!existsSync(dir)) { problems.push(`${dir} 가 없음: make verify 로 테스트를 먼저 실행해야 함`); return r; }
  for (const f of readdirSync(dir).filter((x) => x.endsWith('.xml'))) {
    const text = readFileSync(join(dir, f), 'utf8');
    for (const m of text.matchAll(/<testcase name="([^"]+)" classname="([^"]+)" time="[^"]+"(\/>|>([\s\S]*?)<\/testcase>)/g)) {
      const name = m[1].replace(/\[[^\]]*\]$/, '').replace(/\(\)$/, '');
      const cls = m[2].split('.').pop();
      const file = classFile.get(cls);
      if (!file) { problems.push(`${m[2]}: 소스에서 클래스를 찾지 못함`); continue; }
      const status = /<failure|<error/.test(m[4] ?? '') ? 'failed' : /<skipped/.test(m[4] ?? '') ? 'skipped' : 'passed';
      r.set(`${file}::${name}`, status);
    }
  }
  return r;
}

const jvm = load('domain/build/test-results/jvmTest');
const js = load('domain/build/test-results/jsNodeTest');
for (const [k, v] of jvm) {
  if (js.get(k) !== v) problems.push(`도메인 테스트가 JS 에서 다르게 끝남: ${k} jvm=${v} js=${js.get(k)}`);
}
for (const k of js.keys()) if (!jvm.has(k)) problems.push(`JS 에만 있는 도메인 테스트: ${k}`);
for (const dir of ['gateway/build/test-results/test', 'app/build/test-results/test', 'e2e/build/test-results/test']) for (const [k, v] of load(dir)) actual.set(k, v);
for (const [k, v] of jvm) actual.set(k, v);

// 브라우저 테스트(Playwright)
const pw = '.test-results/web.json';
if (existsSync(pw)) {
  const w = (s) => {
    for (const sp of s.specs ?? []) actual.set(`web/e2e/${sp.file}::${sp.title}`, sp.ok ? 'passed' : 'failed');
    for (const sub of s.suites ?? []) w(sub);
  };
  for (const s of JSON.parse(readFileSync(pw, 'utf8')).suites ?? []) w(s);
} else problems.push(`${pw} 가 없음: 브라우저 테스트 결과가 필요함`);

const rows = doc.split('\n').filter((l) => /^\| [A-Z0-9]+-\d{3} \|/.test(l)).map((l) => l.split(' | ').map((c) => c.replace(/^\| /, '').replace(/ \|$/, '').trim()));
const kindOf = (f) => (f.startsWith('app/src/test/') && /IT\.kt$/.test(f) ? 'integration' : f.startsWith('e2e/') || f.startsWith('web/e2e/') ? 'e2e' : 'unit');
const perKind = { unit: 0, integration: 0, e2e: 0 };
const ids = new Set();
const mapped = new Set();
for (const cells of rows) {
  if (cells.length !== 7) { problems.push(`열 수가 7 이 아님: ${cells[0]}`); continue; }
  const [id, , , , , ref, kind] = cells;
  if (ids.has(id)) problems.push(`ID 중복: ${id}`);
  ids.add(id);
  const key = ref.replace(/^`|`$/g, '');
  if (!actual.has(key)) problems.push(`${id}: 실제 테스트 없음 -> ${key}`);
  else if (actual.get(key) !== 'passed') problems.push(`${id}: 통과하지 않음(${actual.get(key)}) -> ${key}`);
  if (mapped.has(key)) problems.push(`${id}: 같은 테스트가 두 행에 매핑됨 -> ${key}`);
  mapped.add(key);
  const k = kindOf(key.split('::')[0]);
  if (k !== kind) problems.push(`${id}: 종류가 ${kind} 인데 파일은 ${k}`);
  perKind[k]++;
}
for (const key of actual.keys()) if (!mapped.has(key)) problems.push(`문서에 없는 테스트: ${key}`);
if (rows.length !== actual.size) problems.push(`문서 행 ${rows.length} != 실행된 테스트 ${actual.size}`);

const total = Number(/\*\*합계\*\* \| \*\*(\d+)\*\*/.exec(doc)?.[1]);
if (total !== actual.size) problems.push(`요약 표의 합계 ${total} != 실행된 테스트 ${actual.size}`);
for (const k of ['unit', 'integration', 'e2e']) {
  const m = new RegExp(`\\| ${k} [^|]*\\| (\\d+) \\|`).exec(doc);
  if (!m || Number(m[1]) !== perKind[k]) problems.push(`요약 표의 ${k} 개수 ${m?.[1]} != ${perKind[k]}`);
}

if (problems.length) {
  console.error(`테스트 케이스 문서 대조 실패 (${problems.length}건)`);
  for (const p of problems.slice(0, 40)) console.error(' - ' + p);
  process.exit(1);
}
console.log(`테스트 케이스 문서 대조 통과: 문서 ${rows.length}행 = 실행 ${actual.size}건 (unit ${perKind.unit}, integration ${perKind.integration}, e2e ${perKind.e2e}), 도메인 테스트 ${jvm.size}건은 JVM·JS 양쪽에서 같은 결과`);
