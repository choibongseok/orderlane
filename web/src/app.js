// 도메인 규칙은 Kotlin(멀티플랫폼) 모듈을 JS 로 컴파일한 orderlane-domain.mjs 가 실행한다. 이 파일은 화면과 안내만 그린다.
import * as domain from './orderlane-domain.mjs';

const SCENARIOS = {
  'happy': { label: '정상 주문', tech: '기준 흐름', desc: '손님이 주문하면 서버가 재고를 빼고, 결제를 승인받고, 주방·알림·정산에 알립니다. 아무 문제가 없을 때의 기준 모습입니다.' },
  'duplicate-request': { label: '같은 주문이 두 번 들어옴', tech: '멱등 키', desc: '응답을 못 받은 앱이 같은 주문을 다시 보냅니다. 서버가 이름표(멱등 키)와 주문 내용을 비교해 답을 정합니다.' },
  'concurrent-order': { label: '마지막 1개를 동시에 주문', tech: '재고 잠금', desc: '여러 명이 같은 순간에 마지막 재고를 주문합니다. 재고를 지키는 방법에 따라 결과가 달라집니다.' },
  'payment-timeout': { label: '결제 응답이 안 옴', tech: '결과 불명', desc: '결제사 응답이 오지 않아 승인됐는지 모릅니다. 서버가 결제사에 다시 물어봐서 결과를 정합니다.' },
  'callbacks': { label: '결제사 알림이 중복·뒤바뀜', tech: '콜백', desc: '결제사가 같은 알림을 두 번 보내거나 오래된 알림을 늦게 보냅니다. 승인된 결제에 거절 알림이 오는 경우도 있습니다.' },
  'consumer-failure': { label: '후속 처리가 실패함', tech: '재시도 · dead-letter', desc: '결제 뒤에 보내는 알림이 실패합니다. 서버가 다시 시도하고, 안 되면 보관함에 넣었다가 재처리합니다.' },
};
const VARIANTS = {
  'duplicate-request': { 'replay': '같은 요청 다시 보내기', 'in-flight': '처리 중에 다시 보내기', 'mismatch': '같은 키로 다른 내용 보내기', 'reclaim': '서버가 죽은 뒤 다시 보내기' },
  'concurrent-order': { 'optimistic': '낙관적 잠금', 'pessimistic': '비관적 잠금', 'naive': '잠금 없음(비교용)' },
  'payment-timeout': { 'approved-by-inquiry': '다시 물어보니 승인', 'declined-not-found': '다시 물어보니 요청이 없음', 'held-then-resolved': '계속 모름 → 보류 후 확정' },
  'consumer-failure': { 'recovers': '재시도로 회복', 'dead-letter': '보관함 → 재처리' },
};
const EASY = {
  'happy:default': '주문은 한 건 만들어졌고, 결제가 승인됐고, 재고가 줄었습니다.',
  'duplicate-request:replay': '주문은 한 건만 만들어졌고, 다시 보낸 요청에는 처음과 같은 응답이 돌아갔습니다.',
  'duplicate-request:in-flight': '처리 중에 온 같은 요청은 "처리 중" 안내(409)를 받았고, 끝난 뒤에는 처음과 같은 응답을 받았습니다.',
  'duplicate-request:mismatch': '같은 키로 내용이 다른 요청은 거절(422)됐고, 주문은 그대로 한 건입니다.',
  'duplicate-request:reclaim': '서버가 죽었다 돌아온 뒤 같은 요청이 와도 새 주문을 만들지 않고, 만들어 둔 주문을 마무리했습니다.',
  'concurrent-order:optimistic': '사려는 사람이 많아도 재고만큼만 팔렸고, 나머지는 품절 안내를 받았습니다.',
  'concurrent-order:pessimistic': '사려는 사람이 많아도 재고만큼만 팔렸고, 나머지는 품절 안내를 받았습니다.',
  'concurrent-order:naive': '잠금 없이 만든 비교용 방식은 재고보다 많이 팔았습니다. 서버에는 이런 방식이 없습니다.',
  'payment-timeout:approved-by-inquiry': '결제 결과를 다시 물어봐서 승인으로 확정했습니다.',
  'payment-timeout:declined-not-found': '결제사에 요청이 도착하지 않았음을 확인해 거절로 확정하고, 잡아 둔 재고를 돌려놨습니다.',
  'payment-timeout:held-then-resolved': '여러 번 물어도 확정하지 못해 "보류"로 두었다가, 나중에 다시 물어봐서 승인으로 확정했습니다.',
  'callbacks:default': '같은 승인 알림은 한 번만 반영됐고, 늦게 온 옛 알림은 무시됐고, 승인 뒤에 온 거절 알림은 반영하지 않고 불일치로 기록했습니다.',
  'consumer-failure:recovers': '알림 전송이 두 번 실패했지만 다시 시도해서 성공했습니다.',
  'consumer-failure:dead-letter': '알림 전송이 계속 실패해 보관함에 두었고, 재처리 명령으로 알림만 다시 처리했습니다. 이미 끝난 처리는 건너뛰었습니다.',
};
const TITLE = {
  'happy:default': '주문 1건이 결제까지 끝났습니다',
  'duplicate-request:replay': '주문은 1건만 만들어졌습니다',
  'duplicate-request:in-flight': '처리 중인 주문은 다시 만들어지지 않았습니다',
  'duplicate-request:mismatch': '내용이 다른 요청은 거절됐습니다',
  'duplicate-request:reclaim': '이어받아서 주문은 1건으로 끝났습니다',
  'concurrent-order:optimistic': '재고만큼만 팔렸습니다',
  'concurrent-order:pessimistic': '재고만큼만 팔렸습니다',
  'concurrent-order:naive': '재고보다 많이 팔렸습니다 (잠금 없는 비교용)',
  'payment-timeout:approved-by-inquiry': '결제사에 다시 물어 결제를 확정했습니다',
  'payment-timeout:declined-not-found': '결제가 안 된 것을 확인하고 재고를 돌려놨습니다',
  'payment-timeout:held-then-resolved': '보류했다가 결제를 확정했습니다',
  'callbacks:default': '같은 알림은 한 번만 반영됐습니다',
  'consumer-failure:recovers': '알림이 재시도 끝에 전송됐습니다',
  'consumer-failure:dead-letter': '실패한 알림만 다시 처리했습니다',
};
const TOUR = [
  { title: '주문 하나가 끝까지 처리되는 모습', text: ['손님이 주문하면 서버는 재고를 빼 두고 결제사에 결제를 요청합니다.', '결제가 승인되면 주방 화면과 알림, 정산 기록에 주문이 전달됩니다. 아무 문제가 없을 때의 모습입니다.'], look: '위쪽 "결과"와 가운데 "진행 순서"', scenario: 'happy', variant: 'default', hl: ['#p-result', '#p-timeline'] },
  { title: '같은 주문이 두 번 들어와도 1건만 만들어집니다', text: ['휴대폰이 응답을 못 받으면 같은 주문을 한 번 더 보내기도 합니다. 주문마다 붙은 이름표(멱등 키)가 같으면 서버는 새로 만들지 않고 처음 응답을 돌려줍니다.', '이름표는 같은데 주문 내용이 다르면 거절합니다. 처음 주문이 아직 처리 중이면 "처리 중"이라고 답합니다.'], look: '"일부러 고장 내 보기"에서 네 가지를 눌러 보세요', scenario: 'duplicate-request', variant: 'replay', hl: ['#p-controls', '#p-timeline'] },
  { title: '마지막 1개를 여러 명이 동시에 주문하면', text: ['한 명만 주문에 성공하고 나머지는 품절 안내를 받습니다. 재고를 지키는 방법은 두 가지(낙관적 잠금, 비관적 잠금)이고 둘 다 그렇게 동작합니다.', '"잠금 없음(비교용)"을 누르면 재고보다 많이 팔리는 경우를 볼 수 있습니다.'], look: '위쪽 "결과"의 "초과 판매" 숫자', scenario: 'concurrent-order', variant: 'optimistic', hl: ['#p-result', '#p-stats', '#p-controls'] },
  { title: '결제 응답이 안 오면 결제사에 다시 물어봅니다', text: ['응답이 없으면 승인됐는지 알 수 없습니다. 서버는 거절도 승인도 하지 않고 "모름"으로 두고, 결제사에 결과를 다시 물어봅니다.', '계속 모르면 "보류"로 두고 계속 다시 물어봅니다. 그동안 재고는 빼 둔 채로 둡니다.'], look: '진행 순서의 상태 표시(UNKNOWN → APPROVED)', scenario: 'payment-timeout', variant: 'approved-by-inquiry', hl: ['#p-timeline'] },
  { title: '결제사 알림이 두 번 오거나 순서가 바뀌어도 한 번만 반영됩니다', text: ['같은 승인 알림은 한 번만 반영하고, 늦게 도착한 오래된 알림은 무시합니다.', '이미 승인된 결제에 "거절" 알림이 오면 반영하지 않고, 나중에 사람이 볼 수 있게 기록해 둡니다.'], look: '위쪽 숫자(반영·무시·거부)와 진행 순서', scenario: 'callbacks', variant: 'default', hl: ['#p-stats', '#p-timeline'] },
  { title: '알림 전송이 실패해도 주문은 사라지지 않습니다', text: ['알림 전송 같은 뒤처리가 실패하면 서버가 몇 번 다시 시도합니다. 그래도 안 되면 실패한 건을 보관함(dead-letter)에 넣고 다음 일을 계속합니다.', '원인을 고치고 재처리 명령을 내리면 실패했던 처리만 다시 실행합니다. 이미 끝난 처리는 건너뜁니다.'], look: '"일부러 고장 내 보기"의 "보관함 → 재처리"', scenario: 'consumer-failure', variant: 'dead-letter', hl: ['#p-controls', '#p-timeline'] },
  { title: '하루 정산과 결제사 거래 내역 대조', text: ['하루치 매출을 매장별로 처음부터 다시 계산하므로 몇 번 돌려도 결과가 같습니다. 결제사가 준 거래 내역 파일과 우리 기록을 맞춰 보면서 빠진 거래나 금액이 다른 거래를 찾고, 같은 거래가 두 번 들어간 것도 찾습니다.', '이 부분은 화면 안의 모의 실행으로는 볼 수 없습니다. 서버를 실제로 띄워 돌려 본 결과를 아래에 적어 두었습니다.'], look: '아래 "서버를 실제로 띄워 확인한 것"의 "정산·대사"', scenario: null, hl: ['#p-e2e'] },
  { title: '이 화면에서 실제 코드인 부분', text: ['주문을 받을지, 결제를 확정할지 정하는 규칙은 서버와 같은 Kotlin 코드를 JavaScript로 바꿔 이 브라우저에서 돌립니다. 서버, DB, 메시지 브로커는 없습니다.', '아래 테스트 결과와 동시 주문 표는 실제로 테스트를 돌려 남긴 파일에서 읽은 값입니다.'], look: '아래 "테스트 실행 결과"와 "동시 주문 테스트 결과"', scenario: null, hl: ['#p-log', '#p-conc'] },
];

