import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import * as api from '../api/role'
import type { RoleAdmin, RoleDetail, RolePermissionNode } from '../api/role'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can, type Action } from '../auth/permissions'
import { Layout, type NoticeState } from '../components/Layout'
import { Modal } from '../components/Modal'
import { formatDateTime } from '../lib/format'

const ACTIONS: { cd: Action; nm: string }[] = [
  { cd: 'READ', nm: '조회' }, { cd: 'CREATE', nm: '등록' }, { cd: 'UPDATE', nm: '수정' },
  { cd: 'DELETE', nm: '삭제' }, { cd: 'EXCEL', nm: '엑셀' }, { cd: 'PRIVACY', nm: '개인정보' },
]
const NO_PERM = '권한이 없습니다'

/** 메뉴 ID → 체크된 액션 */
type Grants = Record<number, Action[]>

function grantsOf(nodes: RolePermissionNode[], out: Grants = {}): Grants {
  nodes.forEach((n) => {
    if (n.granted.length) out[n.menuId] = [...n.granted]
    grantsOf(n.children, out)
  })
  return out
}

/**
 * SCR-ROL-02 역할 상세 (① React). 위쪽 역할 정보, 아래쪽 [권한] [관리자] 탭. ②③과 같은 마크업이다.
 * 권한 표 체크 규칙(BR-03): READ가 아닌 칸을 체크하면 READ도 체크, READ를 해제하면 같은 행 전체 해제.
 */
