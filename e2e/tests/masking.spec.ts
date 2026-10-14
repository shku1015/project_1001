import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 마스킹 설정 (docs/05-screens/10-masking.md). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * t_system(시스템관리자: MASKING READ·UPDATE)으로 로그인한다. 바꾼 설정은 시나리오 끝에 되돌린다.
 */
const prefix = () => test.info().project.metadata.prefix as string

async function login(page: Page) {
  await page.goto(`${prefix()}/login`)
  await page.getByLabel('아이디', { exact: true }).fill('t_system')
  await page.getByLabel('비밀번호', { exact: true }).fill(e2ePassword())
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/?$`))
  await expect(page.locator('#user-name')).toHaveText('테스트시스템')
}

async function gotoMasking(page: Page) {
  await page.goto(`${prefix()}/masking`)
  await expect(page.locator('#masking-table tr.masking-row')).toHaveCount(4)
}

async function saveWithConfirm(page: Page) {
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('저장되었습니다')
}

test('항목별 마스킹 예가 보이고 바뀐 내용이 없으면 저장하지 않는다', async ({ page }) => {
  await login(page)
  await gotoMasking(page)
  const mobile = page.locator('#masking-table tr[data-field-cd="MOBILE_NO"]')
  await expect(mobile).toContainText('휴대폰 번호')
  await expect(mobile).toContainText('010-****-5678')
  await page.locator('#btn-save').click()
  await expect(page.locator('#masking-message')).toContainText('바뀐 내용이 없습니다')
})

test('화면 마스킹을 끄고 저장한 뒤 다시 켠다', async ({ page }) => {
  await login(page)
  await gotoMasking(page)
  await page.locator('#screen-EMAIL').uncheck()
  await saveWithConfirm(page)
  await gotoMasking(page)
  await expect(page.locator('#screen-EMAIL')).not.toBeChecked()
  await expect(page.locator('#masking-table tr[data-field-cd="EMAIL"]')).toContainText('테스트시스템')

  await page.locator('#screen-EMAIL').check()
  await saveWithConfirm(page)
  await gotoMasking(page)
  await expect(page.locator('#screen-EMAIL')).toBeChecked()
})
