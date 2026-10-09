import { describe, expect, it } from 'vitest'
import { quickRange } from './dateRange'

describe('quickRange', () => {
  const today = new Date(2026, 9, 9)    // 2026-10-09
  it('오늘·1주일·1개월·3개월을 오늘까지로 채운다', () => {
    expect(quickRange('today', today)).toEqual(['2026-10-09', '2026-10-09'])
    expect(quickRange('1w', today)).toEqual(['2026-10-02', '2026-10-09'])
    expect(quickRange('1m', today)).toEqual(['2026-09-09', '2026-10-09'])
    expect(quickRange('3m', today)).toEqual(['2026-07-09', '2026-10-09'])
  })
})
