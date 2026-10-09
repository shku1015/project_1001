import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 기업정보관리 (docs/05-screens/01-company.md). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * 기업 전 권한이 있는 t_super로 로그인한다. 프론트마다 전용 사업자등록번호(800000000x)를 쓰고,
 * global-setup이 이전 실행의 기업을 지운다. 우편번호 검색은 외부 서비스라 E2E에서 열지 않는다.
 */
const prefix = () => test.info().project.metadata.prefix as string
const front = () => test.info().project.name
const BIZ = { react: '8000000001', jsp: '8000000002', ssr: '8000000003' } as Record<string, string>

async function login(page: Page) {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill('t_super')
  await page.getByLabel('비밀번호', { exact: true }).fill(e2ePassword())
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/?$`))
  await expect(page.locator('#user-name')).toHaveText('테스트슈퍼')
}

async function gotoCompanies(page: Page) {
  await page.goto(`${prefix()}/companies`)
  await expect(page.locator('#company-table tbody tr').first()).toBeVisible()
  await expect(page.locator('#total-count')).not.toBeEmpty()
}

async function openCompany(page: Page, name: string) {
  await gotoCompanies(page)
  await page.locator('#search-companyNm').fill(name)
  await page.getByRole('button', { name: '검색' }).click()
  await page.locator('#company-table').getByRole('link', { name, exact: true }).click()
  await expect(page.locator('#company-companyNm')).toHaveText(name)
}

test('목록을 검색·정렬하고 엑셀을 내려받는다', async ({ page }) => {
  await login(page)
  await gotoCompanies(page)

  // 소속 회원 수 내림차순 (머리글을 두 번 누른다)
  const sortLink = page.locator('#company-table .sort-link[data-sort="memberCnt"]')
  await sortLink.click()
  await expect(page.locator('#company-table th[aria-sort="ascending"]')).toContainText('소속 회원 수')
  await page.locator('#company-table .sort-link[data-sort="memberCnt"]').click()
  await expect(page.locator('#company-table th[aria-sort="descending"]')).toContainText('소속 회원 수')
  await expect(page.locator('#company-table tbody tr').first()).toContainText('(주)테스트상사')

  // 상태 + 기간(오늘) 검색
  await page.locator('#search-statusCd').selectOption('SUSPENDED')
  await page.locator('.date-quick[data-range="today"]').click()
  await expect(page.locator('#search-regDtFrom')).not.toHaveValue('')
  await page.getByRole('button', { name: '검색' }).click()
  await expect(page.locator('#company-table tbody')).toContainText('테스트유통(주)')
  await expect(page.locator('#company-table tbody')).not.toContainText('테스트물산')

  // 엑셀: 파일명은 기업정보관리_yyyyMMddHHmm.xlsx
  const download = page.waitForEvent('download')
  await page.locator('#btn-excel').click()
  expect((await download).suggestedFilename()).toMatch(/^기업정보관리_\d{12}\.xlsx$/)
})

test('소속 회원이 있으면 삭제할 수 없고 회원 이름은 가려진다', async ({ page }) => {
  await login(page)
  await openCompany(page, '(주)테스트상사')
  await expect(page.locator('#company-bizRegNo')).toHaveText('100-00-00001')
  await expect(page.locator('#delete-note')).toContainText('소속 회원이 있어 삭제할 수 없습니다')
  await expect(page.locator('#btn-delete')).toHaveCount(0)
  await expect(page.locator('#users-total')).toHaveText('(4명)')
  await expect(page.locator('#users-table')).toContainText('한*업')
  await expect(page.locator('#users-table')).not.toContainText('한기업')
})

test('기업을 등록하고 수정·정지·정지 해제·삭제한다', async ({ page }) => {
  await login(page)
  const name = `E2E기업 ${front()}`
  const bizRegNo = BIZ[front()]

  await gotoCompanies(page)
  await page.locator('#btn-company-create').click()
  await page.locator('#company-companyNm').fill(name)
  await page.locator('#company-bizRegNo').fill(bizRegNo)
  await page.locator('#company-ceoNm').fill('이대표')
  await page.locator('#company-telNo').fill('0212345678')
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(page.locator('#company-bizRegNo')).toHaveText(`${bizRegNo.slice(0, 3)}-${bizRegNo.slice(3, 5)}-${bizRegNo.slice(5)}`)
  await expect(page.locator('#company-status')).toHaveText('정상')

  // 수정 (②는 입력값을 API로 채우므로 채워진 뒤에 고친다)
  await page.locator('#btn-edit').click()
  await expect(page.locator('#company-form-title')).toHaveText('기업 수정')
  await expect(page.locator('#company-companyNm')).toHaveValue(name)
  await expect(page.locator('#company-bizRegNo')).not.toBeEditable()
  await page.locator('#company-ceoNm').fill('박대표')
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('수정되었습니다')
  await expect(page.locator('#company-ceoNm')).toHaveText('박대표')

  // 정지 → 정지 해제 (사유 입력창)
  await page.locator('#btn-suspend').click()
  await expect(page.locator('#reason-modal')).toBeVisible()
  await expect(page.locator('#reason-title')).toHaveText('기업 정지')
  await page.locator('#reason-input').fill('E2E 정지 사유')
  await page.locator('#btn-reason-save').click()
  await expect(page.getByRole('status')).toContainText('정지되었습니다')
  await expect(page.locator('#company-status')).toHaveText('정지')
  await page.locator('#btn-resume').click()
  await page.locator('#reason-input').fill('E2E 해제 사유')
  await page.locator('#btn-reason-save').click()
  await expect(page.getByRole('status')).toContainText('정지 해제되었습니다')
  await expect(page.locator('#company-status')).toHaveText('정상')

  // 삭제
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-delete').click()
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
  await expect(page).toHaveURL(new RegExp(`${prefix()}/companies$`))
})