const $ = (s) => document.querySelector(s);
const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
const state = { scenario: 'happy', variant: 'default' };

function fmtTime(ms) { return ms < 1000 ? `+${ms}ms` : `+${(ms / 1000).toFixed(ms % 1000 === 0 ? 0 : 1)}s`; }
function el(tag, props = {}, ...kids) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (k === 'class') e.className = v; else if (k === 'text') e.textContent = v; else if (k.startsWith('data-') || k.startsWith('aria-') || k === 'role') e.setAttribute(k, v); else e[k] = v;
  }
  for (const kid of kids) if (kid != null) e.append(kid);
  return e;
}

function radios(container, items, current, onPick, withTech) {
  container.replaceChildren();
  const buttons = Object.entries(items).map(([id, v]) => {
    const label = typeof v === 'string' ? v : v.label;
    const b = el('button', { type: 'button', role: 'radio', 'aria-checked': String(id === current), 'data-id': id, tabIndex: id === current ? 0 : -1 });
    b.append(label);
    if (withTech && v.tech) b.append(el('small', { text: v.tech }));
    b.addEventListener('click', () => onPick(id));
    b.addEventListener('keydown', (ev) => {
      const i = buttons.indexOf(b);
      const next = ev.key === 'ArrowRight' || ev.key === 'ArrowDown' ? buttons[(i + 1) % buttons.length] : ev.key === 'ArrowLeft' || ev.key === 'ArrowUp' ? buttons[(i - 1 + buttons.length) % buttons.length] : null;
      if (next) { ev.preventDefault(); ev.stopPropagation(); next.click(); container.querySelector(`[data-id="${next.dataset.id}"]`)?.focus(); }
    });
    return b;
  });
  container.append(...buttons);
}