export function RoleDetailPage() {
  const me = useMe()
  const roleId = Number(useParams().roleId)
  const navigate = useNavigate()
  const perm = { create: can(me, 'ROLE', 'CREATE'), update: can(me, 'ROLE', 'UPDATE'), delete: can(me, 'ROLE', 'DELETE') }
  const [role, setRole] = useState<RoleDetail | null>(null)
  const [tree, setTree] = useState<RolePermissionNode[]>([])
  const [grants, setGrants] = useState<Grants>({})
  const [admins, setAdmins] = useState<RoleAdmin[]>([])
  const [tab, setTab] = useState<'permissions' | 'admins'>('permissions')
  const [error, setError] = useState<string | null>(null)
  const [copyOpen, setCopyOpen] = useState(false)
  const [copyForm, setCopyForm] = useState({ roleCd: '', roleNm: '' })
  const [copyError, setCopyError] = useState<string | null>(null)

  const errorMessage = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')

  const load = useCallback(async () => {
    const [r, t, a] = await Promise.all([api.getRole(roleId), api.getPermissions(roleId), api.getAdmins(roleId)])
    setRole(r); setTree(t); setGrants(grantsOf(t)); setAdmins(a)
  }, [roleId])
  useEffect(() => { load().catch((e) => setError(errorMessage(e))) }, [load])

  const toggle = (menuId: number, action: Action, checked: boolean, grantable: Action[]) => {
    const current = grants[menuId] ?? []
    let next: Action[]
    if (checked) {
      next = [...new Set([...current, action, ...(action !== 'READ' ? ['READ' as Action] : [])])]
    } else if (action === 'READ') {
      next = current.filter((a) => !grantable.includes(a))    // 바꿀 수 있는 칸은 모두 해제
    } else {
      next = current.filter((a) => a !== action)
    }
    setGrants({ ...grants, [menuId]: next })
  }

  const savePermissions = async () => {
    if (!role || !confirm(`이 역할을 가진 관리자 ${role.adminCnt}명에게 바로 반영됩니다. 저장하시겠습니까?`)) return
    const permissions = Object.entries(grants).filter(([, actions]) => actions.length)
      .map(([menuId, actions]) => ({ menuId: Number(menuId), actions }))
    try {
      await api.savePermissions(role.roleId, { permissions, modDt: role.modDt })
      await load()
      navigate(`/roles/${role.roleId}`, { replace: true, state: { notice: '권한이 저장되었습니다' } satisfies NoticeState })
    } catch (e) { setError(errorMessage(e)) }
  }

  const copy = async (e: FormEvent) => {
    e.preventDefault()
    if (!role) return
    setCopyError(null)
    try {
      const { roleId: newId } = await api.copyRole(role.roleId, copyForm.roleCd.trim(), copyForm.roleNm.trim())
      setCopyOpen(false)
      navigate(`/roles/${newId}`, { state: { notice: '복사되었습니다' } satisfies NoticeState })
    } catch (err) { setCopyError(errorMessage(err)) }
  }

  const remove = async () => {
    if (!role || !confirm('역할을 삭제하시겠습니까?')) return
    try {
      await api.deleteRole(role.roleId)
      navigate('/roles', { state: { notice: '삭제되었습니다' } satisfies NoticeState })
    } catch (e) { setError(errorMessage(e)) }
  }

  const editable = !!role?.editable && perm.update
  const permRows = (nodes: RolePermissionNode[]): ReactNode[] => nodes.flatMap((n) => {
    const folder = n.menuTypeCd === 'FOLDER'
    const checked = grants[n.menuId] ?? []
    return [
      <tr key={n.menuId} className={`${folder ? 'perm-folder' : 'perm-row'}${n.useYn === 'N' ? ' text-secondary' : ''}`} data-menu-id={n.menuId}>
        <td className={`perm-depth-${n.depth}${folder ? ' fw-bold' : ''}`}>{n.menuNm}</td>
        {ACTIONS.map((a) => (
          <td className="text-center" key={a.cd}>
            {n.actions.includes(a.cd) && (
              <input type="checkbox" className="form-check-input m-0 perm-check" id={`perm-${n.menuId}-${a.cd}`} data-action={a.cd}
                     aria-label={`${n.menuNm} ${a.nm}`} checked={checked.includes(a.cd)}
                     disabled={!editable || !n.grantableActions.includes(a.cd)}
                     onChange={(e) => toggle(n.menuId, a.cd, e.target.checked, n.grantableActions)} />
            )}
          </td>
        ))}
      </tr>,
      ...permRows(n.children),
    ]
  })

  if (!role) {
    return <Layout title="역할">{error && <div className="alert alert-danger" role="alert">{error}</div>}</Layout>
  }
  const sys = role.systemYn === 'Y'
  const permProps = (allowed: boolean) => (allowed ? {} : { disabled: true, title: NO_PERM })

  return (
    <Layout title="역할">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="card mb-3" id="role-info">
        <div className="card-header">
          <h3 className="card-title" id="role-title">
            {sys ? '🔒 ' : ''}{role.roleNm}
            {role.mine && <span className="badge bg-blue-lt ms-2">내 역할</span>}
          </h3>
          <div className="card-actions btn-list">
            <Link className="btn btn-sm" id="btn-list" to="/roles">목록</Link>
            {role.editable && (perm.update
              ? <Link className="btn btn-sm" id="btn-edit" to={`/roles/${role.roleId}/edit`}>수정</Link>
              : <button type="button" className="btn btn-sm" id="btn-edit" {...permProps(false)}>수정</button>)}
            <button type="button" className="btn btn-sm" id="btn-copy" {...permProps(perm.create)}
                    onClick={() => { setCopyError(null); setCopyForm({ roleCd: '', roleNm: '' }); setCopyOpen(true) }}>복사</button>
            {!sys && role.adminCnt === 0 && (
              <button type="button" className="btn btn-sm btn-ghost-danger" id="btn-delete" {...permProps(perm.delete)} onClick={remove}>삭제</button>
            )}
          </div>
        </div>
        <div className="card-body">
          {!role.editable && (
            <div className="alert alert-info" role="note">{sys ? '시스템 역할' : '내가 가진 역할'}은 수정하거나 권한을 바꿀 수 없습니다.</div>
          )}
          <dl className="row mb-0">
            <dt className="col-sm-2">역할 코드</dt><dd className="col-sm-4" id="role-roleCd">{role.roleCd}</dd>
            <dt className="col-sm-2">사용 여부</dt><dd className="col-sm-4" id="role-useYn">{role.useYn === 'Y' ? '사용' : '사용 안 함'}</dd>
            <dt className="col-sm-2">설명</dt><dd className="col-sm-10" id="role-description">{role.description || '-'}</dd>
            <dt className="col-sm-2">관리자 수</dt><dd className="col-sm-4" id="role-adminCnt">{role.adminCnt}</dd>
            <dt className="col-sm-2">수정</dt><dd className="col-sm-4">{role.modNm || '-'} · {formatDateTime(role.modDt)}</dd>
          </dl>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <ul className="nav nav-tabs card-header-tabs" role="tablist">
            <li className="nav-item" role="presentation">
              <a className={`nav-link${tab === 'permissions' ? ' active' : ''}`} id="tab-permissions" href="#pane-permissions" role="tab"
                 onClick={(e) => { e.preventDefault(); setTab('permissions') }}>권한</a>
            </li>
            <li className="nav-item" role="presentation">
              <a className={`nav-link${tab === 'admins' ? ' active' : ''}`} id="tab-admins" href="#pane-admins" role="tab"
                 onClick={(e) => { e.preventDefault(); setTab('admins') }}>관리자 ({role.adminCnt})</a>
            </li>
          </ul>
        </div>
        <div className="tab-content">
          <div className={`tab-pane${tab === 'permissions' ? ' active show' : ''}`} id="pane-permissions" role="tabpanel">
            <div className="table-responsive">
              <table className="table table-vcenter table-sm mb-0" id="perm-table">
                <thead><tr><th>메뉴</th>{ACTIONS.map((a) => <th className="text-center" key={a.cd}>{a.nm}</th>)}</tr></thead>
                <tbody>{permRows(tree)}</tbody>
              </table>
            </div>
            {role.editable && (
              <div className="card-footer text-end" id="perm-footer">
                <button type="button" className="btn btn-primary" id="btn-perm-save" {...permProps(perm.update)} onClick={savePermissions}>권한 저장</button>
              </div>
            )}
          </div>
          <div className={`tab-pane${tab === 'admins' ? ' active show' : ''}`} id="pane-admins" role="tabpanel">
            <div className="table-responsive">
              <table className="table table-vcenter mb-0" id="admin-table">
                <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서</th><th>상태</th><th>부여일시</th></tr></thead>
                <tbody>
                  {admins.map((a) => (
                    <tr key={a.adminId}><td><Link to={`/admins/${a.adminId}`}>{a.loginId}</Link></td><td>{a.adminNm}</td><td>{a.deptNm || '-'}</td><td>{a.statusNm}</td><td>{formatDateTime(a.grantedDt)}</td></tr>
                  ))}
                  {admins.length === 0 && <tr><td colSpan={5} className="text-secondary text-center">이 역할을 가진 관리자가 없습니다</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <Modal id="copy-modal" open={copyOpen} title="역할 복사" onClose={() => setCopyOpen(false)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="copy-form" className="btn btn-primary" id="btn-copy-save">복사</button></>}>
        <form className="modal-body" id="copy-form" noValidate onSubmit={copy}>
          <p className="text-secondary" id="copy-guide">{role.roleNm}의 설명·권한을 그대로 가진 새 역할을 만듭니다.</p>
          <div className="mb-3"><label className="form-label required" htmlFor="copy-roleCd">새 역할 코드</label>
            <input className="form-control" id="copy-roleCd" maxLength={50} required placeholder="영문 대문자·숫자·_" value={copyForm.roleCd} onChange={(e) => setCopyForm({ ...copyForm, roleCd: e.target.value })} /></div>
          <div className="mb-3"><label className="form-label required" htmlFor="copy-roleNm">새 역할명</label>
            <input className="form-control" id="copy-roleNm" maxLength={100} required value={copyForm.roleNm} onChange={(e) => setCopyForm({ ...copyForm, roleNm: e.target.value })} /></div>
          <div id="copy-message">{copyError && <div className="alert alert-danger" role="alert">{copyError}</div>}</div>
        </form>
      </Modal>
    </Layout>
  )
}
