import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 관리자관리 (docs/05-screens/06-admin.md)와 관리자별 최종 권한 (07-permission.md SCR-PRM-02).
 * 같은 시나리오를 /react, /jsp, /ssr로 3회. t_system(시스템관리자: ADMIN 전 권한)으로 로그인한다.
 * 프론트마다 전용 관리자(e2eadm{front})를 화면에서 등록하고, global-setup이 이전 실행의 계정을 지운다.
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

async function gotoAdmins(page: Page) {
  await page.goto(`${prefix()}/admins`)
  await expect(page.locator('#admin-table tbody tr').first()).toBeVisible()
  await expect(page.locator('#total-count')).not.toBeEmpty()
}

async function openAdmin(page: Page, loginId: string) {
  await gotoAdmins(page)
  await page.locator('#search-loginId').fill(loginId)
  await page.getByRole('button', { name: '검색' }).click()
  await page.locator('#admin-table').getByRole('link', { name: loginId, exact: true }).click()
  await expect(page.locator('#admin-loginId')).toHaveText(loginId)
}

test('목록을 검색·정렬하고 페이지 크기를 바꾼다', async ({ page }) => {
  await login(page)
  await gotoAdmins(page)

  // 로그인 아이디 오름차순 → 다시 누르면 내림차순 (정렬 순서는 DB 콜레이션을 따르므로 서로 역순인지만 본다)
  await page.locator('#admin-table .sort-link[data-sort="loginId"]').click()
  await expect(page.locator('#admin-table th[aria-sort="ascending"]')).toContainText('로그인 아이디')
  const asc = await page.locator('#admin-table tbody tr td:nth-child(2)').allTextContents()
  await page.locator('#admin-table .sort-link[data-sort="loginId"]').click()
  await expect(page.locator('#admin-table th[aria-sort="descending"]')).toContainText('로그인 아이디')
  const desc = await page.locator('#admin-table tbody tr td:nth-child(2)').allTextContents()
  expect(asc.length).toBeLessThan(20)    // 한 페이지에 모두 들어오는 경우에만 역순 비교가 맞다
  expect(desc).toEqual([...asc].reverse())

  // 페이지 크기 10
  await page.locator('#page-size').selectOption('10')
  await expect(page.locator('#page-size')).toHaveValue('10')
  await expect(page.locator('#admin-table tbody tr')).toHaveCount(10)    // 테스트 관리자는 10명보다 많다

  // 검색: 역할이 둘이면 "회원운영자 외 1"
  await page.locator('#search-loginId').fill('t_multi')
  await page.getByRole('button', { name: '검색' }).click()
  await expect(page.locator('#admin-table tbody tr')).toHaveCount(1)
  await expect(page.locator('#admin-table tbody tr')).toContainText('회원운영자 외 1')
  await expect(page.locator('#total-count')).toHaveText('1')
})

test('본인 계정은 역할 변경·사용중지를 할 수 없다', async ({ page }) => {
  await login(page)
  await openAdmin(page, 't_system')
  await expect(page.locator('#admin-title')).toContainText('본인')
  await expect(page.locator('#btn-disable')).toHaveCount(0)
  await expect(page.locator('#role-add-form')).toHaveCount(0)
  await expect(page.locator('#history-table tbody tr').first()).toContainText('성공')
})

test('관리자를 등록하고 수정·역할·비밀번호 초기화·사용중지를 한다', async ({ page }) => {
  await login(page)
  const loginId = `e2eadm${front()}`
  const extraRole = `E2E 권한 ${front()}`

  // 등록 → 상세에 임시 비밀번호가 한 번 보인다 (BR-02)
  await gotoAdmins(page)
  await page.locator('#btn-admin-create').click()
  await page.locator('#admin-loginId').fill(loginId)
  await page.locator('#admin-adminNm').fill(`E2E관리자 ${front()}`)
  await page.locator('#admin-email').fill(`${loginId}@example.com`)
  await page.locator('#admin-mobileNo').fill('01012345678')
  await page.locator('#admin-roles-choice').getByLabel('시스템관리자', { exact: true }).check()
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(page.locator('#temp-password')).toHaveText(/^\S{10}$/)
  await expect(page.locator('#admin-mobileNo')).toHaveText('010-1234-5678')
  await expect(page.locator('#admin-pwdTempYn')).toHaveText('예')
  await page.locator('#btn-temp-close').click()
  await expect(page.locator('#temp-password-box')).toHaveCount(0)

  // 정보 수정
  await page.locator('#btn-edit').click()
  await expect(page.locator('#admin-form-title')).toHaveText('관리자 수정')
  // ②는 입력값을 API로 채운다. 채워진 뒤에 고친다
  await expect(page.locator('#admin-adminNm')).toHaveValue(`E2E관리자 ${front()}`)
  await page.locator('#admin-deptNm').fill('E2E팀')
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('수정되었습니다')
  await expect(page.locator('#admin-deptNm')).toHaveText('E2E팀')

  // 역할 추가·회수
  await page.locator('#role-add-select').selectOption({ label: extraRole })
  await page.locator('#btn-role-add').click()
  await expect(page.getByRole('status')).toContainText('역할이 부여되었습니다')
  const extraRow = page.locator('#role-table tbody tr', { hasText: extraRole })
  await expect(extraRow).toBeVisible()
  page.once('dialog', (d) => d.accept())
  await extraRow.locator('.btn-role-revoke').click()
  await expect(page.getByRole('status')).toContainText('역할이 회수되었습니다')
  await expect(page.locator('#role-table tbody tr')).toHaveCount(1)
  // 역할이 하나면 회수 버튼이 없다 (BR-04)
  await expect(page.locator('.btn-role-revoke')).toHaveCount(0)

  // 비밀번호 초기화
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-password-reset').click()
  await expect(page.getByRole('status')).toContainText('비밀번호가 초기화되었습니다')
  await expect(page.locator('#temp-password')).toHaveText(/^\S{10}$/)

  // 사용중지 → 재사용
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-disable').click()
  await expect(page.getByRole('status')).toContainText('사용중지되었습니다')
  await expect(page.locator('#admin-status')).toHaveText('사용중지')
  await expect(page.locator('#btn-password-reset')).toHaveCount(0)
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-enable').click()
  await expect(page.getByRole('status')).toContainText('재사용 처리되었습니다')
  await expect(page.locator('#admin-status')).toHaveText('사용')
})

test('관리자별 최종 권한을 본다', async ({ page }) => {
  await login(page)
  await page.goto(`${prefix()}/permissions`)
  await page.locator('#btn-effective').click()
  await expect(page.locator('#admin-picker')).toBeVisible()

  await page.locator('#picker-keyword').fill('테스트회원운영')
  await page.locator('#picker-form').getByRole('button', { name: '검색' }).click()
  await page.locator('#picker-table tr', { hasText: 't_member' }).locator('.btn-pick').click()
  await expect(page.locator('#effective-title')).toContainText('테스트회원운영')
  const userRow = page.locator('#effective-table tr', { hasText: '사용자관리' })
  await expect(userRow.locator('td[data-action="PRIVACY"] .perm-granted')).toHaveAttribute('title', '회원운영자')
  await expect(userRow.locator('td[data-action="DELETE"]')).toHaveText('-')

  await page.locator('#picker-keyword').fill('테스트슈퍼')
  await page.locator('#picker-form').getByRole('button', { name: '검색' }).click()
  await page.locator('#picker-table tr', { hasText: 't_super' }).locator('.btn-pick').click()
  await expect(page.getByRole('note')).toContainText('슈퍼관리자는 모든 권한을 가집니다')
})
