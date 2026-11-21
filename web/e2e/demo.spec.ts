import { expect, test, type Page } from '@playwright/test';

async function open(page: Page, query = '', firstVisit = false) {
  // 기본은 "이미 본 방문자"(쿠키 있음)로 연다. 첫 방문 동작을 시험할 때만 firstVisit 를 켠다.
  if (!firstVisit) await page.context().addCookies([{ name: 'ol_tour', value: 'seen', url: 'http://127.0.0.1:4273/orderlane/' }]);
  const errors: string[] = [];
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', (e) => errors.push(String(e)));
  await page.goto('./' + query);
  await expect(page.locator('body[data-ready="true"]')).toBeVisible({ timeout: 15_000 });
  return errors;
}
const scenario = (page: Page, name: string) => page.locator('#scenarios').getByRole('radio', { name }).click();
const variant = (page: Page, name: string) => page.locator('#variants').getByRole('radio', { name }).click();
const fact = (page: Page, key: string) => page.locator('#p-stats .stat').filter({ has: page.locator('.k', { hasText: new RegExp(`^${key}$`) }) }).locator('.v');

test('도메인 엔진(Kotlin→JS)이 뜨고 첫 화면 문장과 기본 결과가 나오며 콘솔 오류가 없다', async ({ page }) => {
  const errors = await open(page);
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  await expect(page.locator('#banner')).toContainText('실제 서버와 같은 코드');
  await expect(page.locator('#h-hero')).toHaveText('주문·결제 서버 모의 테스트');
  await expect(page.locator('.lead')).toContainText('매장 주문을 받는 서버를 Kotlin·Spring Boot로 만들었습니다');
  await expect(page).toHaveTitle('주문·결제 서버 모의 테스트 — Orderlane');
  await expect(page.locator('#verdict-title')).toHaveText('주문 1건이 결제까지 끝났습니다');
  await expect(page.locator('#easy')).toContainText('주문은 한 건 만들어졌고');
  await expect(page.locator('#timeline li .state-chip', { hasText: 'APPROVED' })).toBeVisible();
  await expect(page.locator('#rules tbody tr').first()).toContainText('Idempotency.decide');
  expect(errors).toEqual([]);
});

test('같은 주문이 두 번: 재전송은 같은 응답, 처리 중은 충돌, 다른 내용은 거부, 죽은 뒤에는 이어받는다', async ({ page }) => {
  await open(page);
  await scenario(page, /같은 주문이 두 번/);
  await expect(page.locator('#easy')).toContainText('주문은 한 건만 만들어졌고');
  await expect(page.locator('#timeline')).toContainText('Replay');
  await variant(page, '처리 중에 다시 보내기');
  await expect(page.locator('#timeline')).toContainText('409 Conflict');
  await variant(page, '같은 키로 다른 내용 보내기');
  await expect(page.locator('#timeline')).toContainText('422 거부');
  await variant(page, '서버가 죽은 뒤 다시 보내기');
  await expect(page.locator('#timeline')).toContainText('Reclaim');
  await expect(page.locator('#easy')).toContainText('새 주문을 만들지 않고');
});

test('마지막 1개 동시 주문: 두 잠금 방식은 초과 판매가 없고 잠금 없는 비교용은 초과 판매가 난다', async ({ page }) => {
  await open(page);
  await scenario(page, /마지막 1개/);
  await expect(page.locator('#params')).toBeVisible();
  await expect(page.locator('#verdict-title')).toHaveText('재고만큼만 팔렸습니다');
  await expect(fact(page, '초과 판매')).toHaveText('0');
  await expect(page.locator('#timeline')).toContainText('버전 불일치: 재시도');
  await variant(page, '비관적 잠금');
  await expect(page.locator('#verdict-title')).toHaveText('재고만큼만 팔렸습니다');
  await variant(page, '잠금 없음(비교용)');
  await expect(page.locator('#verdict-title')).toContainText('재고보다 많이 팔렸습니다');
  await expect(fact(page, '초과 판매')).toHaveText('4');
  await page.locator('#p-stock').fill('3');
  await page.locator('#p-buyers').fill('10');
  await expect(fact(page, '초과 판매')).toHaveText('7');
  await variant(page, '낙관적 잠금');
  await expect(fact(page, '판매')).toHaveText('3');
});

test('결제 응답이 안 옴: 다시 물어봐서 승인·거절을 확정하고, 확정 못 하면 보류했다가 확정한다', async ({ page }) => {
  await open(page);
  await scenario(page, /결제 응답이 안 옴/);
  await expect(page.locator('#easy')).toContainText('승인으로 확정');
  await expect(page.locator('#timeline')).toContainText('UNKNOWN');
  await variant(page, '다시 물어보니 요청이 없음');
  await expect(page.locator('#verdict')).toContainText('재고 반환');
  await variant(page, '계속 모름 → 보류 후 확정');
  await expect(page.locator('#timeline li .state-chip', { hasText: 'HELD' }).first()).toBeVisible();
  await expect(page.locator('#verdict')).toContainText('결제 APPROVED');
});

