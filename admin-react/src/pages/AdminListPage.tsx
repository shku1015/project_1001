import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import * as api from '../api/admin'
import type { AdminPage, AdminRoleOption, AdminSearch } from '../api/admin'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout } from '../components/Layout'
import { Pager, SortTh } from '../components/Pager'
import { formatDateTime } from '../lib/format'

const EMPTY = { loginId: '', adminNm: '', deptNm: '', roleId: '', statusCd: '' }

/** SCR-ADM-01 관리자 목록 (① React). ②③과 같은 마크업 */
export function AdminListPage() {
  const me = useMe()
  const [form, setForm] = useState(EMPTY)
  const [query, setQuery] = useState<AdminSearch>({ page: 1, size: 20, sort: '' })
  const [result, setResult] = useState<AdminPage | null>(null)
  const [roles, setRoles] = useState<AdminRoleOption[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => { api.getRoleOptions().then(setRoles).catch(() => setRoles([])) }, [])
  useEffect(() => {
    api.listAdmins(query).then(setResult).catch((e) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.'))
  }, [query])

  const onSearch = (e: FormEvent) => { e.preventDefault(); setQuery({ ...query, ...form, page: 1 }) }
  const reset = (e: React.MouseEvent) => { e.preventDefault(); setForm(EMPTY); setQuery({ page: 1, size: query.size, sort: '' }) }
  const input = (name: 'loginId' | 'adminNm' | 'deptNm', label: string) => (
    <div className="col-md-2"><input className="form-control" id={`search-${name}`} placeholder={label} aria-label={label}
                                     value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></div>
  )
  const sort = query.sort ?? ''
  const onSort = (s: string) => setQuery({ ...query, sort: s, page: 1 })

  return (
    <Layout title="관리자">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">관리자 목록</h3>
          <div className="card-actions">
            {can(me, 'ADMIN', 'CREATE')
              ? <Link className="btn btn-primary btn-sm" id="btn-admin-create" to="/admins/new">등록</Link>
              : <button type="button" className="btn btn-primary btn-sm" id="btn-admin-create" disabled title="권한이 없습니다">등록</button>}
          </div>
        </div>
        <div className="card-body border-bottom">
          <form className="row g-2" id="admin-search" onSubmit={onSearch}>
            {input('loginId', '로그인 아이디')}
            {input('adminNm', '이름')}
            {input('deptNm', '부서')}
            <div className="col-md-2">
              <select className="form-select" id="search-roleId" aria-label="역할" value={form.roleId} onChange={(e) => setForm({ ...form, roleId: e.target.value })}>
                <option value="">역할 전체</option>
                {roles.map((r) => <option value={r.roleId} key={r.roleId}>{r.roleNm}</option>)}
              </select>
            </div>
            <div className="col-md-2">
              <select className="form-select" id="search-statusCd" aria-label="상태" value={form.statusCd} onChange={(e) => setForm({ ...form, statusCd: e.target.value })}>
                <option value="">상태 전체</option><option value="ACTIVE">사용</option><option value="LOCKED">잠금</option><option value="DISABLED">사용중지</option>
              </select>
            </div>
            <div className="col-md-2 d-flex gap-2">
              <button type="submit" className="btn btn-primary">검색</button>
              <a className="btn" id="btn-search-reset" href="#" onClick={reset}>초기화</a>
            </div>
          </form>
        </div>
        <div className="table-responsive">
          <table className="table table-vcenter" id="admin-table">
            <thead>
              <tr>
                <th>번호</th>
                <SortTh field="loginId" label="로그인 아이디" sort={sort} onSort={onSort} />
                <SortTh field="adminNm" label="이름" sort={sort} onSort={onSort} />
                <th>부서</th><th>역할</th><th>상태</th>
                <SortTh field="lastLoginDt" label="마지막 로그인" sort={sort} onSort={onSort} />
                <SortTh field="regDt" label="등록일시" sort={sort} onSort={onSort} />
              </tr>
            </thead>
            <tbody>
              {result?.items.map((a, i) => (
                <tr key={a.adminId}>
                  <td>{result.totalCount - (result.page - 1) * result.size - i}</td>
                  <td><Link to={`/admins/${a.adminId}`}>{a.loginId}</Link></td>
                  <td>{a.adminNm}</td><td>{a.deptNm || '-'}</td>
                  <td>{a.roles.length ? `${a.roles[0].roleNm}${a.roles.length > 1 ? ` 외 ${a.roles.length - 1}` : ''}` : '-'}</td>
                  <td>{a.statusNm}</td><td>{formatDateTime(a.lastLoginDt)}</td><td>{formatDateTime(a.regDt)}</td>
                </tr>
              ))}
              {result && result.items.length === 0 && <tr><td colSpan={8} className="text-secondary text-center">조회된 데이터가 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
        {result && <Pager meta={result} onPage={(page) => setQuery({ ...query, page })} onSize={(size) => setQuery({ ...query, size, page: 1 })} />}
      </div>
    </Layout>
  )
}
