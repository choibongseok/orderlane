// README·점검용 스크린샷. 먼저 `make web` 으로 web/dist 를 만든 뒤 로컬 Chrome 으로 찍는다: node scripts/screenshots.mjs
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
async function shot(name, { theme, width, height, full = false, seen = true }, prepare) {
  const ctx = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: 1 });
  if (seen) await ctx.addCookies([{ name: 'ol_tour', value: 'seen', url: 'http://127.0.0.1:4274/orderlane/' }]);
  const page = await ctx.newPage();
  page.on('console', (m) => { if (m.type() === 'error') errors.push(`${name}: ${m.text()}`); });
  page.on('pageerror', (e) => errors.push(`${name}: ${e}`));
  await page.goto('http://127.0.0.1:4274/orderlane/');
  await page.waitForSelector('body[data-ready="true"]');
  if (theme === 'light') await page.locator('#theme').click();
  if (prepare) await prepare(page);
  await page.waitForTimeout(700);
  if (full) await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: `docs/${name}.png`, fullPage: full });
  await ctx.close();
}
const tourAt = (n) => async (p) => { await p.locator('#tour-start').click(); for (let i = 1; i < n; i++) await p.locator('#tour-next').click(); };
await shot('demo-desktop-dark', { theme: 'dark', width: 1440, height: 1000, full: true }, tourAt(3));
await shot('demo-desktop-light', { theme: 'light', width: 1440, height: 1000, full: true }, tourAt(4));
await shot('demo-tablet', { theme: 'dark', width: 1024, height: 900, full: true }, tourAt(2));
await shot('demo-mobile', { theme: 'dark', width: 390, height: 844 }, tourAt(3));
await shot('demo-mobile-first-visit', { theme: 'dark', width: 390, height: 844, seen: false });
await browser.close();
server.kill();
if (errors.length) { console.error('콘솔 오류:\n' + errors.join('\n')); process.exit(1); }
console.log('스크린샷 5장 저장, 콘솔 오류 0');
