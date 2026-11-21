// 테스트 실행 결과 파일(JUnit XML, 동시성 결과, e2e 관찰 기록)을 docs/evidence/ 로 모은다. 사이트는 이 파일을 그대로 보여 준다.
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const suites = [
  { dir: 'domain/build/test-results/jvmTest', kind: 'unit', src: 'domain/src/commonTest', js: false },
  { dir: 'gateway/build/test-results/test', kind: 'unit', src: 'gateway/src/test' },
  { dir: 'app/build/test-results/test', kind: null, src: 'app/src/test' },
  { dir: 'e2e/build/test-results/test', kind: 'e2e', src: 'e2e/src/test' },
];

function xmls(dir) { return existsSync(dir) ? readdirSync(dir).filter((f) => f.endsWith('.xml')).map((f) => join(dir, f)) : []; }
function parse(file) {
  const text = readFileSync(file, 'utf8');
  const out = [];
  for (const m of text.matchAll(/<testcase name="([^"]+)" classname="([^"]+)" time="([^"]+)"(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const body = m[5] ?? '';
    const status = /<failure|<error/.test(body) ? 'failed' : /<skipped/.test(body) ? 'skipped' : 'passed';
    out.push({ name: m[1].replace(/\[[^\]]*\]$/, '').replace(/\(\)$/, ''), cls: m[2], ms: Math.round(Number(m[3]) * 1000), status });
  }
  return out;
}

const tests = [];
for (const s of suites) {
  for (const f of xmls(s.dir)) for (const t of parse(f)) {
    const kind = s.kind ?? (t.cls.endsWith('IT') ? 'integration' : 'unit');
    tests.push({ kind, suite: t.cls.split('.').pop(), name: t.name, status: t.status, ms: t.ms });
  }
}
const js = xmls('domain/build/test-results/jsNodeTest').flatMap(parse);
const totals = {};
for (const t of tests) { totals[t.kind] ??= { passed: 0, failed: 0 }; totals[t.kind][t.status === 'passed' ? 'passed' : 'failed']++; }

mkdirSync('docs/evidence', { recursive: true });
const order = { unit: 0, integration: 1, e2e: 2 };
tests.sort((a, b) => order[a.kind] - order[b.kind] || a.suite.localeCompare(b.suite) || a.name.localeCompare(b.name));
writeFileSync('docs/evidence/test-results.json', JSON.stringify({
  generatedAt: new Date().toISOString().replace(/\.\d+Z$/, 'Z'),
  totals,
  domainJs: { passed: js.filter((t) => t.status === 'passed').length, failed: js.filter((t) => t.status !== 'passed').length },
  tests,
}, null, 2));

const strategies = ['optimistic', 'pessimistic'].map((n) => {
  const p = `build/evidence/concurrency-${n}.json`;
  if (!existsSync(p)) throw new Error(`동시성 결과 파일이 없음: ${p}`);
  return JSON.parse(readFileSync(p, 'utf8'));
});
writeFileSync('docs/evidence/concurrency.json', JSON.stringify({ buyersPerRound: strategies[0].buyersPerRound, strategies }, null, 2));

const e2e = 'build/evidence/e2e-scenarios.json';
if (!existsSync(e2e)) throw new Error(`e2e 관찰 기록이 없음: ${e2e}`);
writeFileSync('docs/evidence/e2e-scenarios.json', readFileSync(e2e, 'utf8'));
console.log(`증거 수집: ${JSON.stringify(totals)}, 도메인 JS ${js.length}건, 동시성 ${strategies.map((s) => s.strategy + ' 초과 판매 ' + s.totalOversold).join(', ')}`);