test('결제사 알림이 중복·뒤바뀜: 한 번만 반영하고 충돌은 불일치로 남긴다', async ({ page }) => {
  await open(page);
  await scenario(page, /결제사 알림/);
  await expect(fact(page, '반영')).toHaveText('1');
  await expect(fact(page, '무시')).toHaveText('2');
  await expect(fact(page, '거부')).toHaveText('1');
  await expect(page.locator('#rules')).toContainText('PaymentMachine.on');
  await expect(page.locator('#timeline')).toContainText('conflict');
});

test('후속 처리 실패: 재시도로 회복하거나 보관함으로 간 뒤 재처리되고 다른 소비자는 중복을 건너뛴다', async ({ page }) => {
  await open(page);
  await scenario(page, /후속 처리가 실패/);
  await expect(page.locator('#timeline')).toContainText('재시도 예약');
  await expect(page.locator('#timeline')).not.toContainText('dead-letter 보관');
  await variant(page, '보관함 → 재처리');
  await expect(page.locator('#timeline li', { hasText: '이미 처리한 이벤트 ID 라 건너뜀' })).toHaveCount(2);
  await expect(page.locator('#verdict')).toContainText('dead-letter 보관 → 재처리');
});

test('실제 테스트 결과·동시성 반복 결과·e2e 시나리오가 결과 파일에서 읽혀 표시된다', async ({ page }) => {
  await open(page);
  await expect(page.locator('body')).not.toHaveAttribute('data-evidence-error', /.+/);
  await expect(page.locator('#test-summary tbody tr')).toHaveCount(3);
  for (const k of ['unit', 'integration', 'e2e']) {
    const n = Number(await page.locator('#test-summary tr', { hasText: new RegExp(`^${k}`) }).locator('td').nth(2).innerText());
    expect(n).toBeGreaterThan(5);
  }
  await expect(page.locator('#test-summary tbody tr td:nth-child(4)').first()).toHaveText('0');
  await page.locator('#test-list summary').click();
  await expect(page.locator('#test-output')).toContainText('PASS  unit');
  await expect(page.locator('#test-output')).toContainText('같은 테스트를 돌린 결과');
  await expect(page.locator('#conc tbody tr')).toHaveCount(4);
  for (let i = 0; i < 4; i++) await expect(page.locator('#conc tbody tr').nth(i).locator('td').nth(3)).toHaveText('0');
  expect(await page.locator('#e2e li').count()).toBeGreaterThanOrEqual(9);
  await expect(page.locator('#e2e')).toContainText('SIGTERM');
});

test('다크가 기본이고 라이트로 전환할 수 있다', async ({ page }) => {
  await open(page);
  const bg = () => page.evaluate(() => getComputedStyle(document.body).backgroundColor);
  expect(await bg()).toBe('rgb(17, 18, 23)');
  await page.locator('#theme').click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  expect(await bg()).toBe('rgb(244, 245, 245)');
});

test('글자 대비가 라이트·다크 모두 4.5:1 이상이다', async ({ page }) => {
  await open(page);
  const measure = () => page.evaluate(() => {
    const parse = (c: string) => c.match(/[\d.]+/g)!.slice(0, 3).map(Number);
    const lum = ([r, g, b]: number[]) => { const f = (v: number) => { v /= 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; }; return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b); };
    const ratio = (a: string, b: string) => { const [x, y] = [lum(parse(a)), lum(parse(b))].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05); };
    const bgOf = (e: Element) => { for (let n: Element | null = e; n; n = n.parentElement) { const c = getComputedStyle(n).backgroundColor; if (c !== 'rgba(0, 0, 0, 0)') return c; } return 'rgb(255,255,255)'; };
    const pairs: Record<string, number> = {};
    for (const [name, sel] of [['본문', '.easy'], ['보조 글자', '.hint'], ['패널 제목', '.ptitle'], ['결과 제목', '.what'], ['기본 버튼', '.primary'], ['단계 시간', '.step-time'], ['표 머리글', 'th'], ['규칙 코드', 'td.rule']] as const) {
      const e = document.querySelector(sel)!;
      pairs[name] = ratio(getComputedStyle(e).color, bgOf(e));
    }
    return pairs;
  });
  for (const theme of ['dark', 'light']) {
    if (theme === 'light') await page.locator('#theme').click();
    const r = await measure();
    for (const [name, v] of Object.entries(r)) expect(v, `${theme} ${name} ${v.toFixed(2)}`).toBeGreaterThanOrEqual(4.5);
  }
});

