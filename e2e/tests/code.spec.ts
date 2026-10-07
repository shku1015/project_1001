import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 코드관리 (docs/05-screens/04-code.md). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * t_system(시스템관리자)으로 로그인한다. 테스트마다 전용 그룹코드를 써서 여러 번 실행해도 안전하게 한다.
 */
const prefix = () => test.info().project.metadata.prefix as string
const front = () => test.info().project.name

async function login(page: Page, loginId = 't_system', adminNm = '테스트시스템') {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill(loginId)
  await page.getByLabel('비밀번호', { exact: true }).fill(e2ePassword())
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/?$`))
  // 홈 레이아웃이 자리 잡은 뒤 다음 화면으로 이동한다 (이동 중 리다이렉트와 겹치지 않게)
  await expect(page.locator('#user-name')).toHaveText(adminNm)
}

async function gotoCodes(page: Page) {
  await page.goto(`${prefix()}/codes`)
  await expect(page.getByRole('heading', { name: '그룹코드' })).toBeVisible()
}

// 그룹코드 테이블에서 특정 그룹 행
const groupRow = (page: Page, groupCd: string) =>
  page.locator('table').first().locator('tr', { has: page.getByRole('link', { name: new RegExp(groupCd) }) })

test('코드관리 메뉴가 보이고 초기 코드가 조회된다', async ({ page }) => {
  await login(page)
  // 왼쪽 메뉴에 코드관리가 있다
  await page.getByRole('link', { name: '코드관리' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/codes`))

  await page.getByRole('link', { name: /USER_STATUS/ }).click()
  // 상세코드에 정상/휴면/정지/탈퇴가 보인다
  const detail = page.locator('#detail-panel, .col-lg-7').last()
  await expect(detail).toContainText('정상')
  await expect(detail).toContainText('탈퇴')
})

test('그룹코드를 등록하고 상세코드를 등록한다', async ({ page }) => {
  await login(page)
  await gotoCodes(page)
  const group = `E2E_${front().toUpperCase()}_G`

  // 그룹 등록
  await page.getByRole('button', { name: '그룹 등록' }).click()
  await page.locator('#group-groupCd').fill(group)
  await page.locator('#group-groupNm').fill('E2E 그룹')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(groupRow(page, group)).toBeVisible()

  // 상세코드 등록
  await page.getByRole('link', { name: new RegExp(group) }).click()
  await page.getByRole('button', { name: '코드 등록' }).click()
  await page.locator('#detail-code').fill('FIRST')
  await page.locator('#detail-codeNm').fill('첫째')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(page.locator('.col-lg-7').last()).toContainText('FIRST')

  // 정리: 상세코드·그룹 삭제 (반복 실행 대비)
  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: '삭제' }).first().click()
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: '그룹 삭제' }).click()
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
})

test('시스템 코드 그룹은 삭제·코드 등록 버튼이 없다', async ({ page }) => {
  await login(page)
  await gotoCodes(page)
  await page.getByRole('link', { name: /USER_STATUS/ }).click()
  const panel = page.locator('.col-lg-7').last()
  await expect(panel.getByRole('button', { name: '그룹 수정' })).toBeVisible()
  await expect(panel.getByRole('button', { name: '그룹 삭제' })).toHaveCount(0)
  await expect(panel.getByRole('button', { name: '코드 등록' })).toHaveCount(0)
})

test('조회전용 관리자는 코드관리 메뉴가 보이지 않는다', async ({ page }) => {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill('t_viewer')
  await page.getByLabel('비밀번호', { exact: true }).fill(e2ePassword())
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/?$`))
  await expect(page.locator('#side-menu')).not.toContainText('코드관리')
})

test('조회 권한만 있으면 등록·수정 버튼이 비활성으로 보인다', async ({ page }) => {
  // t_member(회원운영자)는 코드관리 READ만 있다 (docs/05-ia-screens.md 4.2)
  await login(page, 't_member', '테스트회원운영')
  await gotoCodes(page)
  await expect(page.locator('#btn-group-create')).toBeDisabled()
  await expect(page.locator('#btn-group-create')).toHaveAttribute('title', '권한이 없습니다')

  await page.getByRole('link', { name: /USER_STATUS/ }).click()
  await expect(page.locator('#btn-group-edit')).toBeDisabled()
  await expect(page.locator('.col-lg-7').last().getByRole('button', { name: '수정', exact: true }).first()).toBeDisabled()
})
