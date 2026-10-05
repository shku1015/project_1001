import type { Me } from '../api/types'

export type Action = 'READ' | 'CREATE' | 'UPDATE' | 'DELETE' | 'EXCEL' | 'PRIVACY'

/** 메뉴 × 액션 권한 확인. 슈퍼관리자는 모든 권한 (ADR-0002) */
export function can(me: Me, menuCd: string, action: Action): boolean {
  return me.superAdmin || (me.permissions[menuCd]?.includes(action) ?? false)
}
