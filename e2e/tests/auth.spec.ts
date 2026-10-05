import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * M1 공통 화면 (docs/05-screens/00-common.md): 로그인, 홈, 비밀번호 변경, 내 정보, 로그아웃.
 * 같은 시나리오가 세 프론트에서 똑같이 동작해야 한다 (ADR-0003).
 */
const prefix = () => test.info().project.metadata.prefix as string
const front = () => test.info().project.name

async function login(page: Page, loginId: string, password = e2ePassword()) {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill(loginId)
  await page.getByLabel('비밀번호', { exact: true }).fill(password)
  await page.getByRole('button', { name: '로그인' }).click()
}

const homeUrl = () => new RegExp(`${prefix()}/?$`)

test('로그인하면 홈에 내 정보와 권한에 맞는 메뉴가 보인다', async ({ page }) => {
  await login(page, 't_system')
  await expect(page).toHaveURL(homeUrl())
  await expect(page.locator('#home-name')).toHaveText('테스트시스템')
  await expect(page.locator('#user-name')).toHaveText('테스트시스템')
  // 시스템관리자: 시스템관리·관리자관리·게시판관리 메뉴가 보이고 회원관리는 보이지 않는다
  const menu = page.locator('#side-menu')
  await expect(menu).toContainText('메뉴관리')
  await expect(menu).toContainText('역할')
  await expect(menu).not.toContainText('사용자관리')
  await expect(page.locator('#home-shortcuts a')).toHaveCount(6)
})

test('아이디가 없으면 같은 실패 메시지를 보여 준다', async ({ page }) => {
  await login(page, 'e2e_no_such_admin', 'wrong')
  await expect(page.getByRole('alert')).toContainText('아이디 또는 비밀번호가 올바르지 않습니다')
  await expect(page).toHaveURL(/\/login/)
})

test('로그인하지 않고 홈에 가면 로그인 화면으로 보낸다', async ({ page }) => {
  await page.goto(`${prefix()}/`)
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('button', { name: '로그인' })).toBeVisible()
})

test('임시 비밀번호로 로그인하면 비밀번호를 바꿔야 홈으로 간다', async ({ page }) => {
  await login(page, `e2e_temp_${front()}`)
  await expect(page).toHaveURL(/\/password$/)
  await expect(page.getByText('임시 비밀번호로 로그인했습니다')).toBeVisible()
  // 헤더·메뉴 없이 보여 준다
  await expect(page.locator('#side-menu')).toBeHidden()

  // 다른 화면으로는 갈 수 없다
  await page.goto(`${prefix()}/`)
  await expect(page).toHaveURL(/\/password$/)

  await page.getByLabel('현재 비밀번호').fill(e2ePassword())
  await page.getByLabel('새 비밀번호', { exact: true }).fill('e2e-new-pw')
  await page.getByLabel('새 비밀번호 확인').fill('e2e-new-pw')
  await page.getByRole('button', { name: '변경' }).click()

  await expect(page).toHaveURL(homeUrl())
  await expect(page.getByRole('status')).toContainText('비밀번호가 변경되었습니다')
})

test('내 정보를 수정하면 저장되고 다시 열어도 바뀐 값이 보인다', async ({ page }) => {
  await login(page, `e2e_me_${front()}`)
  await expect(page).toHaveURL(homeUrl())
  await page.goto(`${prefix()}/me`)
  await expect(page.getByLabel('로그인 아이디')).toHaveValue(`e2e_me_${front()}`)

  await page.getByLabel('부서').fill(`E2E-${front()}`)
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('status')).toContainText('저장되었습니다')

  await page.reload()
  await expect(page.getByLabel('부서')).toHaveValue(`E2E-${front()}`)
})

test('로그아웃하면 로그인 화면으로 가고 다시 들어갈 수 없다', async ({ page }) => {
  await login(page, 't_viewer')
  await expect(page).toHaveURL(homeUrl())
  await page.locator('#user-menu [data-bs-toggle="dropdown"]').click()
  await page.getByRole('button', { name: '로그아웃' }).click()

  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('status')).toContainText('로그아웃되었습니다')
  await page.goto(`${prefix()}/`)
  await expect(page).toHaveURL(/\/login/)
})
