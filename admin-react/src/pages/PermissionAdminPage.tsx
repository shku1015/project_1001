import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import * as adminApi from '../api/admin'
import type { AdminListItem } from '../api/admin'
import * as api from '../api/permission'
import type { AdminEffectivePermissions } from '../api/permission'
import { ApiError } from '../api/client'
import type { Action } from '../auth/permissions'
import { Layout } from '../components/Layout'

const ACTIONS: Action[] = ['READ', 'CREATE', 'UPDATE', 'DELETE', 'EXCEL', 'PRIVACY']

/**
 * SCR-PRM-02 관리자별 최종 권한 (① React). 위쪽 관리자 선택(CMP-12), 아래쪽 메뉴 × 액션 표 (조회 전용).
 * 고른 관리자는 주소의 ?adminId= 로 남긴다. ②③과 같은 마크업이다.
 */
export function PermissionAdminPage() {
  const [params, setParams] = useSearchParams()
  const adminId = params.get('adminId')
  const [keyword, setKeyword] = useState('')
  const [candidates, setCandidates] = useState<AdminListItem[] | null>(null)
  const [effective, setEffective] = useState<AdminEffectivePermissions | null>(null)
  const [error, setError] = useState<string | null>(null)
  const fail = (e: unknown) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.')

  useEffect(() => {
    if (adminId) api.getAdminPermissions(Number(adminId)).then(setEffective).catch(fail)
  }, [adminId])

  const search = (e: FormEvent) => {
    e.preventDefault()
    if (!keyword.trim()) return
    adminApi.listAdmins({ keyword: keyword.trim(), size: 10 }).then((r) => setCandidates(r.items)).catch(fail)
  }

  return (
    <Layout title="권한">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="mb-3"><Link className="btn btn-sm" id="btn-back" to="/permissions">메뉴 기준 권한관리</Link></div>
      <div className="card mb-3" id="admin-picker">
        <div className="card-header"><h3 className="card-title">관리자 선택</h3></div>
        <div className="card-body">
          <form className="d-flex gap-2" id="picker-form" onSubmit={search}>
            <input className="form-control" id="picker-keyword" placeholder="이름 또는 로그인 아이디" aria-label="이름 또는 로그인 아이디"
                   value={keyword} onChange={(e) => setKeyword(e.target.value)} />
            <button type="submit" className="btn btn-primary text-nowrap">검색</button>
          </form>
        </div>
        {candidates && (
          <div className="table-responsive">
            <table className="table table-vcenter mb-0" id="picker-table">
              <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서</th><th>상태</th><th /></tr></thead>
              <tbody>
                {candidates.map((a) => (
                  <tr key={a.adminId}>
                    <td>{a.loginId}</td><td>{a.adminNm}</td><td>{a.deptNm || '-'}</td><td>{a.statusNm}</td>
                    <td className="text-end"><a className="btn btn-sm btn-pick" href="#"
                                                onClick={(e) => { e.preventDefault(); setParams({ adminId: String(a.adminId) }) }}>선택</a></td>
                  </tr>
                ))}
                {candidates.length === 0 && <tr><td colSpan={5} className="text-secondary text-center">조회된 데이터가 없습니다</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {adminId && effective && (
        <div className="card" id="effective-card">
          <div className="card-header">
            <h3 className="card-title" id="effective-title">{effective.admin.adminNm} <span className="text-secondary ms-1">({effective.admin.loginId})</span>의 최종 권한</h3>
          </div>
          {effective.superAdmin ? (
            <div className="card-body"><div className="alert alert-info mb-0" role="note">슈퍼관리자는 모든 권한을 가집니다.</div></div>
          ) : (
            <div className="table-responsive">
              <table className="table table-vcenter table-sm mb-0" id="effective-table">
                <thead><tr><th>메뉴</th>{['조회', '등록', '수정', '삭제', '엑셀', '개인정보'].map((l) => <th className="text-center" key={l}>{l}</th>)}</tr></thead>
                <tbody>
                  {effective.menus.map((m) => (
                    <tr data-menu-id={m.menuId} key={m.menuId}>
                      <td className={`perm-depth-${m.depth}${m.menuTypeCd === 'FOLDER' ? ' fw-bold' : ''}`}>{m.menuNm}</td>
                      {ACTIONS.map((a) => (
                        <td className="text-center" data-action={a} key={a}>
                          {m.actions.includes(a) && (m.granted[a]
                            ? <span className="perm-granted" title={m.granted[a]?.join(', ')}>●</span>
                            : <span className="text-secondary">-</span>)}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </Layout>
  )
}