const meta = JSON.parse(domain.listScenarios());

function pick(scenario, variant) { state.scenario = scenario; state.variant = variant ?? Object.keys(VARIANTS[scenario] ?? { default: 1 })[0]; render(); }

function render() {
  const sc = SCENARIOS[state.scenario];
  const variants = VARIANTS[state.scenario];
  $('#fail-box').hidden = !variants;
  if (variants) radios($('#variants'), variants, state.variant, (v) => { state.variant = v; render(); $(`#variants [data-id="${v}"]`)?.focus(); });
  radios($('#scenarios'), SCENARIOS, state.scenario, (id) => { pick(id); $(`#scenarios [data-id="${id}"]`)?.focus(); }, true);
  $('#params').hidden = state.scenario !== 'concurrent-order';
  const params = state.scenario === 'concurrent-order' ? `stock=${$('#p-stock').value}&buyers=${$('#p-buyers').value}` : '';
  const result = JSON.parse(domain.runScenario(state.scenario, state.variant, params));

  $('#scenario-desc').textContent = sc.desc;
  $('#scenario-title').textContent = `진행 순서 — ${result.title}`;
  $('#verdict-title').textContent = TITLE[`${state.scenario}:${state.variant}`] ?? (result.verdictOk ? '기대한 대로 처리됐습니다' : '기대와 다른 결과가 나왔습니다');
  $('#easy').textContent = EASY[`${state.scenario}:${state.variant}`] ?? '';
  $('#verdict').textContent = result.verdict;
  $('#p-result').dataset.ok = String(result.verdictOk);
  $('#p-stats').replaceChildren(...result.facts.map((f) => {
    const bad = (f.k === '초과 판매' && f.v !== '0') || (f.k === '거부' && f.v !== '0');
    const good = f.k === '초과 판매' && f.v === '0';
    return el('div', { class: 'stat' + (bad ? ' bad' : good ? ' ok' : '') }, el('span', { class: 'k', text: f.k }), el('span', { class: 'v', text: f.v }));
  }));
  $('#strip').replaceChildren(...result.steps.map((s) => el('span', { 'data-tone': s.tone })));
  $('#timeline').replaceChildren(...result.steps.map((s) => {
    const head = el('div', { class: 'step-head' }, el('span', { class: 'step-time', text: fmtTime(s.atMs) }), el('span', { class: 'pill-actor', text: s.actor }));
    if (s.state) head.append(el('span', { class: 'state-chip', text: s.state }));
    head.append(el('span', { class: 'step-action', text: s.action }));
    return el('li', { 'data-tone': s.tone, 'data-actor': s.actor }, head, el('div', { class: 'step-detail', text: s.detail }));
  }));
  $('#rules tbody').replaceChildren(...result.steps.filter((s) => s.rule).map((s) =>
    el('tr', {}, el('td', { text: fmtTime(s.atMs) }), el('td', { text: s.actor }), el('td', { class: 'rule', text: s.rule }), el('td', { text: s.state ?? '' }))));
}
for (const id of ['#p-stock', '#p-buyers']) $(id).addEventListener('input', render);

