import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react'
import Sortable from 'sortablejs'
import * as api from '../api/menu'
import type { MenuAdminNode, MenuDetail } from '../api/menu'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can, type Action } from '../auth/permissions'
import { Layout } from '../components/Layout'
import { Modal } from '../components/Modal'

const ACTIONS: { cd: Action; nm: string }[] = [
  { cd: 'READ', nm: '조회' }, { cd: 'CREATE', nm: '등록' }, { cd: 'UPDATE', nm: '수정' },
  { cd: 'DELETE', nm: '삭제' }, { cd: 'EXCEL', nm: '엑셀' }, { cd: 'PRIVACY', nm: '개인정보열람' },
]
const NO_PERM = '권한이 없습니다'

/** 상위 메뉴 ID('' = 최상위) → 하위 메뉴 ID 순서 */
type Order = Record<string, number[]>
type MenuForm = { menuCd: string; menuNm: string; menuTypeCd: 'FOLDER' | 'PAGE'; menuUrl: string; actions: Action[]; icon: string; useYn: 'Y' | 'N' }

function orderOf(tree: MenuAdminNode[]): Order {
  const result: Order = {}
  const walk = (key: string, nodes: MenuAdminNode[]) => {
    result[key] = nodes.map((n) => n.menuId)
    nodes.forEach((n) => { if (n.children.length) walk(String(n.menuId), n.children) })
  }
  walk('', tree)
  return result
}

function flatten(nodes: MenuAdminNode[]): MenuAdminNode[] {
  return nodes.flatMap((n) => [n, ...flatten(n.children)])
}

/**
 * SCR-MNU-01 메뉴관리 (① React). ②③과 같은 마크업이다.
 * 순서는 드래그(SortableJS)·▲▼로 같은 상위 메뉴 안에서만 바꾸고 [순서 저장] 때 보낸다 (ADR-0015).
 * SortableJS가 옮긴 DOM은 되돌리고 순서 상태만 바꿔 React가 다시 그리게 한다.
 */
