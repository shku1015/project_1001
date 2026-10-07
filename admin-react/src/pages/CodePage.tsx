import { useEffect, useState, type FormEvent } from 'react'
import * as api from '../api/code'
import type { CodeDetail, CodeGroup, CodeGroupSummary } from '../api/code'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout } from '../components/Layout'
import { Modal } from '../components/Modal'

type GroupForm = { mode: 'create' | 'edit'; groupCd: string; groupNm: string; description: string; useYn: string; systemYn: string; modDt: string }
type DetailForm = { mode: 'create' | 'edit'; code: string; codeNm: string; sortOrd: number; description: string; useYn: string; modDt: string }

/** SCR-COD-01 코드관리 (① React). ②③과 같은 화면 구조 */
export function CodePage() {
  const me = useMe()
  const [groups, setGroups] = useState<CodeGroupSummary[]>([])
  const [keyword, setKeyword] = useState('')
  const [searchUseYn, setSearchUseYn] = useState('')
  const [selected, setSelected] = useState<CodeGroup | null>(null)
  const [details, setDetails] = useState<CodeDetail[]>([])
  const [notice, setNotice] = useState<string | null>(null)
  const [groupForm, setGroupForm] = useState<GroupForm | null>(null)
  const [detailForm, setDetailForm] = useState<DetailForm | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const loadGroups = async () => setGroups(await api.listGroups(keyword.trim() || undefined, searchUseYn || undefined))
  useEffect(() => { void loadGroups() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const selectGroup = async (groupCd: string) => {
    setSelected(await api.getGroup(groupCd))
    setDetails(await api.listDetails(groupCd))
  }

  const toast = (message: string) => { setNotice(message); setTimeout(() => setNotice(null), 3000) }
  const errorMessage = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')

  // ----- 그룹 저장 -----
  const saveGroup = async (e: FormEvent) => {
    e.preventDefault()
    if (!groupForm) return
    setFormError(null)
    try {
      if (groupForm.mode === 'create') {
        await api.createGroup({ groupCd: groupForm.groupCd.trim(), groupNm: groupForm.groupNm.trim(), description: groupForm.description.trim() || null, useYn: groupForm.useYn as 'Y' | 'N' })
      } else {
        await api.updateGroup(groupForm.groupCd, { groupNm: groupForm.groupNm.trim(), description: groupForm.description.trim() || null, useYn: groupForm.useYn as 'Y' | 'N', modDt: groupForm.modDt })
      }
      setGroupForm(null)
      toast(groupForm.mode === 'create' ? '등록되었습니다' : '수정되었습니다')
      await loadGroups()
      if (groupForm.mode === 'edit') await selectGroup(groupForm.groupCd)
    } catch (err) { setFormError(errorMessage(err)) }
  }

  const removeGroup = async () => {
    if (!selected || !confirm('그룹코드를 삭제하시겠습니까?')) return
    try {
      await api.deleteGroup(selected.groupCd)
      setSelected(null); setDetails([]); toast('삭제되었습니다'); await loadGroups()
    } catch (err) { toast(errorMessage(err)) }
  }

  // ----- 상세 저장 -----
  const saveDetail = async (e: FormEvent) => {
    e.preventDefault()
    if (!detailForm || !selected) return
    setFormError(null)
    try {
      if (detailForm.mode === 'create') {
        await api.createDetail(selected.groupCd, { code: detailForm.code.trim(), codeNm: detailForm.codeNm.trim(), sortOrd: detailForm.sortOrd, description: detailForm.description.trim() || null, useYn: detailForm.useYn as 'Y' | 'N' })
      } else {
        await api.updateDetail(selected.groupCd, detailForm.code, { codeNm: detailForm.codeNm.trim(), sortOrd: detailForm.sortOrd, description: detailForm.description.trim() || null, useYn: detailForm.useYn as 'Y' | 'N', modDt: detailForm.modDt })
      }
      setDetailForm(null)
      toast(detailForm.mode === 'create' ? '등록되었습니다' : '수정되었습니다')
      await selectGroup(selected.groupCd); await loadGroups()
    } catch (err) { setFormError(errorMessage(err)) }
  }

  const removeDetail = async (code: string) => {
    if (!selected || !confirm('삭제하면 복구할 수 없습니다. 사용 안 함으로 바꾸는 것을 권장합니다. 삭제하시겠습니까?')) return
    try {
      await api.deleteDetail(selected.groupCd, code)
      toast('삭제되었습니다'); await selectGroup(selected.groupCd); await loadGroups()
    } catch (err) { toast(errorMessage(err)) }
  }

  const sys = selected?.systemYn === 'Y'
  // 권한이 없는 버튼은 비활성으로 보여 준다 (docs/05-ia-screens.md 4.2)
  const perm = (action: 'CREATE' | 'UPDATE' | 'DELETE') =>
    can(me, 'CODE', action) ? { disabled: false } : { disabled: true, title: '권한이 없습니다' }
  const nextSort = details.length ? Math.max(...details.map((d) => d.sortOrd)) + 1 : 1
  const renderUseYn = (value: string, onChange: (v: string) => void) => (
    <div className="mb-3">
      <label className="form-label">사용 여부</label>
      {(['Y', 'N'] as const).map((v) => (
        <label className="form-check form-check-inline" key={v}>
          <input className="form-check-input" type="radio" checked={value === v} onChange={() => onChange(v)} /> {v === 'Y' ? '사용' : '사용 안 함'}
        </label>
      ))}
    </div>
  )

  return (
    <Layout title="코드관리">
      <div id="notice-area">{notice && <div className="alert alert-success" role="status">{notice}</div>}</div>
      <div className="row row-cards">
        {/* 그룹코드 */}
        <div className="col-lg-5">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">그룹코드</h3>
              <div className="card-actions">
                <button type="button" className="btn btn-primary btn-sm" id="btn-group-create" {...perm('CREATE')}
                        onClick={() => { setFormError(null); setGroupForm({ mode: 'create', groupCd: '', groupNm: '', description: '', useYn: 'Y', systemYn: 'N', modDt: '' }) }}>그룹 등록</button>
              </div>
            </div>
            <div className="card-body border-bottom">
              <form className="row g-2" onSubmit={(e) => { e.preventDefault(); void loadGroups() }}>
                <div className="col"><input className="form-control" placeholder="그룹코드 / 그룹코드명" value={keyword} onChange={(e) => setKeyword(e.target.value)} /></div>
                <div className="col-auto">
                  <select className="form-select" value={searchUseYn} onChange={(e) => setSearchUseYn(e.target.value)}>
                    <option value="">전체</option><option value="Y">사용</option><option value="N">사용 안 함</option>
                  </select>
                </div>
                <div className="col-auto"><button type="submit" className="btn">검색</button></div>
              </form>
            </div>
            <div className="table-responsive">
              <table className="table table-vcenter table-selectable">
                <thead><tr><th>그룹코드</th><th>그룹코드명</th><th className="text-center">상세</th><th className="text-center">사용</th></tr></thead>
                <tbody>
                  {groups.map((g) => (
                    <tr key={g.groupCd} className={selected?.groupCd === g.groupCd ? 'table-active' : undefined}>
                      <td><a href="#" onClick={(e) => { e.preventDefault(); void selectGroup(g.groupCd) }}>{g.systemYn === 'Y' ? '🔒 ' : ''}{g.groupCd}</a></td>
                      <td>{g.groupNm}</td><td className="text-center">{g.codeCnt}</td><td className="text-center">{g.useYn === 'Y' ? '사용' : '사용 안 함'}</td>
                    </tr>
                  ))}
                  {groups.length === 0 && <tr><td colSpan={4} className="text-secondary text-center">조회된 데이터가 없습니다</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* 상세코드 */}
        <div className="col-lg-7">
          {!selected ? (
            <div className="card"><div className="card-body text-secondary">왼쪽에서 그룹코드를 선택하세요.</div></div>
          ) : (
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">{sys ? '🔒 ' : ''}{selected.groupNm} <span className="text-secondary ms-1">({selected.groupCd})</span></h3>
                <div className="card-actions btn-list">
                  <button type="button" className="btn btn-sm" id="btn-group-edit" {...perm('UPDATE')}
                    onClick={() => { setFormError(null); setGroupForm({ mode: 'edit', groupCd: selected.groupCd, groupNm: selected.groupNm, description: selected.description ?? '', useYn: selected.useYn, systemYn: selected.systemYn, modDt: selected.modDt }) }}>그룹 수정</button>
                  {!sys && details.length === 0 && <button type="button" className="btn btn-sm btn-ghost-danger" id="btn-group-delete" {...perm('DELETE')} onClick={removeGroup}>그룹 삭제</button>}
                  {!sys && <button type="button" className="btn btn-sm btn-primary" id="btn-detail-create" {...perm('CREATE')}
                    onClick={() => { setFormError(null); setDetailForm({ mode: 'create', code: '', codeNm: '', sortOrd: nextSort, description: '', useYn: 'Y', modDt: '' }) }}>코드 등록</button>}
                </div>
              </div>
              <div className="table-responsive">
                <table className="table table-vcenter">
                  <thead><tr><th>코드</th><th>코드명</th><th className="text-center">정렬</th><th>설명</th><th className="text-center">사용</th><th /></tr></thead>
                  <tbody>
                    {details.map((d) => (
                      <tr key={d.code}>
                        <td>{d.code}</td><td>{d.codeNm}</td><td className="text-center">{d.sortOrd}</td><td>{d.description}</td><td className="text-center">{d.useYn === 'Y' ? '사용' : '사용 안 함'}</td>
                        <td className="text-end btn-list">
                          <button type="button" className="btn btn-sm" {...perm('UPDATE')}
                            onClick={() => { setFormError(null); setDetailForm({ mode: 'edit', code: d.code, codeNm: d.codeNm, sortOrd: d.sortOrd, description: d.description ?? '', useYn: d.useYn, modDt: d.modDt }) }}>수정</button>
                          {!sys && <button type="button" className="btn btn-sm btn-ghost-danger" {...perm('DELETE')} onClick={() => removeDetail(d.code)}>삭제</button>}
                        </td>
                      </tr>
                    ))}
                    {details.length === 0 && <tr><td colSpan={6} className="text-secondary text-center">상세코드가 없습니다</td></tr>}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 그룹 모달 */}
      <Modal open={!!groupForm} title={groupForm?.mode === 'edit' ? '그룹 수정' : '그룹 등록'} onClose={() => setGroupForm(null)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="group-form" className="btn btn-primary">저장</button></>}>
        {groupForm && (
          <form className="modal-body" id="group-form" onSubmit={saveGroup} noValidate>
            {groupForm.mode === 'create' ? (
              <div className="mb-3"><label className="form-label required" htmlFor="group-groupCd">그룹코드</label>
                <input className="form-control" id="group-groupCd" maxLength={50} pattern="[A-Z0-9_]+" value={groupForm.groupCd} onChange={(e) => setGroupForm({ ...groupForm, groupCd: e.target.value })} required /></div>
            ) : (<small className="form-hint d-block mb-3" id="group-groupCd-display">그룹코드: {groupForm.groupCd}</small>)}
            <div className="mb-3"><label className="form-label required" htmlFor="group-groupNm">그룹코드명</label>
              <input className="form-control" id="group-groupNm" maxLength={100} value={groupForm.groupNm} onChange={(e) => setGroupForm({ ...groupForm, groupNm: e.target.value })} required /></div>
            <div className="mb-3"><label className="form-label" htmlFor="group-description">설명</label>
              <textarea className="form-control" id="group-description" rows={2} maxLength={500} value={groupForm.description} onChange={(e) => setGroupForm({ ...groupForm, description: e.target.value })} /></div>
            {!(groupForm.mode === 'edit' && groupForm.systemYn === 'Y') && renderUseYn(groupForm.useYn, (v) => setGroupForm({ ...groupForm, useYn: v }))}
            {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
          </form>
        )}
      </Modal>

      {/* 상세 모달 */}
      <Modal open={!!detailForm} title={detailForm?.mode === 'edit' ? '코드 수정' : '코드 등록'} onClose={() => setDetailForm(null)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="detail-form" className="btn btn-primary">저장</button></>}>
        {detailForm && (
          <form className="modal-body" id="detail-form" onSubmit={saveDetail} noValidate>
            <div className="mb-3"><label className="form-label required" htmlFor="detail-code">코드</label>
              <input className="form-control" id="detail-code" maxLength={50} pattern="[A-Z0-9_]+" value={detailForm.code} readOnly={detailForm.mode === 'edit'} onChange={(e) => setDetailForm({ ...detailForm, code: e.target.value })} required /></div>
            <div className="mb-3"><label className="form-label required" htmlFor="detail-codeNm">코드명</label>
              <input className="form-control" id="detail-codeNm" maxLength={100} value={detailForm.codeNm} onChange={(e) => setDetailForm({ ...detailForm, codeNm: e.target.value })} required /></div>
            <div className="mb-3"><label className="form-label required" htmlFor="detail-sortOrd">정렬 순서</label>
              <input className="form-control" type="number" id="detail-sortOrd" min={0} value={detailForm.sortOrd} onChange={(e) => setDetailForm({ ...detailForm, sortOrd: Number(e.target.value) })} required /></div>
            <div className="mb-3"><label className="form-label" htmlFor="detail-description">설명</label>
              <textarea className="form-control" id="detail-description" rows={2} maxLength={500} value={detailForm.description} onChange={(e) => setDetailForm({ ...detailForm, description: e.target.value })} /></div>
            {!(detailForm.mode === 'edit' && sys) && renderUseYn(detailForm.useYn, (v) => setDetailForm({ ...detailForm, useYn: v }))}
            {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
          </form>
        )}
      </Modal>
    </Layout>
  )
}
