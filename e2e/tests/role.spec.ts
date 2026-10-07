import { expect, test, type Page } from '@playwright/test'
import { e2ePassword } from '../global-setup'

/**
 * 역할관리 (docs/05-screens/08-role.md). 같은 시나리오를 /react, /jsp, /ssr로 3회.
 * t_system(시스템관리자: SYSTEM_ADMIN 역할, ROLE·CODE 전 권한, USER 권한 없음)으로 로그인한다.
 * 프론트마다 전용 역할(E2E_{FRONT}_*)을 쓰고, global-setup이 이전 실행의 역할을 지운다.
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

async function gotoRoles(page: Page) {
  await page.goto(`${prefix()}/roles`)
  await expect(page.locator('#role-table tbody tr').first()).toBeVisible()
}

async function openRole(page: Page, roleCd: string) {
  await gotoRoles(page)
  await page.locator('#role-table').getByRole('link', { name: new RegExp(`${roleCd}$`) }).click()
  await expect(page.locator('#role-roleCd')).toHaveText(roleCd)
}

const permBox = (page: Page, label: string) => page.getByRole('checkbox', { name: label, exact: true })

test('역할 목록이 보이고 검색한다', async ({ page }) => {
  await login(page)
  await gotoRoles(page)
  const table = page.locator('#role-table')
  await expect(table).toContainText('🔒 SUPER_ADMIN')
  await expect(table).toContainText('VIEWER')

  await page.locator('#search-keyword').fill('조회전용')
  await page.getByRole('button', { name: '검색' }).click()
  await expect(table.locator('tbody tr')).toHaveCount(1)
  await expect(table).toContainText('VIEWER')
})

test('내 역할과 시스템 역할은 읽기 전용이다', async ({ page }) => {
  await login(page)
  await openRole(page, 'SYSTEM_ADMIN')
  await expect(page.getByRole('note')).toContainText('내가 가진 역할')
  await expect(page.locator('#btn-edit')).toHaveCount(0)
  await expect(page.locator('#btn-perm-save')).toBeHidden()
  await expect(permBox(page, '코드관리 조회')).toBeDisabled()
  await expect(permBox(page, '코드관리 조회')).toBeChecked()

  // 관리자 탭에 나(t_system)가 보인다
  await page.locator('#tab-admins').click()
  await expect(page.locator('#admin-table')).toContainText('t_system')

  await openRole(page, 'SUPER_ADMIN')
  await expect(page.getByRole('note')).toContainText('시스템 역할')
  await expect(page.locator('#btn-delete')).toHaveCount(0)
})

test('역할을 등록하고 권한을 설정·수정·복사·삭제한다', async ({ page }) => {
  await login(page)
  const roleCd = `E2E_${front().toUpperCase()}_ROLE`
  const copyCd = `E2E_${front().toUpperCase()}_COPY`

  // 등록 → 상세
  await gotoRoles(page)
  await page.locator('#btn-role-create').click()
  await page.locator('#role-roleCd').fill(roleCd)
  await page.locator('#role-roleNm').fill(`E2E 역할 ${front()}`)
  await page.locator('#role-description').fill('E2E 설명')
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('등록되었습니다')
  await expect(page.locator('#role-roleCd')).toHaveText(roleCd)

  // 권한 설정: 수정을 체크하면 조회도 체크된다 (BR-03). 내가 갖지 않은 권한은 체크할 수 없다 (BR-05)
  await expect(permBox(page, '사용자관리 조회')).toBeDisabled()
  await permBox(page, '코드관리 수정').check()
  await expect(permBox(page, '코드관리 조회')).toBeChecked()
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-perm-save').click()
  await expect(page.getByRole('status')).toContainText('권한이 저장되었습니다')
  await page.reload()
  await expect(permBox(page, '코드관리 수정')).toBeChecked()
  await expect(permBox(page, '코드관리 조회')).toBeChecked()

  // 조회를 해제하면 같은 행이 모두 해제된다 (저장하지 않음)
  await permBox(page, '코드관리 조회').uncheck()
  await expect(permBox(page, '코드관리 수정')).not.toBeChecked()

  // 정보 수정
  await page.locator('#btn-edit').click()
  await expect(page.locator('#role-form-title')).toHaveText('역할 수정')
  await expect(page.locator('#role-roleCd')).toHaveValue(roleCd)
  await page.locator('#role-roleNm').fill(`E2E 역할 ${front()} 수정`)
  await page.locator('#role-useYn-N').check()
  await page.locator('#btn-save').click()
  await expect(page.getByRole('status')).toContainText('수정되었습니다')
  await expect(page.locator('#role-title')).toContainText(`E2E 역할 ${front()} 수정`)
  await expect(page.locator('#role-useYn')).toHaveText('사용 안 함')

  // 복사 → 권한이 같은 새 역할
  await page.locator('#btn-copy').click()
  await page.locator('#copy-roleCd').fill(copyCd)
  await page.locator('#copy-roleNm').fill(`E2E 복사 ${front()}`)
  await page.locator('#btn-copy-save').click()
  await expect(page.getByRole('status')).toContainText('복사되었습니다')
  await expect(page.locator('#role-roleCd')).toHaveText(copyCd)
  await expect(permBox(page, '코드관리 수정')).toBeChecked()

  // 삭제 (복사본, 원본)
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-delete').click()
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
  await expect(page).toHaveURL(new RegExp(`${prefix()}/roles$`))
  await openRole(page, roleCd)
  page.once('dialog', (d) => d.accept())
  await page.locator('#btn-delete').click()
  await expect(page.getByRole('status')).toContainText('삭제되었습니다')
  await expect(page.locator('#role-table')).not.toContainText(roleCd)
})