test('390px 폭에서 가로 스크롤 없이 1열로 보인다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await open(page);
  const over = () => page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(await over()).toBeLessThanOrEqual(0);
  await scenario(page, /마지막 1개/);
  await expect(page.locator('#timeline li').first()).toBeVisible();
  expect(await over()).toBeLessThanOrEqual(0);
  const xs = await page.locator('#p-result, #p-timeline').evaluateAll((es) => es.map((e) => Math.round(e.getBoundingClientRect().left)));
  expect(new Set(xs).size).toBe(1);
});

test('키보드로 상황을 바꾸고 초점이 보인다', async ({ page }) => {
  await open(page);
  const first = page.locator('#scenarios button[aria-checked="true"]');
  await first.focus();
  const outline = await first.evaluate((e) => getComputedStyle(e).outlineStyle);
  expect(outline).toBe('solid');
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#scenarios button[aria-checked="true"]')).toContainText('같은 주문이 두 번');
  await expect(page.locator('#scenarios button:focus')).toContainText('같은 주문이 두 번');
  await expect(page.locator('#scenario-title')).toContainText('같은 키');
});

test('외부 요청 없이 같은 출처의 파일만 불러온다', async ({ page }) => {
  const urls: string[] = [];
  page.on('request', (r) => urls.push(r.url()));
  await open(page);
  expect(urls.length).toBeGreaterThan(3);
  for (const u of urls) expect(u.startsWith('http://127.0.0.1:4273/orderlane/') || u.startsWith('data:')).toBe(true);
});

test('설명 따라가기: 시작하면 1/8 장이 열리고, 장을 넘기면 시나리오가 자동 선택되고 볼 패널이 강조된다', async ({ page }) => {
  await open(page);
  await expect(page.locator('#tour')).toBeHidden();
  await page.locator('#tour-start').click();
  await expect(page.locator('#tour')).toBeVisible();
  await expect(page.locator('#tour-prog')).toHaveText('1/8');
  await expect(page.locator('#tour-title')).toBeFocused();
  await expect(page.locator('#p-result')).toHaveClass(/tour-hl/);
  await page.locator('#tour-next').click();
  await expect(page.locator('#tour-prog')).toHaveText('2/8');
  await expect(page.locator('#scenarios button[aria-checked="true"]')).toContainText('같은 주문이 두 번');
  await expect(page.locator('#p-controls')).toHaveClass(/tour-hl/);
  await expect(page.locator('#tour-look')).toContainText('지금 화면에서 볼 곳');
  await page.locator('#tour-next').click();
  await expect(page.locator('#scenarios button[aria-checked="true"]')).toContainText('마지막 1개');
  await expect(fact(page, '초과 판매')).toHaveText('0');
  for (let i = 0; i < 4; i++) await page.locator('#tour-next').click();
  await expect(page.locator('#tour-prog')).toHaveText('7/8');
  await expect(page.locator('#p-e2e')).toHaveClass(/tour-hl/);
  await page.locator('#tour-next').click();
  await expect(page.locator('#tour-title')).toContainText('실제 코드인 부분');
  await expect(page.locator('#tour-next')).toHaveText('마치기');
  await page.locator('#tour-next').click();
  await expect(page.locator('#tour')).toBeHidden();
  await expect(page.locator('.tour-hl')).toHaveCount(0);
});

test('설명 따라가기: ← → 키로 넘기고, 접어도 여는 버튼이 보이며 다시 열면 이어진다', async ({ page }) => {
  await open(page);
  await page.locator('#tour-start').click();
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#tour-prog')).toHaveText('2/8');
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowLeft');
  await expect(page.locator('#tour-prog')).toHaveText('2/8');
  await page.locator('#tour-collapse').click();
  await expect(page.locator('#tour')).toBeHidden();
  await expect(page.locator('#tour-open')).toBeVisible();
  await expect(page.locator('#tour-open')).toBeFocused();
  await page.locator('#tour-open').click();
  await expect(page.locator('#tour-prog')).toHaveText('2/8');
  await expect(page.locator('#tour-open')).toBeHidden();
});

test('설명 따라가기: 자동 진행을 켜면 장이 저절로 넘어가고 끄면 멈춘다', async ({ page }) => {
  await open(page, '?tourms=600');
  await page.locator('#tour-start').click();
  await page.locator('#tour-auto').click();
  await expect(page.locator('#tour-auto')).toHaveAttribute('aria-pressed', 'true');
  await expect(page.locator('#tour-prog')).not.toHaveText('1/8', { timeout: 5000 });
  await page.locator('#tour-auto').click();
  const at = await page.locator('#tour-prog').innerText();
  await page.waitForTimeout(1500);
  await expect(page.locator('#tour-prog')).toHaveText(at);
});

