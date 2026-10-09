import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import * as api from '../api/company'
import type { CompanyPage, CompanySearch } from '../api/company'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { DateRange } from '../components/DateRange'
import { Layout } from '../components/Layout'
import { Pager, SortTh } from '../components/Pager'
import { formatBizRegNo, formatDateTime } from '../lib/format'

const EMPTY = { companyNm: '', bizRegNo: '', ceoNm: '', statusCd: '', regDtFrom: '', regDtTo: '' }
const NO_PERM = '권한이 없습니다'

/** SCR-COM-01 기업 목록 (① React). ②③과 같은 마크업 */
export function CompanyListPage() {
  const me = useMe()
  const [form, setForm] = useState(EMPTY)
  const [query, setQuery] = useState<CompanySearch>({ page: 1, size: 20, sort: '' })
  const [result, setResult] = useState<CompanyPage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const fail = (e: unknown) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.')

  useEffect(() => { api.listCompanies(query).then(setResult).catch(fail) }, [query])

  const onSearch = (e: FormEvent) => { e.preventDefault(); setQuery({ ...query, ...form, page: 1 }) }
  const reset = (e: React.MouseEvent) => { e.preventDefault(); setForm(EMPTY); setQuery({ page: 1, size: query.size, sort: '' }) }
  const input = (name: 'companyNm' | 'bizRegNo' | 'ceoNm', label: string, placeholder = label) => (
    <div className="col-md-2"><input className="form-control" id={`search-${name}`} placeholder={placeholder} aria-label={label}
                                     value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></div>
  )
  const sort = query.sort ?? ''
  const onSort = (s: string) => setQuery({ ...query, sort: s, page: 1 })
  const canExcel = can(me, 'COMPANY', 'EXCEL')

  return (
    <Layout title="기업정보관리">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">기업 목록</h3>
          <div className="card-actions btn-list">
            <button type="button" className="btn btn-sm" id="btn-excel" disabled={!canExcel} title={canExcel ? undefined : NO_PERM}
                    onClick={() => { setError(null); api.downloadExcel(query).catch(fail) }}>엑셀</button>
            {can(me, 'COMPANY', 'CREATE')
              ? <Link className="btn btn-primary btn-sm" id="btn-company-create" to="/companies/new">등록</Link>
              : <button type="button" className="btn btn-primary btn-sm" id="btn-company-create" disabled title={NO_PERM}>등록</button>}
          </div>
        </div>
        <div className="card-body border-bottom">
          <form className="row g-2" id="company-search" onSubmit={onSearch}>
            {input('companyNm', '기업명')}
            {input('bizRegNo', '사업자등록번호', '사업자등록번호 (숫자)')}
            {input('ceoNm', '대표자명')}
            <div className="col-md-2">
              <select className="form-select" id="search-statusCd" aria-label="상태" value={form.statusCd} onChange={(e) => setForm({ ...form, statusCd: e.target.value })}>
                <option value="">상태 전체</option><option value="ACTIVE">정상</option><option value="SUSPENDED">정지</option>
              </select>
            </div>
            <div className="col-md-4">
              <DateRange id="regDt" label="등록일" from={form.regDtFrom} to={form.regDtTo}
                         onChange={(regDtFrom, regDtTo) => setForm({ ...form, regDtFrom, regDtTo })} />
            </div>
            <div className="col-12 d-flex gap-2 justify-content-end">
              <button type="submit" className="btn btn-primary">검색</button>
              <a className="btn" id="btn-search-reset" href="#" onClick={reset}>초기화</a>
            </div>
          </form>
        </div>
        <div className="table-responsive">
          <table className="table table-vcenter" id="company-table">
            <thead>
              <tr>
                <th>번호</th>
                <SortTh field="companyNm" label="기업명" sort={sort} onSort={onSort} />
                <th>사업자등록번호</th><th>대표자</th>
                <SortTh field="memberCnt" label="소속 회원 수" sort={sort} onSort={onSort} />
                <th>상태</th>
                <SortTh field="regDt" label="등록일시" sort={sort} onSort={onSort} />
              </tr>
            </thead>
            <tbody>
              {result?.items.map((c, i) => (
                <tr key={c.companyId}>
                  <td>{result.totalCount - (result.page - 1) * result.size - i}</td>
                  <td><Link to={`/companies/${c.companyId}`}>{c.companyNm}</Link></td>
                  <td>{formatBizRegNo(c.bizRegNo)}</td><td>{c.ceoNm}</td><td>{c.memberCnt}</td>
                  <td>{c.statusNm}</td><td>{formatDateTime(c.regDt)}</td>
                </tr>
              ))}
              {result && result.items.length === 0 && <tr><td colSpan={7} className="text-secondary text-center">조회된 데이터가 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
        {result && <Pager meta={result} onPage={(page) => setQuery({ ...query, page })} onSize={(size) => setQuery({ ...query, size, page: 1 })} />}
      </div>
    </Layout>
  )
}
