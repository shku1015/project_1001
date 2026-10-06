import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 메뉴관리 (docs/05-screens/03-menu.md). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * t_system(시스템관리자)으로 로그인한다. 프론트마다 전용 메뉴(E2E_{FRONT}_*)를 써서 서로 겹치지 않게 하고,
 * 순서 변경용 메뉴는 global-setup이 매번 다시 만든다.
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

async function gotoMenus(page: Page) {
  await page.goto(`${prefix()}/menus`)
  await expect(page.locator('#menu-tree-card .menu-node-link').first()).toBeVisible()
}

const treeLink = (page: Page, name: string) =>
  page.locator('#menu-tree-card').getByRole('link', { name, exact: true })

/** 상위 메뉴 이름 아래 하위 메뉴 이름 순서 */
const childNames = (page: Page, parentNm: string) =>
  page.locator('#menu-tree-card li.menu-node', { has: page.locator(':scope > .menu-node-row a', { hasText: parentNm }) })
    .locator(':scope > ul > li > .menu-node-row .menu-node-link')
    .allTextContents()

async function select(page: Page, name: string) {
  await treeLink(page, name).click()
  await expect(page.locator('#menu-detail-card')).toBeVisible()
  await expect(page.locator('#menu-menuNm')).toHaveValue(name)
}

test('메뉴 트리가 보이고 메뉴를 선택하면 상세가 보인다', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: '메뉴관리' }).first().click()
  await expect(page).toHaveURL(new RegExp(`${prefix()}/menus`))

  await select(page, '코드관리')
  await expect(page.locator('#menu-menuCd')).toHaveValue('CODE')
  await expect(page.locator('#menu-menuUrl')).toHaveValue('/codes')
  await expect(page.locator('#action-READ')).toBeChecked()
  await expect(page.locator('#btn-delete')).toBeVisible()

  // 시스템 메뉴는 메뉴명·아이콘만 바꿀 수 있고 이동·삭제 버튼이 없다
  await select(page, '메뉴관리')
  await expect(page.getByRole('note')).toContainText('메뉴명·아이콘만')
  await expect(page.locator('#btn-move')).toHaveCount(0)
  await expect(page.locator('#btn-delete')).toHaveCount(0)
})

test('하위 화면 메뉴를 등록·수정·삭제한다', async ({ page }) => {
  await login(page)
  await gotoMenus(page)
  const code = `E2E_${front().toUpperCase()}_MENU`
  const name = `E2E 메뉴 ${front()}`

  // 회원관리 아래에 화면 메뉴 등록
  await select(page, '회원관리')
  await page.locator('#btn-child-create').click()
  await expect(page.locator('#menu-parent')).toHaveValue('회원관리')
  await page.locator('#menu-menuCd').fill(code)
  await page.locator('#menu-menuNm').fill(name)
  await page.locator('#menu-type-PAGE').check()
  await page.locator('#menu-menuUrl').fill(`/e2e-${front()}`)
  await page.locator('#action-CREATE').check()
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(treeLink(page, name)).toBeVisible()
  await expect(page.locator('#menu-menuCd')).toHaveValue(code)
  await expect(page.locator('#action-CREATE')).toBeChecked()

  // 메뉴명 수정 (역할에 준 권한이 없으므로 회수 확인 없이 저장된다)
  await page.locator('#menu-menuNm').fill(`${name} 수정`)
  await page.locator('#action-CREATE').uncheck()
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('수정되었습니다')
  await expect(treeLink(page, `${name} 수정`)).toBeVisible()
  await expect(page.locator('#action-CREATE')).not.toBeChecked()

  // 삭제
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-delete').click()
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
  await expect(treeLink(page, `${name} 수정`)).toHaveCount(0)
})

test('▲로 순서를 바꾸고 순서 저장하면 다시 열어도 유지된다', async ({ page }) => {
  await login(page)
  await gotoMenus(page)
  const folder = `E2E 순서 ${front()}`
  const first = `E2E 가 ${front()}`
  const second = `E2E 나 ${front()}`
  expect(await childNames(page, folder)).toEqual([first, second])

  await select(page, second)
  await page.locator('#btn-up').click()
  await expect(page.locator('#order-bar')).toBeVisible()
  expect(await childNames(page, folder)).toEqual([second, first])
  // 저장하지 않은 순서가 있으면 구조를 바꾸는 버튼은 막힌다
  await expect(page.locator('#btn-root-create')).toBeDisabled()

  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-order-save').click()
  await expect(page.getByRole('status')).toContainText('순서가 저장되었습니다')
  await expect(page.locator('#order-bar')).toBeHidden()

  await gotoMenus(page)
  expect(await childNames(page, folder)).toEqual([second, first])
})

test('순서를 바꾼 뒤 되돌리기를 누르면 원래 순서로 돌아간다', async ({ page }) => {
  await login(page)
  await gotoMenus(page)
  await select(page, '코드관리')
  const before = await childNames(page, '시스템관리')

  await page.locator('#btn-down').click()
  await expect(page.locator('#order-bar')).toBeVisible()
  expect(await childNames(page, '시스템관리')).not.toEqual(before)

  await page.locator('#btn-order-reset').click()
  await expect(page.locator('#order-bar')).toBeHidden()
  expect(await childNames(page, '시스템관리')).toEqual(before)
  await expect(page.locator('#btn-root-create')).toBeEnabled()
})