test('설명 따라가기: 390px 에서 하단 시트가 강조된 패널을 가리지 않는다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await open(page);
  await page.locator('#tour-start').click();
  for (let i = 0; i < 8; i++) {
    await page.waitForTimeout(700); // 부드러운 스크롤이 끝나길 기다린다
    const g = await page.evaluate(() => {
      const sheet = document.querySelector('#tour')!.getBoundingClientRect();
      const hl = [...document.querySelectorAll('.tour-hl')].map((e) => e.getBoundingClientRect());
      return { sheetTop: sheet.top, sheetH: sheet.height, vh: innerHeight, firstTop: hl[0].top, firstVisibleBottom: Math.min(hl[0].bottom, innerHeight), pad: parseFloat(getComputedStyle(document.body).paddingBottom) };
    });
    expect(g.sheetH).toBeLessThanOrEqual(g.vh * 0.46 + 1);
    expect(g.pad).toBeGreaterThanOrEqual(g.sheetH - 1); // 본문 끝이 시트에 가리지 않게 여백이 있다
    expect(g.firstTop).toBeLessThan(g.sheetTop - 40); // 강조 패널의 시작이 시트 위쪽에 보인다
    if (i < 7) await page.locator('#tour-next').click();
  }
});

test('첫 방문: 쿠키가 없으면 도메인 엔진이 준비된 뒤 설명이 1/8 장부터 자동으로 열리고 초점을 빼앗지 않는다', async ({ page }) => {
  const errors = await open(page, '', true);
  await expect(page.locator('#tour')).toBeVisible();
  await expect(page.locator('#tour-prog')).toHaveText('1/8');
  await expect(page.locator('#tour-title')).not.toBeFocused();
  await expect(page.locator('#p-result')).toHaveClass(/tour-hl/);
  await expect(page.locator('footer')).toContainText('쿠키는');
  expect((await page.context().cookies()).filter((c) => c.name === 'ol_tour')).toHaveLength(0); // 자동으로 열린 것만으로는 "봤다"고 기록하지 않는다
  expect(errors).toEqual([]);
});

test('첫 방문: 접은 뒤 새로고침하면 열리지 않고, 설명 열기 버튼으로 다시 열 수 있다', async ({ page }) => {
  await open(page, '', true);
  await page.locator('#tour-collapse').click();
  const cookie = (await page.context().cookies()).find((c) => c.name === 'ol_tour')!;
  expect(cookie.value).toBe('seen');
  expect(cookie.path).toBe('/orderlane/');
  expect(cookie.sameSite).toBe('Lax');
  expect(cookie.expires - Date.now() / 1000).toBeGreaterThan(170 * 86400);
  await page.reload();
  await expect(page.locator('body[data-ready="true"]')).toBeVisible();
  await expect(page.locator('#tour')).toBeHidden();
  await expect(page.locator('#tour-start')).toBeVisible();
  await page.locator('#tour-start').click();
  await expect(page.locator('#tour-prog')).toHaveText('1/8');
});

test('첫 방문: 끝까지 본 뒤 새로고침하면 열리지 않고, 다시 열기 버튼이 있다', async ({ page }) => {
  await open(page, '', true);
  for (let i = 0; i < 7; i++) await page.locator('#tour-next').click();
  await page.locator('#tour-next').click(); // 마치기
  await expect(page.locator('#tour')).toBeHidden();
  await page.reload();
  await expect(page.locator('body[data-ready="true"]')).toBeVisible();
  await expect(page.locator('#tour')).toBeHidden();
  await page.locator('#tour-start').click();
  await expect(page.locator('#tour')).toBeVisible();
});

test('쿠키를 쓸 수 없으면 오류 없이 매번 자동으로 열린다', async ({ page }) => {
  await page.addInitScript(() => { Object.defineProperty(Document.prototype, 'cookie', { get: () => '', set: () => { throw new Error('blocked'); } }); });
  const errors = await open(page, '', true);
  await expect(page.locator('#tour')).toBeVisible();
  await page.locator('#tour-collapse').click();
  await page.reload();
  await expect(page.locator('body[data-ready="true"]')).toBeVisible();
  await expect(page.locator('#tour')).toBeVisible();
  expect(errors).toEqual([]);
});

test('390px 에서 자동으로 열린 시트가 첫 화면 문장을 가리지 않는다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await open(page, '', true);
  await expect(page.locator('#tour')).toBeVisible();
  const g = await page.evaluate(() => ({ h1Bottom: document.querySelector('#h-hero')!.getBoundingClientRect().bottom, sheetTop: document.querySelector('#tour')!.getBoundingClientRect().top, scrollY }));
  expect(g.scrollY).toBe(0);
  expect(g.h1Bottom).toBeLessThanOrEqual(g.sheetTop);
});
