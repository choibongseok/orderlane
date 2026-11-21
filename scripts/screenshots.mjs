// README 용 스크린샷(라이트·다크·모바일). 사이트를 web/dist 로 빌드한 뒤 로컬 Chrome 으로 찍는다: node scripts/screenshots.mjs
import { createRequire } from 'node:module';
import { spawn } from 'node:child_process';
import { mkdirSync } from 'node:fs';
const require = createRequire(new URL('../web/package.json', import.meta.url));
const { chromium } = require('@playwright/test');

mkdirSync('docs', { recursive: true });
const server = spawn('node', ['serve.mjs'], { cwd: 'web', env: { ...process.env, PORT: '4274' }, stdio: 'ignore' });
await new Promise((r) => setTimeout(r, 800));
const browser = await chromium.launch({ channel: 'chrome' });
const errors = [];
async function shot(name, { scheme, width, height, full }, prepare) {
  const ctx = await browser.newContext({ colorScheme: scheme, viewport: { width, height }, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  page.on('console', (m) => { if (m.type() === 'error') errors.push(`${name}: ${m.text()}`); });
  page.on('pageerror', (e) => errors.push(`${name}: ${e}`));
  await page.goto('http://127.0.0.1:4274/orderlane/');
  await page.waitForSelector('body[data-ready="true"]');
  if (prepare) await prepare(page);
  await page.screenshot({ path: `docs/${name}.png`, fullPage: full });
  await ctx.close();
}
const concurrent = async (p) => { await p.getByRole('radio', { name: '동시 주문' }).click(); await p.getByRole('radio', { name: '잠금 없음(비교용)' }).click(); };
await shot('demo-desktop-light', { scheme: 'light', width: 900, height: 1500 }, async (p) => { await p.getByRole('radio', { name: '결제 시간 초과' }).click(); });
await shot('demo-desktop-dark', { scheme: 'dark', width: 900, height: 1500 }, concurrent);
await shot('demo-mobile', { scheme: 'light', width: 390, height: 844 }, async (p) => { await p.getByRole('radio', { name: '중복 요청' }).click(); });
await browser.close();
server.kill();
if (errors.length) { console.error('콘솔 오류:\n' + errors.join('\n')); process.exit(1); }
console.log('스크린샷 3장 저장, 콘솔 오류 0');
