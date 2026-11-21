import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  reporter: [['list'], ['json', { outputFile: '../.test-results/web.json' }]],
  use: { baseURL: 'http://127.0.0.1:4273/orderlane/', channel: process.env.PW_CHANNEL === undefined ? 'chrome' : process.env.PW_CHANNEL || undefined, trace: 'off' },
  webServer: { command: 'node serve.mjs', url: 'http://127.0.0.1:4273/orderlane/', reuseExistingServer: false, timeout: 20_000 },
});
