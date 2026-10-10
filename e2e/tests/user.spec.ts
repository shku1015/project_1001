import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 사용자관리 (docs/05-screens/02-user.md). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * 회원 전 권한이 있는 t_super로 로그인한다. 프론트마다 전용 회원(e2eusr{front})을 화면에서 등록하고,
 * global-setup이 이전 실행의 회원을 지운다. 테스트 데이터 회원은 원문 보기만 하고 바꾸지 않는다.
 */
const prefix = () => test.info().project.metadata.prefix as string
const front = () => test.info().project.name

async function login(page: Page) {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill('t_super')
  await page.getByLabel('비밀번호', { exact: true }).fill(e2ePassword())
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/?$`))
  await expect(page.locator('#user-name')).toHaveText('테스트슈퍼')
}

async function search(page: Page, field: string, value: string) {
  await page.goto(`${prefix()}/users`)
  await expect(page.locator('#total-count')).not.toBeEmpty()
  await page.locator(`#search-${field}`).fill(value)
  await page.getByRole('button', { name: '검색' }).click()
}

async function openUser(page: Page, loginId: string) {
  await search(page, 'loginId', loginId)
  await page.locator('#user-table').getByRole('link', { name: loginId, exact: true }).click()
  await expect(page.locator('#user-loginId')).toHaveText(loginId)
}

async function reason(page: Page, button: string, text: string) {
  await page.locator(button).click()
  await expect(page.locator('#reason-modal')).toBeVisible()
  await page.locator('#reason-input').fill(text)
  await page.locator('#btn-reason-save').click()
}

test('목록은 개인정보를 가려서 보여 주고 원문으로 검색한다', async ({ page }) => {
  await login(page)
  await search(page, 'userNm', '세나')
  const row = page.locator('#user-table tbody tr')
  await expect(row).toHaveCount(1)
  await expect(row).toContainText('p_user03')
  await expect(row).toContainText('박**라')
  await expect(row).toContainText('010-****-0003')
  await expect(row).not.toContainText('박세나라')
})

test('기업 상세의 전체 보기는 그 기업의 회원 목록을 연다', async ({ page }) => {
  await login(page)
  await page.goto(`${prefix()}/companies`)
  await page.locator('#search-companyNm').fill('(주)테스트상사')
  await page.getByRole('button', { name: '검색' }).click()
  await page.locator('#company-table').getByRole('link', { name: '(주)테스트상사', exact: true }).click()
  await page.locator('#btn-users-all').click()
  await expect(page.locator('#total-count')).toHaveText('4')
  await expect(page.locator('#user-table tbody')).toContainText('c_user01')
})

test('사유를 고르면 개인정보 원문을 볼 수 있다', async ({ page }) => {
  await login(page)
  await openUser(page, 'p_user01')
  await expect(page.locator('#user-userNm')).toHaveText('김*나')
  await page.locator('#btn-privacy').click()
  await expect(page.locator('#privacy-modal')).toBeVisible()
  await page.locator('#privacy-reasonCd').selectOption({ label: '고객 문의 응대' })
  await page.locator('#btn-privacy-save').click()
  await expect(page.locator('#user-userNm')).toHaveText('김하나')
  await expect(page.locator('#user-mobileNo')).toHaveText('010-0000-0001')
  await expect(page.locator('#privacy-revealed')).toBeVisible()
  await expect(page.locator('#btn-privacy')).toHaveCount(0)
})

test('기업 회원을 등록하고 수정·정지·강제 탈퇴·삭제한다', async ({ page }) => {
  await login(page)
  const loginId = `e2eusr${front()}`

  await page.goto(`${prefix()}/users`)
  await page.locator('#btn-user-create').click()
  await page.locator('#user-type-CORPORATE').check()
  await page.locator('#user-loginId').fill(loginId)
  await page.locator('#user-userNm').fill('이투이')
  await page.locator('#user-email').fill(`${loginId}@example.com`)
  await page.locator('#user-mobileNo').fill('01055556666')
  await expect(page.locator('#user-companyId option', { hasText: '(주)테스트상사' })).toHaveCount(1)
  await page.locator('#user-companyId').selectOption({ label: '(주)테스트상사' })
  await page.locator('#user-deptNm').fill('영업팀')
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(page.locator('#temp-password')).toHaveText(/^\S{10}$/)
  await expect(page.locator('#user-status')).toHaveText('정상')
  await expect(page.locator('#user-joinPath')).toHaveText('관리자 등록')
  await expect(page.locator('#user-company')).toHaveText('(주)테스트상사')

  // 수정 화면은 원문을 보여 준다 (②는 입력값을 API로 채우므로 채워진 뒤에 고친다)
  await page.locator('#btn-edit').click()
  await expect(page.locator('#user-form-title')).toHaveText('회원 수정')
  await expect(page.locator('#user-userNm')).toHaveValue('이투이')
  await page.locator('#user-userNm').fill('이수정')
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('수정되었습니다')
  await expect(page.locator('#user-userNm')).toHaveText('이*정')

  // 정지 → 정지 해제 → 강제 탈퇴
  await reason(page, '#btn-suspend', 'E2E 정지')
  await expect(page.getByRole('status')).toContainText('상태가 변경되었습니다')
  await expect(page.locator('#user-status')).toHaveText('정지')
  await reason(page, '#btn-resume', 'E2E 해제')
  await expect(page.locator('#user-status')).toHaveText('정상')
  await reason(page, '#btn-withdraw', 'E2E 강제 탈퇴')
  await expect(page.locator('#withdrawn-note')).toBeVisible()
  await expect(page.locator('#btn-edit')).toHaveCount(0)
  await expect(page.locator('#history-table tbody tr')).toHaveCount(3)

  // 삭제 (사유 입력)
  await reason(page, '#btn-delete', 'E2E 정리')
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
  await expect(page).toHaveURL(new RegExp(`${prefix()}/users$`))
})
