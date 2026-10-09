import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import * as api from '../api/permission'
import type { MenuRoleGrants, PermissionMenuNode } from '../api/permission'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can, type Action } from '../auth/permissions'
import { Layout } from '../components/Layout'

const ACTION_NM: Record<Action, string> = { READ: '조회', CREATE: '등록', UPDATE: '수정', DELETE: '삭제', EXCEL: '엑셀', PRIVACY: '개인정보' }
const NO_PERM = '권한이 없습니다'

/** 역할 ID → 체크된 액션 */
type Checks = Record<number, Action[]>

const key = (actions: Action[] | undefined) => [...(actions ?? [])].sort().join(',')

/**
 * SCR-PRM-01 권한관리 (① React). 왼쪽 메뉴 트리, 오른쪽 선택한 메뉴의 역할 × 액션 표. ②③과 같은 마크업이다.
 * 체크 규칙(BR-02): READ가 아닌 칸을 체크하면 READ도 체크, READ를 해제하면 같은 행 전체 해제. 바뀐 역할만 저장한다.
 */
export function PermissionPage() {
  const me = useMe()
  const canUpdate = can(me, 'PERMISSION', 'UPDATE')
  const [tree, setTree] = useState<PermissionMenuNode[]>([])
  const [grants, setGrants] = useState<MenuRoleGrants | null>(null)
  const [initial, setInitial] = useState<Checks>({})
  const [checks, setChecks] = useState<Checks>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const errorMessage = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')
  useEffect(() => { api.getTree().then(setTree).catch((e) => setError(errorMessage(e))) }, [])

  const changes = grants
    ? grants.roles.filter((r) => r.systemYn !== 'Y' && key(checks[r.roleId]) !== key(initial[r.roleId]))
      .map((r) => ({ roleId: r.roleId, actions: checks[r.roleId] ?? [] }))
    : []

  const load = async (menuId: number) => {
    const g = await api.getMenuGrants(menuId)
    const c: Checks = Object.fromEntries(g.roles.map((r) => [r.roleId, [...r.granted]]))
    setGrants(g); setInitial(c); setChecks(c); setInfo(null)
  }

  const select = (menuId: number) => {
    // 저장하지 않은 변경이 있으면 다른 메뉴로 가기 전에 확인한다
    if (changes.length && !confirm('저장하지 않은 변경이 있습니다. 이동하시겠습니까?')) return
    load(menuId).catch((e) => setError(errorMessage(e)))
  }

  const toggle = (roleId: number, action: Action, checked: boolean) => {
    const current = checks[roleId] ?? []
    const grantable = grants?.grantableActions ?? []
    let next: Action[]
    if (checked) next = [...new Set<Action>([...current, action, ...(action !== 'READ' ? ['READ' as Action] : [])])]
    else if (action === 'READ') next = current.filter((a) => !grantable.includes(a))
    else next = current.filter((a) => a !== action)
    setChecks({ ...checks, [roleId]: next })
  }

  const save = async () => {
    if (!grants) return
    if (!changes.length) { setInfo('바뀐 내용이 없습니다.'); return }
    if (!confirm(`역할 ${changes.length}개의 권한이 바뀝니다. 저장하시겠습니까?`)) return
    try {
      await api.saveMenuGrants(grants.menu.menuId, { roles: changes })
      setNotice('권한이 저장되었습니다'); setTimeout(() => setNotice(null), 3000)
      await load(grants.menu.menuId)
    } catch (e) { setError(errorMessage(e)) }
  }

  const renderTree = (nodes: PermissionMenuNode[]): ReactNode => (
    <ul className="menu-tree">
      {nodes.map((m) => (
        <li className="menu-node" data-menu-id={m.menuId} key={m.menuId}>
          <div className={['menu-node-row', grants?.menu.menuId === m.menuId ? 'is-selected' : '', m.useYn === 'N' ? 'is-unused' : ''].filter(Boolean).join(' ')}>
            {m.menuTypeCd === 'PAGE'
              ? <a className="menu-node-link" href="#" onClick={(e) => { e.preventDefault(); select(m.menuId) }}>{m.menuNm}</a>
              : <span className="menu-node-link fw-bold">{m.menuNm}</span>}
          </div>
          {m.children.length > 0 && renderTree(m.children)}
        </li>
      ))}
    </ul>
  )

  return (
    <Layout title="권한">
      <div id="notice-area">
        {notice && <div className="alert alert-success" role="status">{notice}</div>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
      </div>
      <div className="mb-3 text-end"><Link className="btn btn-sm" id="btn-effective" to="/permissions/admins">관리자별 최종 권한</Link></div>
      <div className="row row-cards">
        <div className="col-lg-4">
          <div className="card" id="perm-menu-card">
            <div className="card-header"><h3 className="card-title">메뉴</h3></div>
            <div className="card-body" id="perm-menu-tree">{renderTree(tree)}</div>
          </div>
        </div>
        <div className="col-lg-8" id="grant-col">
          {!grants ? (
            <div className="card"><div className="card-body text-secondary">왼쪽에서 화면 메뉴를 선택하세요.</div></div>
          ) : (
            <div className="card" id="grant-card">
              <div className="card-header">
                <h3 className="card-title" id="grant-title">{grants.menu.menuNm} <span className="text-secondary ms-1">({grants.menu.menuCd})</span></h3>
              </div>
              <div className="table-responsive">
                <table className="table table-vcenter mb-0" id="grant-table">
                  <thead><tr><th>역할</th>{grants.menu.actions.map((a) => <th className="text-center" key={a}>{ACTION_NM[a]}</th>)}</tr></thead>
                  <tbody>
                    {grants.roles.map((r) => {
                      const system = r.systemYn === 'Y'
                      return (
                        <tr className={`grant-row${r.useYn === 'N' ? ' text-secondary' : ''}`} data-role-id={r.roleId} key={r.roleId}>
                          <td>
                            {r.roleNm}{system ? ' 🔒' : ''}
                            {r.mine && <span className="badge bg-blue-lt ms-1">내 역할</span>}
                            {r.useYn === 'N' && <span className="badge bg-secondary-lt ms-1">사용 안 함</span>}
                          </td>
                          {grants.menu.actions.map((a) => (
                            <td className="text-center" key={a}>
                              {system ? <span className="perm-all" title="모든 권한">■</span> : (
                                <input type="checkbox" className="form-check-input m-0 perm-check" id={`grant-${r.roleId}-${a}`} data-action={a}
                                       aria-label={`${r.roleNm} ${ACTION_NM[a]}`} checked={(checks[r.roleId] ?? []).includes(a)}
                                       disabled={!canUpdate || !r.editable || !grants.grantableActions.includes(a)}
                                       onChange={(e) => toggle(r.roleId, a, e.target.checked)} />
                              )}
                            </td>
                          ))}
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
              <div className="card-footer">
                <div id="grant-message">{info && <div className="alert alert-info" role="note">{info}</div>}</div>
                <div className="text-end">
                  <button type="button" className="btn btn-primary" id="btn-grant-save" disabled={!canUpdate} title={canUpdate ? undefined : NO_PERM} onClick={save}>저장</button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  )
}