async function loadJson(p) { const r = await fetch(p); if (!r.ok) throw new Error(`${p} ${r.status}`); return r.json(); }
const KIND_DESC = {
  unit: '주문·결제 처리 규칙(결제 상태, 중복 요청, 재고, 정산, 대사), 모의 결제사',
  integration: '실제 PostgreSQL·Redis·Redpanda 를 띄워 주문, 결제, 알림 전달, 정산, 재고 동시성',
  e2e: '서버 전체를 띄워 결제사 고장, 동시 주문, 종료 중 요청',
};

async function renderEvidence() {
  const [tests, conc, e2e] = await Promise.all([loadJson('data/test-results.json'), loadJson('data/concurrency.json'), loadJson('data/e2e-scenarios.json')]);
  $('#test-summary tbody').replaceChildren(...['unit', 'integration', 'e2e'].map((k) => {
    const t = tests.totals[k] ?? { passed: 0, failed: 0 };
    return el('tr', {}, el('td', { text: k }), el('td', { text: KIND_DESC[k] }), el('td', { class: 'num ' + (t.failed ? '' : 'good'), text: String(t.passed) }), el('td', { class: 'num ' + (t.failed ? 'bad' : ''), text: String(t.failed) }));
  }));
  $('#test-output').textContent = tests.tests.map((t) => `${t.status === 'passed' ? 'PASS' : 'FAIL'}  ${t.kind.padEnd(11)} ${t.suite} > ${t.name}`).join('\n') +
    `\n\n같은 처리 규칙을 JavaScript(Node)로 바꿔 같은 테스트를 돌린 결과: 통과 ${tests.domainJs.passed}, 실패 ${tests.domainJs.failed}\n집계 시각 ${tests.generatedAt}`;
  $('#conc-buyers').textContent = String(conc.buyersPerRound);
  const rows = [];
  for (const s of conc.strategies) for (const c of s.cases) {
    rows.push(el('tr', {}, el('td', { text: s.strategy === 'optimistic' ? '낙관적(버전 비교)' : '비관적(행 잠금)' }), el('td', { class: 'num', text: String(c.stock) }),
      el('td', { class: 'num', text: String(c.rounds) }), el('td', { class: 'num ' + (c.oversold === 0 ? 'zero' : 'bad'), text: String(c.oversold) }), el('td', { text: c.soldPerRound.join(', ') }), el('td', { text: c.finalStock.join(', ') })));
  }
  $('#conc tbody').replaceChildren(...rows);
  $('#e2e').replaceChildren(...e2e.map((s) => el('li', {}, el('strong', { text: s.scenario }), ...s.observed.map((o) => el('span', { text: o })))));
}

