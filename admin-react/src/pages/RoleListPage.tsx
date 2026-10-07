import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import * as api from '../api/role'
import type { RoleListItem } from '../api/role'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout } from '../components/Layout'

/** SCR-ROL-01 역할 목록 (① React). ②③과 같은 마크업 */
export function RoleListPage() {
  const me = useMe()
  const [roles, setRoles] = useState<RoleListItem[]>([])
  const [keyword, setKeyword] = useState('')
  const [useYn, setUseYn] = useState('')

  const load = async () => setRoles(await api.listRoles(keyword.trim() || undefined, useYn || undefined))
  useEffect(() => { void load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const onSearch = (e: FormEvent) => { e.preventDefault(); void load() }
  const canCreate = can(me, 'ROLE', 'CREATE')

  return (
    <Layout title="역할">
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">역할 목록</h3>
          <div className="card-actions">
            {canCreate
              ? <Link className="btn btn-primary btn-sm" id="btn-role-create" to="/roles/new">등록</Link>
              : <button type="button" className="btn btn-primary btn-sm" id="btn-role-create" disabled title="권한이 없습니다">등록</button>}
          </div>
        </div>
        <div className="card-body border-bottom">
          <form className="row g-2" id="role-search" onSubmit={onSearch}>
            <div className="col"><input className="form-control" id="search-keyword" placeholder="역할 코드 / 역할명" value={keyword} onChange={(e) => setKeyword(e.target.value)} /></div>
            <div className="col-auto">
              <select className="form-select" id="search-useYn" value={useYn} onChange={(e) => setUseYn(e.target.value)}>
                <option value="">전체</option><option value="Y">사용</option><option value="N">사용 안 함</option>
              </select>
            </div>
            <div className="col-auto"><button type="submit" className="btn">검색</button></div>
          </form>
        </div>
        <div className="table-responsive">
          <table className="table table-vcenter" id="role-table">
            <thead><tr><th>역할 코드</th><th>역할명</th><th>설명</th><th className="text-center">관리자 수</th><th className="text-center">사용</th></tr></thead>
            <tbody>
              {roles.map((r) => (
                <tr key={r.roleId}>
                  <td><Link to={`/roles/${r.roleId}`}>{r.systemYn === 'Y' ? '🔒 ' : ''}{r.roleCd}</Link></td>
                  <td>{r.roleNm}</td><td>{r.description}</td>
                  <td className="text-center">{r.adminCnt}</td><td className="text-center">{r.useYn === 'Y' ? '사용' : '사용 안 함'}</td>
                </tr>
              ))}
              {roles.length === 0 && <tr><td colSpan={5} className="text-secondary text-center">조회된 데이터가 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  )
}
