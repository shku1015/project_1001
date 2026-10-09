import { quickRange, type QuickRange } from '../lib/dateRange'

/**
 * CMP-05 기간 선택: 시작일 ~ 종료일 + 빠른 선택(오늘, 1주일, 1개월, 3개월). ②③의 .date-range 마크업과 같다.
 */
export function DateRange({ id, label, from, to, onChange }: {
  id: string; label: string; from: string; to: string; onChange: (from: string, to: string) => void
}) {
  const quick: [string, QuickRange][] = [['오늘', 'today'], ['1주일', '1w'], ['1개월', '1m'], ['3개월', '3m']]
  return (
    <div className="date-range">
      <div className="input-group">
        <input type="date" className="form-control" id={`search-${id}From`} aria-label={`${label} 시작`} data-range-from
               value={from} onChange={(e) => onChange(e.target.value, to)} />
        <span className="input-group-text">~</span>
        <input type="date" className="form-control" id={`search-${id}To`} aria-label={`${label} 끝`} data-range-to
               value={to} onChange={(e) => onChange(from, e.target.value)} />
      </div>
      <div className="btn-list mt-1">
        {quick.map(([text, range]) => (
          <button type="button" className="btn btn-sm date-quick" data-range={range} key={range}
                  onClick={() => onChange(...quickRange(range))}>{text}</button>
        ))}
      </div>
    </div>
  )
}
