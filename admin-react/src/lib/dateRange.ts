// CMP-05 기간 선택의 빠른 선택 (오늘, 1주일, 1개월, 3개월)
export type QuickRange = 'today' | '1w' | '1m' | '3m'

/** 빠른 선택 값 → [시작일, 종료일] (오늘 포함). ②③은 admin-form-kit.js */
export function quickRange(range: QuickRange, today = new Date()): [string, string] {
  const ymd = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const to = new Date(today)
  const from = new Date(today)
  if (range === '1w') from.setDate(from.getDate() - 7)
  if (range === '1m') from.setMonth(from.getMonth() - 1)
  if (range === '3m') from.setMonth(from.getMonth() - 3)
  return [ymd(from), ymd(to)]
}
