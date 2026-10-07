import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import * as api from '../api/role'
import type { RoleDetail } from '../api/role'
import { ApiError } from '../api/client'
import { Layout, type NoticeState } from '../components/Layout'

/** SCR-ROL-03 역할 등록·수정 (① React). roleId가 있으면 수정. ②③과 같은 마크업 */
export function RoleFormPage() {
  const params = useParams()
  const roleId = params.roleId ? Number(params.roleId) : null
  const navigate = useNavigate()
  const [original, setOriginal] = useState<RoleDetail | null>(null)
  const [form, setForm] = useState({ roleCd: '', roleNm: '', description: '', useYn: 'Y' as 'Y' | 'N' })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (roleId == null) return
    api.getRole(roleId).then((r) => {
      setOriginal(r)
      setForm({ roleCd: r.roleCd, roleNm: r.roleNm, description: r.description ?? '', useYn: r.useYn })
    }).catch((e) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.'))
  }, [roleId])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const body = { roleNm: form.roleNm.trim(), description: form.description.trim() || null, useYn: form.useYn }
    try {
      if (roleId == null) {
        const { roleId: id } = await api.createRole({ ...body, roleCd: form.roleCd.trim() })
        navigate(`/roles/${id}`, { state: { notice: '등록되었습니다' } satisfies NoticeState })
        return
      }
      if (!original) return
      // 사용 안 함으로 바꾸면 그 역할을 가진 관리자의 권한이 빠진다 (BR-07)
      if (original.useYn === 'Y' && form.useYn === 'N' && original.adminCnt > 0
          && !confirm(`이 역할의 권한이 관리자 ${original.adminCnt}명에게서 빠집니다. 저장하시겠습니까?`)) return
      await api.updateRole(roleId, { ...body, modDt: original.modDt })
      navigate(`/roles/${roleId}`, { state: { notice: '수정되었습니다' } satisfies NoticeState })
    } catch (err) {
      setError(err instanceof ApiError ? err.displayMessage : '저장할 수 없습니다.')
    }
  }

  const isEdit = roleId != null
  return (
    <Layout title="역할">
      <div className="card" id="role-form-card">
        <div className="card-header"><h3 className="card-title" id="role-form-title">{isEdit ? '역할 수정' : '역할 등록'}</h3></div>
        <form id="role-form" noValidate onSubmit={onSubmit}>
          <div className="card-body">
            <div className="mb-3">
              <label className={`form-label${isEdit ? '' : ' required'}`} htmlFor="role-roleCd">역할 코드</label>
              <input className="form-control" id="role-roleCd" maxLength={50} required placeholder="영문 대문자·숫자·_" readOnly={isEdit}
                     value={form.roleCd} onChange={(e) => setForm({ ...form, roleCd: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label required" htmlFor="role-roleNm">역할명</label>
              <input className="form-control" id="role-roleNm" maxLength={100} required value={form.roleNm} onChange={(e) => setForm({ ...form, roleNm: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="role-description">설명</label>
              <textarea className="form-control" id="role-description" rows={3} maxLength={500} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">사용 여부</label>
              <div>
                {(['Y', 'N'] as const).map((v) => (
                  <label className="form-check form-check-inline" key={v}>
                    <input className="form-check-input" type="radio" name="useYn" id={`role-useYn-${v}`} checked={form.useYn === v} onChange={() => setForm({ ...form, useYn: v })} /> {v === 'Y' ? '사용' : '사용 안 함'}
                  </label>
                ))}
              </div>
            </div>
            <div id="role-message">{error && <div className="alert alert-danger" role="alert">{error}</div>}</div>
          </div>
          <div className="card-footer d-flex gap-2">
            <button type="submit" className="btn btn-primary" id="btn-save">저장</button>
            <Link className="btn" id="btn-cancel" to={isEdit ? `/roles/${roleId}` : '/roles'}>취소</Link>
          </div>
        </form>
      </div>
    </Layout>
  )
}
