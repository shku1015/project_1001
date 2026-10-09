import { describe, expect, it } from 'vitest'
import type { MenuNode } from '../api/types'
import { formatDate, formatDateTime, formatDateTimeSec, formatMobile, pageMenus } from './format'

const menu = (menuCd: string, menuTypeCd: 'FOLDER' | 'PAGE', children: MenuNode[] = []): MenuNode => ({
  menuId: menuCd.length, menuCd, menuNm: menuCd, menuTypeCd, menuUrl: menuTypeCd === 'PAGE' ? `/${menuCd}` : null,
  icon: null, children,
})

describe('format', () => {
  it('일시는 분까지, 날짜는 날짜만 보여 준다', () => {
    expect(formatDateTime('2026-10-04T14:30:15')).toBe('2026-10-04 14:30')
    expect(formatDate('2026-10-04T14:30:15')).toBe('2026-10-04')
    expect(formatDateTime(null)).toBe('-')
    expect(formatDateTimeSec('2026-10-04T14:30:15')).toBe('2026-10-04 14:30:15')
  })

  it('휴대폰 번호에 하이픈을 넣는다', () => {
    expect(formatMobile('01012345678')).toBe('010-1234-5678')
    expect(formatMobile('0101234567')).toBe('010-123-4567')
    expect(formatMobile(null)).toBe('-')
  })

  it('메뉴 트리에서 화면 메뉴만 순서대로 꺼낸다', () => {
    const tree = [menu('A', 'FOLDER', [menu('A1', 'PAGE'), menu('A2', 'FOLDER', [menu('A21', 'PAGE')])]), menu('B', 'PAGE')]
    expect(pageMenus(tree).map((m) => m.menuCd)).toEqual(['A1', 'A21', 'B'])
  })
})