// ---------- 설명 따라가기 ----------
const tourMs = Number(new URLSearchParams(location.search).get('tourms')) || 8000;
const tour = { open: false, i: 0, auto: false, timer: null };
// 첫 방문 여부는 쿠키 하나(ol_tour=seen)로만 기억한다. 식별 정보는 없고, 쿠키를 쓸 수 없으면 매번 자동으로 연다.
function hasSeen() { try { return /(?:^|;\s*)ol_tour=seen(?:;|$)/.test(document.cookie); } catch { return false; } }
function markSeen() { try { document.cookie = 'ol_tour=seen; path=/orderlane/; max-age=15552000; SameSite=Lax'; } catch { /* 쿠키를 쓸 수 없는 환경 */ } }

function highlight(sels) {
  document.querySelectorAll('.tour-hl').forEach((e) => e.classList.remove('tour-hl'));
  for (const sel of sels || []) document.querySelectorAll(sel).forEach((e) => e.classList.add('tour-hl'));
}
function tourTimer() {
  clearTimeout(tour.timer);
  if (!tour.open || !tour.auto || reduced || tour.i >= TOUR.length - 1) return;
  tour.timer = setTimeout(() => tourGo(tour.i + 1), tourMs);
}
function tourGo(i, scroll = true) {
  tour.i = Math.max(0, Math.min(TOUR.length - 1, i));
  const st = TOUR[tour.i];
  if (st.scenario) { if (tour.i === 2) { $('#p-stock').value = '1'; $('#p-buyers').value = '5'; } pick(st.scenario, st.variant); }
  highlight(st.hl);
  $('#tour-prog').textContent = `${tour.i + 1}/${TOUR.length}`;
  $('#tour-prog').setAttribute('aria-label', `${TOUR.length}장 중 ${tour.i + 1}장`);
  $('#tour-title').textContent = st.title;
  $('#tour-text').replaceChildren(...st.text.map((t) => el('p', { text: t })));
  $('#tour-look').textContent = `지금 화면에서 볼 곳: ${st.look}`;
  $('#tour-prev').disabled = tour.i === 0;
  $('#tour-next').textContent = tour.i === TOUR.length - 1 ? '마치기' : '다음';
  if (scroll) document.querySelector('.tour-hl')?.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'start' });
  sheetHeight();
  tourTimer();
}
function sheetHeight() { document.documentElement.style.setProperty('--sheet-h', tour.open ? `${$('#tour').offsetHeight}px` : '0px'); }
function tourOpen(i = tour.i, focus = false, scroll = true) {
  tour.open = true;
  $('#tour').hidden = false; $('#tour-open').hidden = true;
  document.body.classList.add('tour-open');
  tourGo(i, scroll);
  if (focus) $('#tour-title').focus({ preventScroll: true });
}
function tourCollapse() {
  markSeen();
  tour.open = false; clearTimeout(tour.timer);
  $('#tour').hidden = true; $('#tour-open').hidden = false;
  document.body.classList.remove('tour-open');
  document.documentElement.style.setProperty('--sheet-h', '0px');
  highlight([]);
}
function tourFinish() { markSeen(); tour.i = 0; tour.auto = false; $('#tour-auto').setAttribute('aria-pressed', 'false'); $('#tour-auto').textContent = '자동 진행 켜기'; tourCollapse(); $('#tour-open').hidden = true; }

