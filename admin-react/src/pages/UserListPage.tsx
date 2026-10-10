import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import * as api from '../api/user'
import type { UserPage, UserSearch } from '../api/user'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { DateRange } from '../components/DateRange'
import { Layout } from '../components/Layout'
import { Pager, SortTh } from '../components/Pager'
import { formatDateTime, formatMobile } from '../lib/format'

const EMPTY = { userTypeCd: '', loginId: '', userNm: '', email: '', mobileNo: '', companyNm: '', statusCd: '', joinPath: '', joinDtFrom: '', joinDtTo: '' }
const NO_PERM = '권한이 없습니다'

/** 휴대폰 번호: 숫자면 하이픈을 넣고, 가린 값(010-****-5678)은 그대로 */
const mobile = (v: string | null) => (v && !v.includes('*') ? formatMobile(v) : v || '-')

/** SCR-USR-01 회원 목록 (① React). 개인정보는 화면 마스킹 설정대로 가려서 온다. ②③과 같은 마크업 */
export function UserListPage() {
  const me = useMe()
  // 기업 상세의 [전체 보기]는 ?companyId= 로 들어온다
  const companyId = useSearchParams()[0].get('companyId') ?? undefined
  const [form, setForm] = useState(EMPTY)
  const [query, setQuery] = useState<UserSearch>({ page: 1, size: 20, sort: '', companyId })
  const [result, setResult] = useState<UserPage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const fail = (e: unknown) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.')

  useEffect(() => { api.listUsers(query).then(setResult).catch(fail) }, [query])

  const onSearch = (e: FormEvent) => { e.preventDefault(); setQuery({ ...query, ...form, page: 1 }) }
  const reset = (e: React.MouseEvent) => { e.preventDefault(); setForm(EMPTY); setQuery({ page: 1, size: query.size, sort: '', companyId }) }
  const input = (name: 'loginId' | 'userNm' | 'email' | 'mobileNo' | 'companyNm', label: string, placeholder = label) => (
    <div className="col-md-2"><input className="form-control" id={`search-${name}`} placeholder={placeholder} aria-label={label}
                                     value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></div>
  )
  const sort = query.sort ?? ''
  const onSort = (s: string) => setQuery({ ...query, sort: s, page: 1 })
  const canExcel = can(me, 'USER', 'EXCEL')
  const canCreate = can(me, 'USER', 'CREATE') && can(me, 'USER', 'PRIVACY')
  const select = (name: 'userTypeCd' | 'statusCd' | 'joinPath', label: string, options: [string, string][]) => (
    <div className="col-md-2">
      <select className="form-select" id={`search-${name}`} aria-label={label} value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })}>
        {options.map(([v, t]) => <option value={v} key={v}>{t}</option>)}
      </select>
    </div>
  )

  return (
    <Layout title="사용자관리">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">회원 목록</h3>
          <div className="card-actions btn-list">
            <button type="button" className="btn btn-sm" id="btn-excel" disabled={!canExcel} title={canExcel ? undefined : NO_PERM}
                    onClick={() => { setError(null); api.downloadExcel(query).catch(fail) }}>엑셀</button>
            {canCreate
              ? <Link className="btn btn-primary btn-sm" id="btn-user-create" to="/users/new">등록</Link>
              : <button type="button" className="btn btn-primary btn-sm" id="btn-user-create" disabled title={NO_PERM}>등록</button>}
          </div>
        </div>
        <div className="card-body border-bottom">
          <form className="row g-2" id="user-search" onSubmit={onSearch}>
            {select('userTypeCd', '회원 구분', [['', '구분 전체'], ['PERSONAL', '개인'], ['CORPORATE', '기업']])}
            {input('loginId', '로그인 아이디')}
            {input('userNm', '이름')}
            {input('email', '이메일')}
            {input('mobileNo', '휴대폰 번호', '휴대폰 번호 (숫자)')}
            {input('companyNm', '소속 기업')}
            {select('statusCd', '상태', [['', '상태 전체'], ['ACTIVE', '정상'], ['DORMANT', '휴면'], ['SUSPENDED', '정지'], ['WITHDRAWN', '탈퇴']])}
            {select('joinPath', '가입 경로', [['', '가입 경로 전체'], ['USER_SERVICE', '사용자 서비스'], ['ADMIN', '관리자 등록']])}
            <div className="col-md-4">
              <DateRange id="joinDt" label="가입일" from={form.joinDtFrom} to={form.joinDtTo}
                         onChange={(joinDtFrom, joinDtTo) => setForm({ ...form, joinDtFrom, joinDtTo })} />
            </div>
            <div className="col-12 d-flex gap-2 justify-content-end">
              <button type="submit" className="btn btn-primary">검색</button>
              <a className="btn" id="btn-search-reset" href="#" onClick={reset}>초기화</a>
            </div>
          </form>
        </div>
        <div className="table-responsive">
          <table className="table table-vcenter" id="user-table">
            <thead>
              <tr>
                <th>번호</th><th>구분</th>
                <SortTh field="loginId" label="로그인 아이디" sort={sort} onSort={onSort} />
                <SortTh field="userNm" label="이름" sort={sort} onSort={onSort} />
                <th>이메일</th><th>휴대폰 번호</th><th>소속 기업</th><th>상태</th>
                <SortTh field="joinDt" label="가입일시" sort={sort} onSort={onSort} />
              </tr>
            </thead>
            <tbody>
              {result?.items.map((u, i) => (
                <tr key={u.userId}>
                  <td>{result.totalCount - (result.page - 1) * result.size - i}</td><td>{u.userTypeNm}</td>
                  <td><Link to={`/users/${u.userId}`}>{u.loginId}</Link></td><td>{u.userNm}</td><td>{u.email}</td>
                  <td>{mobile(u.mobileNo)}</td><td>{u.companyNm || '-'}</td><td>{u.statusNm}</td><td>{formatDateTime(u.joinDt)}</td>
                </tr>
              ))}
              {result && result.items.length === 0 && <tr><td colSpan={9} className="text-secondary text-center">조회된 데이터가 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
        {result && <Pager meta={result} onPage={(page) => setQuery({ ...query, page })} onSize={(size) => setQuery({ ...query, size, page: 1 })} />}
      </div>
    </Layout>
  )
}
