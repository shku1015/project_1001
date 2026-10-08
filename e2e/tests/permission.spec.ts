import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 권한관리 (docs/05-screens/07-permission.md SCR-PRM-01). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * t_system(시스템관리자: PERMISSION READ·UPDATE, CODE 전 권한, USER 권한 없음)으로 로그인한다.
 * 기본 역할은 바꾸지 않고 global-setup이 만든 프론트별 역할('E2E 권한 {front}')의 권한만 바꾼다.
 */
const prefix = () => test.info().project.metadata.prefix as string
const front = () => test.info().project.name

async function login(page: Page) {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill('t_system')
  await page.getByLabel('비밀번호', { exact: true }).fill(e2ePassword())
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/?$`))
  await expect(page.locator('#user-name')).toHaveText('테스트시스템')
}

async function openMenu(page: Page, menuNm: string) {
  await page.goto(`${prefix()}/permissions`)
  await page.locator('#perm-menu-card').getByRole('link', { name: menuNm, exact: true }).click()
  await expect(page.locator('#grant-title')).toContainText(menuNm)
}

const box = (page: Page, label: string) => page.getByRole('checkbox', { name: label, exact: true })

test('메뉴를 고르면 역할 × 액션 표가 보인다', async ({ page }) => {
  await login(page)
  await openMenu(page, '코드관리')
  const superRow = page.locator('#grant-table tr.grant-row', { hasText: '슈퍼관리자' })
  await expect(superRow.locator('.perm-all')).toHaveCount(4)
  // 내 역할은 바꿀 수 없다 (BR-04)
  const mine = page.locator('#grant-table tr.grant-row', { hasText: '시스템관리자' })
  await expect(mine).toContainText('내 역할')
  await expect(box(page, '시스템관리자 조회')).toBeDisabled()
  await expect(box(page, '회원운영자 조회')).toBeChecked()
  await expect(box(page, '회원운영자 조회')).toBeEnabled()

  // 내가 갖지 않은 권한은 체크할 수 없다 (BR-05)
  await page.locator('#perm-menu-card').getByRole('link', { name: '사용자관리', exact: true }).click()
  await expect(page.locator('#grant-title')).toContainText('사용자관리')
  await expect(box(page, `E2E 권한 ${front()} 조회`)).toBeDisabled()
})

test('메뉴 기준으로 역할에 권한을 부여하고 회수한다', async ({ page }) => {
  await login(page)
  const role = `E2E 권한 ${front()}`
  await openMenu(page, '코드관리')

  // 바뀐 내용이 없으면 저장하지 않는다
  await page.locator('#btn-grant-save').click()
  await expect(page.locator('#grant-message')).toContainText('바뀐 내용이 없습니다')

  // 수정을 체크하면 조회도 체크된다 (BR-02)
  await box(page, `${role} 수정`).check()
  await expect(box(page, `${role} 조회`)).toBeChecked()
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-grant-save').click()
  await expect(page.getByRole('status')).toContainText('권한이 저장되었습니다')

  await openMenu(page, '코드관리')
  await expect(box(page, `${role} 수정`)).toBeChecked()
  await expect(box(page, `${role} 조회`)).toBeChecked()

  // 조회를 해제하면 같은 행이 모두 해제된다
  await box(page, `${role} 조회`).uncheck()
  await expect(box(page, `${role} 수정`)).not.toBeChecked()
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-grant-save').click()
  await expect(page.getByRole('status')).toContainText('권한이 저장되었습니다')

  await openMenu(page, '코드관리')
  await expect(box(page, `${role} 조회`)).not.toBeChecked()
})
