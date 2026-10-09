/** 목록 응답의 페이지 정보 (docs/06-api-spec.md 4절) */
export type PageMeta = { page: number; size: number; totalCount: number; totalPages: number }

/**
 * CMP-03 페이징: « ‹ 번호(최대 10개) › » + 총 건수 + 페이지 크기. ②③의 admin-pager.js·pager.tag와 같은 마크업이다.
 */
export function Pager({ meta, onPage, onSize }: { meta: PageMeta; onPage: (page: number) => void; onSize: (size: number) => void }) {
  const last = Math.max(meta.totalPages, 1)
  const start = Math.floor((meta.page - 1) / 10) * 10 + 1
  const end = Math.min(start + 9, last)
  const item = (target: number, label: string, aria: string | undefined, off: boolean, active = false) => (
    <li className={`page-item${off ? ' disabled' : ''}${active ? ' active' : ''}`} key={`${label}-${target}`}>
      <a className="page-link" href="#" data-page={target} aria-label={aria}
         onClick={(e) => { e.preventDefault(); if (!off) onPage(target) }}>{label}</a>
    </li>
  )
  const numbers = Array.from({ length: end - start + 1 }, (_, i) => start + i)
  return (
    <div className="card-footer d-flex align-items-center flex-wrap gap-2" id="pager">
      <p className="m-0 text-secondary">총 <strong id="total-count">{meta.totalCount.toLocaleString('ko-KR')}</strong>건</p>
      <ul className="pagination m-0 ms-auto">
        {item(1, '«', '첫 페이지', meta.page <= 1)}
        {item(meta.page - 1, '‹', '이전 페이지', meta.page <= 1)}
        {numbers.map((n) => item(n, String(n), undefined, false, n === meta.page))}
        {item(meta.page + 1, '›', '다음 페이지', meta.page >= last)}
        {item(last, '»', '마지막 페이지', meta.page >= last)}
      </ul>
      <select className="form-select form-select-sm w-auto" id="page-size" aria-label="페이지 크기" value={meta.size}
              onChange={(e) => onSize(Number(e.target.value))}>
        {[10, 20, 50].map((s) => <option value={s} key={s}>{s}건씩</option>)}
      </select>
    </div>
  )
}

/** CMP-02 정렬 머리글. 누르면 오름차순, 다시 누르면 내림차순 (②③의 sortTh와 같은 마크업) */
export function SortTh({ field, label, sort, onSort }: { field: string; label: string; sort: string; onSort: (sort: string) => void }) {
  const dir = sort === `${field},asc` ? 'asc' : sort === `${field},desc` ? 'desc' : ''
  return (
    <th aria-sort={dir === 'asc' ? 'ascending' : dir === 'desc' ? 'descending' : 'none'}>
      <a className="sort-link text-reset" href="#" data-sort={field}
         onClick={(e) => { e.preventDefault(); onSort(dir === 'asc' ? `${field},desc` : `${field},asc`) }}>
        {label}<span className="sort-mark">{dir === 'asc' ? ' ▲' : dir === 'desc' ? ' ▼' : ''}</span>
      </a>
    </th>
  )
}
