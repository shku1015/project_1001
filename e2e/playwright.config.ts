import { defineConfig, devices } from '@playwright/test'

/**
 * 세 프론트 공통 E2E (docs/08-architecture.md 2.2).
 * 같은 시나리오를 접두어만 바꿔 세 번 실행한다: ① /react ② /jsp ③ /ssr
 * 실행: 저장소 루트에서 ./verify.sh e2e (서버를 띄우고 이 테스트를 돌린다)
 */
export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup.ts',
  // 같은 관리자 계정의 동시 로그인을 막는 규칙(NF-LG-02)이 있으므로 한 번에 하나씩 실행한다
  workers: 1,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:18080',
    locale: 'ko-KR',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'react', metadata: { prefix: '/react' }, use: { ...devices['Desktop Chrome'] } },
    { name: 'jsp', metadata: { prefix: '/jsp' }, use: { ...devices['Desktop Chrome'] } },
    { name: 'ssr', metadata: { prefix: '/ssr' }, use: { ...devices['Desktop Chrome'] } },
  ],
})
