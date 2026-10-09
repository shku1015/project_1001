import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import * as api from '../api/admin'
import type { AdminDetail, AdminRoleOption } from '../api/admin'
import { ApiError } from '../api/client'
import { Layout } from '../components/Layout'
import type { AdminNoticeState } from './AdminDetailPage'

/** SCR-ADM-03 관리자 등록·수정 (① React). adminId가 있으면 수정. 역할은 등록 때만 고른다. ②③과 같은 마크업 */
export function AdminFormPage() {
  const params = useParams()
  const adminId = params.adminId ? Number(params.adminId) : null
  const navigate = useNavigate()
  const [original, setOriginal] = useState<AdminDetail | null>(null)
  const [roles, setRoles] = useState<AdminRoleOption[]>([])
  const [form, setForm] = useState({ loginId: '', adminNm: '', email: '', mobileNo: '', deptNm: '', roleIds: [] as number[] })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fail = (e: unknown) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.')
    if (adminId == null) {
      api.getRoleOptions().then((r) => setRoles(r.filter((o) => o.assignable))).catch(fail)
      return
    }
    api.getAdmin(adminId).then((a) => {
      setOriginal(a)
      setForm({ loginId: a.loginId, adminNm: a.adminNm, email: a.email, mobileNo: a.mobileNo ?? '', deptNm: a.deptNm ?? '', roleIds: [] })
    }).catch(fail)
  }, [adminId])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const body = { adminNm: form.adminNm.trim(), email: form.email.trim(), mobileNo: form.mobileNo.trim() || null, deptNm: form.deptNm.trim() || null }
    try {
      if (adminId == null) {
        const created = await api.createAdmin({ ...body, loginId: form.loginId.trim(), roleIds: form.roleIds })
        navigate(`/admins/${created.adminId}`, { state: { notice: '등록되었습니다', tempPassword: created.tempPassword } satisfies AdminNoticeState })
        return
      }
      if (!original) return
      await api.updateAdmin(adminId, { ...body, modDt: original.modDt })
      navigate(`/admins/${adminId}`, { state: { notice: '수정되었습니다' } satisfies AdminNoticeState })
    } catch (err) {
      setError(err instanceof ApiError ? err.displayMessage : '저장할 수 없습니다.')
    }
  }

  const isEdit = adminId != null
  const field = (name: 'adminNm' | 'email' | 'mobileNo' | 'deptNm', label: string, props: { required?: boolean; maxLength: number; type?: string; placeholder?: string }) => (
    <div className="mb-3">
      <label className={`form-label${props.required ? ' required' : ''}`} htmlFor={`admin-${name}`}>{label}</label>
      <input className="form-control" id={`admin-${name}`} type={props.type ?? 'text'} maxLength={props.maxLength} required={props.required}
             placeholder={props.placeholder} value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} />
    </div>
  )
  const toggleRole = (roleId: number, on: boolean) =>
    setForm({ ...form, roleIds: on ? [...form.roleIds, roleId] : form.roleIds.filter((id) => id !== roleId) })

  return (
    <Layout title="관리자">
      <div className="card" id="admin-form-card">
        <div className="card-header"><h3 className="card-title" id="admin-form-title">{isEdit ? '관리자 수정' : '관리자 등록'}</h3></div>
        <form id="admin-form" noValidate onSubmit={onSubmit}>
          <div className="card-body">
            <div className="mb-3">
              <label className={`form-label${isEdit ? '' : ' required'}`} htmlFor="admin-loginId">로그인 아이디</label>
              <input className="form-control" id="admin-loginId" maxLength={50} required placeholder="영문 소문자·숫자 4~50자" readOnly={isEdit}
                     value={form.loginId} onChange={(e) => setForm({ ...form, loginId: e.target.value })} />
            </div>
            {field('adminNm', '이름', { required: true, maxLength: 50 })}
            {field('email', '이메일', { required: true, maxLength: 100, type: 'email' })}
            {field('mobileNo', '휴대폰 번호', { maxLength: 11, placeholder: '숫자만 10~11자리' })}
            {field('deptNm', '부서', { maxLength: 100 })}
            {!isEdit && (
              <div className="mb-3">
                <label className="form-label required">역할</label>
                <div id="admin-roles-choice">
                  {roles.map((r) => (
                    <label className="form-check form-check-inline" key={r.roleId}>
                      <input className="form-check-input" type="checkbox" id={`role-${r.roleId}`} checked={form.roleIds.includes(r.roleId)}
                             onChange={(e) => toggleRole(r.roleId, e.target.checked)} /> {r.roleNm}
                    </label>
                  ))}
                </div>
                <small className="form-hint">내 권한 범위 안의 사용 중인 역할만 고를 수 있습니다. 등록 후 임시 비밀번호가 한 번 표시됩니다.</small>
              </div>
            )}
            <div id="admin-message">{error && <div className="alert alert-danger" role="alert">{error}</div>}</div>
          </div>
          <div className="card-footer d-flex gap-2">
            <button type="submit" className="btn btn-primary" id="btn-save">저장</button>
            <Link className="btn" id="btn-cancel" to={isEdit ? `/admins/${adminId}` : '/admins'}>취소</Link>
          </div>
        </form>
      </div>
    </Layout>
  )
}