$('#tour-start').addEventListener('click', () => { tour.i = 0; tourOpen(0, true); });
$('#tour-skip').addEventListener('click', () => { tourFinish(); $('#h-scn').scrollIntoView({ behavior: reduced ? 'auto' : 'smooth' }); });
$('#tour-open').addEventListener('click', () => tourOpen(tour.i, true));
$('#tour-collapse').addEventListener('click', () => { tourCollapse(); $('#tour-open').focus(); });
$('#tour-prev').addEventListener('click', () => tourGo(tour.i - 1));
$('#tour-next').addEventListener('click', () => (tour.i === TOUR.length - 1 ? tourFinish() : tourGo(tour.i + 1)));
if (reduced) { $('#tour-auto').disabled = true; $('#tour-keys').textContent = '← → 키로 넘길 수 있습니다. (동작 줄이기 설정이라 자동 진행은 꺼져 있습니다)'; }
$('#tour-auto').addEventListener('click', () => {
  tour.auto = !tour.auto;
  $('#tour-auto').setAttribute('aria-pressed', String(tour.auto));
  $('#tour-auto').textContent = tour.auto ? '자동 진행 끄기' : '자동 진행 켜기';
  tourTimer();
});
document.addEventListener('keydown', (e) => {
  if (!tour.open || e.defaultPrevented || e.altKey || e.ctrlKey || e.metaKey) return;
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)) return;
  if (document.activeElement?.closest('[role="radiogroup"]')) return;
  if (e.key === 'ArrowRight') { e.preventDefault(); tour.i === TOUR.length - 1 ? tourFinish() : tourGo(tour.i + 1); }
  else if (e.key === 'ArrowLeft') { e.preventDefault(); tourGo(tour.i - 1); }
});
new ResizeObserver(sheetHeight).observe($('#tour'));

// ---------- 테마 ----------
const root = document.documentElement;
let saved = null;
try { saved = localStorage.getItem('ol-theme'); } catch { /* 저장소를 못 쓰는 환경 */ }
root.dataset.theme = saved === 'light' ? 'light' : 'dark';
$('#theme').addEventListener('click', () => {
  root.dataset.theme = root.dataset.theme === 'dark' ? 'light' : 'dark';
  try { localStorage.setItem('ol-theme', root.dataset.theme); } catch { /* 무시 */ }
});

render();
if (!hasSeen()) tourOpen(0, false, false);
renderEvidence().then(() => { document.body.dataset.ready = 'true'; }).catch((e) => {
  $('#test-output').textContent = '증거 파일을 읽지 못했습니다: ' + e.message;
  document.body.dataset.ready = 'true';
  document.body.dataset.evidenceError = e.message;
});
