// 공통 표시 형식 (docs/05-ia-screens.md 4.1)
import type { MenuNode } from '../api/types'

/** "2026-10-04T14:30:15" → "2026-10-04 14:30" */
export function formatDateTime(value: string | null | undefined): string {
  return value ? value.replace('T', ' ').substring(0, 16) : '-'
}

/** "2026-10-04T14:30:15" → "2026-10-04 14:30:15" (상세 화면) */
export function formatDateTimeSec(value: string | null | undefined): string {
  return value ? value.replace('T', ' ').substring(0, 19) : '-'
}

/** "01012345678" → "010-1234-5678" */
export function formatMobile(value: string | null | undefined): string {
  if (!value) return '-'
  return value.length === 11 ? value.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3') : value.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3')
}

/** "1234567890" → "123-45-67890" */
export function formatBizRegNo(value: string | null | undefined): string {
  return value && value.length === 10 ? `${value.slice(0, 3)}-${value.slice(3, 5)}-${value.slice(5)}` : value || '-'
}

/** "2026-10-04T14:30:15" → "2026-10-04" */
export function formatDate(value: string | null | undefined): string {
  return value ? value.substring(0, 10) : '-'
}

/** 메뉴 트리에서 화면 메뉴만 순서대로 꺼낸다 (홈 바로가기) */
export function pageMenus(menus: MenuNode[]): MenuNode[] {
  return menus.flatMap((m) => (m.menuTypeCd === 'PAGE' ? [m] : pageMenus(m.children)))
}