export function MenuPage() {
  const me = useMe()
  const perm = { create: can(me, 'MENU', 'CREATE'), update: can(me, 'MENU', 'UPDATE'), delete: can(me, 'MENU', 'DELETE') }
  const [tree, setTree] = useState<MenuAdminNode[]>([])
  const [original, setOriginal] = useState<Order>({})
  const [order, setOrder] = useState<Order>({})
  const [mode, setMode] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<MenuDetail | null>(null)
  const [createParent, setCreateParent] = useState<MenuAdminNode | null>(null)
  const [form, setForm] = useState<MenuForm | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [pageError, setPageError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [moveOpen, setMoveOpen] = useState(false)
  const [moveTarget, setMoveTarget] = useState('')
  const treeRef = useRef<HTMLDivElement>(null)

  const byId = useMemo(() => new Map(flatten(tree).map((n) => [n.menuId, n])), [tree])
  const dirty = Object.keys(order).some((k) => order[k].join(',') !== (original[k] ?? []).join(','))

  const toast = (message: string) => { setNotice(message); setTimeout(() => setNotice(null), 3000) }
  const errorMessage = (e: unknown) => (e instanceof ApiError ? e.fieldErrors[0]?.message ?? e.displayMessage : '처리할 수 없습니다.')

  const loadTree = async () => {
    const t = await api.getTree()
    setTree(t)
    const o = orderOf(t)
    setOriginal(o)
    setOrder(o)
  }
  useEffect(() => { loadTree().catch((e) => setPageError(errorMessage(e))) }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // 같은 상위 메뉴 안에서만 끌어 옮긴다. 옮긴 DOM은 되돌리고 상태로 다시 그린다
  useEffect(() => {
    if (!perm.update || !treeRef.current) return
    const instances = Array.from(treeRef.current.querySelectorAll<HTMLElement>('ul.menu-tree')).map((ul) =>
      Sortable.create(ul, {
        group: `menu-parent-${ul.dataset.parentId || 'root'}`,
        handle: '.drag-handle',
        draggable: 'li.menu-node',
        animation: 150,
        onEnd: (evt) => {
          const { from, item, oldIndex, newIndex } = evt
          if (oldIndex === undefined || newIndex === undefined || oldIndex === newIndex) return
          from.removeChild(item)
          from.insertBefore(item, from.children[oldIndex] ?? null)
          const key = from.dataset.parentId ?? ''
          setOrder((prev) => {
            const ids = [...prev[key]]
            const [moved] = ids.splice(oldIndex, 1)
            ids.splice(newIndex, 0, moved)
            return { ...prev, [key]: ids }
          })
        },
      }))
    return () => instances.forEach((s) => s.destroy())
  }, [tree, perm.update])

  // ----- 선택·등록 -----
  const select = async (menuId: number) => {
    const m = await api.getMenu(menuId)
    setSelected(m); setMode('edit'); setCreateParent(null); setFormError(null)
    setForm({ menuCd: m.menuCd, menuNm: m.menuNm, menuTypeCd: m.menuTypeCd, menuUrl: m.menuUrl ?? '', actions: m.actions, icon: m.icon ?? '', useYn: m.useYn })
  }

  const startCreate = (parent: MenuAdminNode | null) => {
    const depth = parent ? parent.depth + 1 : 1
    setSelected(null); setMode('create'); setCreateParent(parent); setFormError(null)
    setForm({ menuCd: '', menuNm: '', menuTypeCd: depth === 3 ? 'PAGE' : 'FOLDER', menuUrl: '', actions: ['READ'], icon: '', useYn: 'Y' })
  }

  // ----- 저장 -----
  const save = async (e: FormEvent) => {
    e.preventDefault()
    if (!form) return
    setFormError(null)
    if (dirty && !confirm('저장하지 않은 순서 변경은 사라집니다. 계속하시겠습니까?')) return
    const page = form.menuTypeCd === 'PAGE'
    const common = {
      menuNm: form.menuNm.trim(),
      menuUrl: page ? form.menuUrl.trim() || null : null,
      actions: page ? form.actions : [],
      icon: form.icon.trim() || null,
      useYn: form.useYn,
    }
    try {
      if (mode === 'create') {
        const { menuId } = await api.createMenu({ ...common, parentMenuId: createParent?.menuId ?? null, menuCd: form.menuCd.trim(), menuTypeCd: form.menuTypeCd })
        toast('등록되었습니다')
        await loadTree(); await select(menuId)
        return
      }
      if (!selected) return
      const body = { ...common, modDt: selected.modDt }
      // 액션을 빼서 회수되는 역할이 있으면 먼저 확인한다 (BR-07)
      const { revokedRoles } = await api.updateMenu(selected.menuId, body, true)
      if (revokedRoles.length) {
        const names = revokedRoles.map((r) => `${r.roleNm} (${r.actions.join(', ')})`).join(', ')
        if (!confirm(`${revokedRoles.length}개 역할에서 권한이 회수됩니다: ${names}\n저장하시겠습니까?`)) return
      }
      await api.updateMenu(selected.menuId, body)
      toast('수정되었습니다')
      await loadTree(); await select(selected.menuId)
    } catch (err) { setFormError(errorMessage(err)) }
  }

  const remove = async () => {
    if (!selected || !confirm('메뉴를 삭제하시겠습니까? 이 메뉴의 권한과 역할 매핑도 함께 지워집니다.')) return
    try {
      await api.deleteMenu(selected.menuId)
      setSelected(null); setMode(null); setForm(null)
      toast('삭제되었습니다'); await loadTree()
    } catch (err) { setPageError(errorMessage(err)) }
  }

  // ----- 상위 메뉴 변경 -----
  const moveTargets = selected
    ? flatten(tree).filter((n) => n.menuId !== selected.menuId && n.menuTypeCd === 'FOLDER' && n.depth < 3
        && !isDescendant(byId, n, selected.menuId))
    : []

  const submitMove = async (e: FormEvent) => {
    e.preventDefault()
    if (!selected) return
    setMoveOpen(false)
    try {
      await api.moveMenu(selected.menuId, moveTarget ? Number(moveTarget) : null, selected.modDt)
      toast('이동했습니다')
      await loadTree(); await select(selected.menuId)
    } catch (err) { setPageError(errorMessage(err)) }
  }

  // ----- 순서 -----
  const step = (delta: -1 | 1) => {
    if (!selected) return
    const key = selected.parentMenuId == null ? '' : String(selected.parentMenuId)
    const ids = [...order[key]]
    const i = ids.indexOf(selected.menuId)
    const j = i + delta
    if (i < 0 || j < 0 || j >= ids.length) return
    ;[ids[i], ids[j]] = [ids[j], ids[i]]
    setOrder({ ...order, [key]: ids })
  }

  const saveOrder = async () => {
    if (!confirm('메뉴 순서를 저장하시겠습니까?')) return
    const orders = Object.keys(order)
      .filter((k) => order[k].join(',') !== (original[k] ?? []).join(','))
      .map((k) => ({ parentMenuId: k ? Number(k) : null, menuIds: order[k] }))
    try {
      await api.saveOrder({ orders })
      toast('순서가 저장되었습니다'); await loadTree()
    } catch (err) { setPageError(errorMessage(err)) }
  }

  // ----- 화면 -----
  const renderTree = (key: string): ReactNode => {
    const ids = order[key] ?? []
    const before = original[key] ?? []
    return (
      <ul className="menu-tree" data-parent-id={key}>
        {ids.map((id, index) => {
          const m = byId.get(id)
          if (!m) return null
          const cls = ['menu-node-row', selected?.menuId === id ? 'is-selected' : '', m.useYn === 'N' ? 'is-unused' : ''].filter(Boolean).join(' ')
          return (
            <li className="menu-node" data-menu-id={id} key={id}>
              <div className={cls}>
                <span className="drag-handle" title="끌어서 순서 변경" aria-hidden="true">⠿</span>
                <a className="menu-node-link" href="#" onClick={(e) => { e.preventDefault(); select(id).catch((err) => setPageError(errorMessage(err))) }}>{m.menuNm}</a>
                {m.systemYn === 'Y' && <span className="badge bg-secondary-lt" title="시스템 메뉴">🔒</span>}
                {m.boardAutoYn === 'Y' && <span className="badge bg-azure-lt">자동</span>}
                <span className="badge bg-yellow-lt changed-badge" hidden={before[index] === id}>변경됨</span>
              </div>
              {m.children.length > 0 && renderTree(String(id))}
            </li>
          )
        })}
      </ul>
    )
  }

  const create = mode === 'create'
  const depth = create ? (createParent ? createParent.depth + 1 : 1) : selected?.depth ?? 1
  const prot = !create && !!selected && (selected.systemYn === 'Y' || selected.boardAutoYn === 'Y')
  const hasChildren = !!selected && (byId.get(selected.menuId)?.children.length ?? 0) > 0
  const permProps = (allowed: boolean) => ({ disabled: !allowed, title: allowed ? undefined : NO_PERM })
  const structProps = (allowed: boolean) => ({ disabled: !allowed || dirty, title: allowed ? undefined : NO_PERM })
  const toggleAction = (cd: Action, on: boolean) =>
    form && setForm({ ...form, actions: on ? [...form.actions, cd] : form.actions.filter((a) => a !== cd) })

  return (
    <Layout title="메뉴관리">
      <div id="notice-area">{notice && <div className="alert alert-success" role="status">{notice}</div>}</div>
      <div id="page-message">{pageError && <div className="alert alert-danger" role="alert">{pageError}</div>}</div>
      <div className="row row-cards">
        <div className="col-lg-5">
          <div className="card" id="menu-tree-card">
            <div className="card-header">
              <h3 className="card-title">메뉴</h3>
              <div className="card-actions">
                <button type="button" className="btn btn-primary btn-sm" id="btn-root-create" {...structProps(perm.create)}
                        onClick={() => startCreate(null)}>최상위 메뉴 추가</button>
              </div>
            </div>
            <div className="card-body">
              {/* d-flex(display:flex !important)가 hidden을 덮으므로 hidden은 감싸는 div에 둔다 */}
              <div id="order-bar" hidden={!dirty}>
                <div className="alert alert-warning d-flex align-items-center gap-2 py-2">
                  <span className="me-auto">저장하지 않은 순서 변경이 있습니다</span>
                  <button type="button" className="btn btn-primary btn-sm" id="btn-order-save" onClick={saveOrder}>순서 저장</button>
                  <button type="button" className="btn btn-sm" id="btn-order-reset" onClick={() => setOrder(original)}>되돌리기</button>
                </div>
              </div>
              <div id="menu-tree-root" ref={treeRef}>{tree.length > 0 && renderTree('')}</div>
            </div>
            <div className="card-footer d-flex gap-2">
              <button type="button" className="btn btn-sm" id="btn-up" disabled={!perm.update || !selected} title={perm.update ? undefined : NO_PERM} onClick={() => step(-1)}>▲ 위로</button>
              <button type="button" className="btn btn-sm" id="btn-down" disabled={!perm.update || !selected} title={perm.update ? undefined : NO_PERM} onClick={() => step(1)}>▼ 아래로</button>
            </div>
          </div>
        </div>

        <div className="col-lg-7" id="menu-detail-col">
          {!mode || !form ? (
            <div className="card"><div className="card-body text-secondary">왼쪽에서 메뉴를 선택하세요.</div></div>
          ) : (
            <div className="card" id="menu-detail-card">
              <div className="card-header"><h3 className="card-title">{create ? '메뉴 등록' : '메뉴 상세'}</h3></div>
              <form id="menu-form" noValidate onSubmit={save}>
                <div className="card-body">
                  {prot && selected && (
                    <div className="alert alert-info" role="note">
                      {selected.systemYn === 'Y' ? '시스템 메뉴' : '게시판별 자동 메뉴'}는 메뉴명·아이콘만 바꿀 수 있습니다.
                    </div>
                  )}
                  <div className="mb-3"><label className="form-label" htmlFor="menu-parent">상위 메뉴</label>
                    <input className="form-control" id="menu-parent" readOnly value={(create ? createParent?.menuNm : selected?.parentMenuNm) ?? '-'} /></div>
                  <div className="mb-3"><label className={create ? 'form-label required' : 'form-label'} htmlFor="menu-menuCd">메뉴 코드</label>
                    <input className="form-control" id="menu-menuCd" maxLength={50} readOnly={!create} placeholder={create ? '영문 대문자·숫자·_' : undefined}
                           value={form.menuCd} onChange={(e) => setForm({ ...form, menuCd: e.target.value })} /></div>
                  <div className="mb-3"><label className="form-label required" htmlFor="menu-menuNm">메뉴명</label>
                    <input className="form-control" id="menu-menuNm" maxLength={100} required value={form.menuNm} onChange={(e) => setForm({ ...form, menuNm: e.target.value })} /></div>
                  <div className="mb-3"><label className="form-label">종류</label>
                    {create ? (
                      <div>
                        {(['FOLDER', 'PAGE'] as const).map((t) => (
                          <label className="form-check form-check-inline" key={t}>
                            <input className="form-check-input" type="radio" id={`menu-type-${t}`} checked={form.menuTypeCd === t}
                                   disabled={t === 'FOLDER' && depth === 3} onChange={() => setForm({ ...form, menuTypeCd: t })} /> {t === 'FOLDER' ? '폴더' : '화면'}
                          </label>
                        ))}
                      </div>
                    ) : (<div className="form-control-plaintext" id="menu-type">{form.menuTypeCd === 'FOLDER' ? '폴더' : '화면'}</div>)}
                  </div>
                  <div data-page-only hidden={form.menuTypeCd !== 'PAGE'}>
                    <div className="mb-3"><label className="form-label required" htmlFor="menu-menuUrl">URL</label>
                      <input className="form-control" id="menu-menuUrl" maxLength={200} placeholder="/로 시작" readOnly={prot}
                             value={form.menuUrl} onChange={(e) => setForm({ ...form, menuUrl: e.target.value })} /></div>
                    <div className="mb-3"><label className="form-label">사용 액션</label>
                      <div>
                        {ACTIONS.map((a) => (
                          <label className="form-check form-check-inline" key={a.cd}>
                            <input className="form-check-input" type="checkbox" id={`action-${a.cd}`} checked={a.cd === 'READ' || form.actions.includes(a.cd)}
                                   disabled={a.cd === 'READ' || prot} onChange={(e) => toggleAction(a.cd, e.target.checked)} /> {a.nm}
                          </label>
                        ))}
                      </div>
                    </div>
                  </div>
                  {depth === 1 && (
                    <div className="mb-3"><label className="form-label" htmlFor="menu-icon">아이콘</label>
                      <input className="form-control" id="menu-icon" maxLength={50} value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })} /></div>
                  )}
                  <div className="mb-3"><label className="form-label">사용 여부</label>
                    <div>
                      {(['Y', 'N'] as const).map((v) => (
                        <label className="form-check form-check-inline" key={v}>
                          <input className="form-check-input" type="radio" name="useYn" checked={form.useYn === v} disabled={prot}
                                 onChange={() => setForm({ ...form, useYn: v })} /> {v === 'Y' ? '사용' : '사용 안 함'}
                        </label>
                      ))}
                    </div>
                  </div>
                  <div id="menu-message">{formError && <div className="alert alert-danger" role="alert">{formError}</div>}</div>
                </div>
                <div className="card-footer d-flex flex-wrap gap-2">
                  <button type="submit" className="btn btn-primary" id="btn-save" {...permProps(create ? perm.create : perm.update)}>저장</button>
                  {!create && selected?.menuTypeCd === 'FOLDER' && selected.depth < 3 && (
                    <button type="button" className="btn" id="btn-child-create" {...structProps(perm.create)}
                            onClick={() => startCreate(byId.get(selected.menuId) ?? null)}>하위 메뉴 추가</button>
                  )}
                  {!create && !prot && (
                    <button type="button" className="btn" id="btn-move" {...structProps(perm.update)}
                            onClick={() => { setMoveTarget(selected?.parentMenuId == null ? '' : String(selected.parentMenuId)); setMoveOpen(true) }}>상위 메뉴 변경</button>
                  )}
                </div>
              </form>
              {!create && !prot && !hasChildren && (
                <div className="card-footer border-top-0 pt-0">
                  <button type="button" className="btn btn-outline-danger" id="btn-delete" {...structProps(perm.delete)} onClick={remove}>삭제</button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <Modal id="move-modal" open={moveOpen} title="상위 메뉴 변경" onClose={() => setMoveOpen(false)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="move-form" className="btn btn-primary">이동</button></>}>
        <form className="modal-body" id="move-form" onSubmit={submitMove}>
          <label className="form-label" htmlFor="move-parent">옮길 위치</label>
          <select className="form-select" id="move-parent" value={moveTarget} onChange={(e) => setMoveTarget(e.target.value)}>
            <option value="">(최상위)</option>
            {moveTargets.map((n) => <option key={n.menuId} value={n.menuId}>{n.depth === 2 ? '└ ' : ''}{n.menuNm}</option>)}
          </select>
          <p className="text-secondary mt-2 mb-0">옮긴 메뉴는 새 상위 메뉴의 맨 뒤 순서가 됩니다.</p>
        </form>
      </Modal>
    </Layout>
  )
}

/** node가 ancestorId 메뉴의 하위인지 (자기 하위로는 옮길 수 없다) */
function isDescendant(byId: Map<number, MenuAdminNode>, node: MenuAdminNode, ancestorId: number): boolean {
  for (let p = node.parentMenuId; p != null; p = byId.get(p)?.parentMenuId ?? null) {
    if (p === ancestorId) return true
  }
  return false
}
