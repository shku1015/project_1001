import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import * as authApi from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth, useMe } from '../auth/AuthContext'
import { Layout, type NoticeState } from '../components/Layout'

/** SCR-MY-01 내 정보 (① React) */
export function MePage() {
  const me = useMe()
  const { reloadMe } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    adminNm: me.adminNm,
    email: me.email,
    mobileNo: me.mobileNo ?? '',
    deptNm: me.deptNm ?? '',
  })
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await authApi.updateMe({
        adminNm: form.adminNm.trim(),
        email: form.email.trim(),
        mobileNo: form.mobileNo.trim() || null,
        deptNm: form.deptNm.trim() || null,
        modDt: me.modDt,
      })
      await reloadMe()
      navigate('/me', { replace: true, state: { notice: '저장되었습니다' } satisfies NoticeState })
    } catch (err) {
      setError(err instanceof ApiError ? err.displayMessage : '저장할 수 없습니다.')
    }
  }

  const input = (name: keyof typeof form, label: string, props: { type?: string; maxLength: number; required?: boolean; placeholder?: string }) => (
    <div className="mb-3">
      <label className={`form-label${props.required ? ' required' : ''}`} htmlFor={name}>{label}</label>
      <input className="form-control" id={name} type={props.type ?? 'text'} maxLength={props.maxLength}
             required={props.required} placeholder={props.placeholder}
             value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} />
    </div>
  )

  return (
    <Layout title="내 정보">
      <div className="card">
        <div className="card-body">
          <form onSubmit={onSubmit} noValidate>
            <div className="mb-3">
              <label className="form-label" htmlFor="loginId">로그인 아이디</label>
              <input className="form-control" id="loginId" value={me.loginId} readOnly />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="roles">역할</label>
              <input className="form-control" id="roles" value={me.roles.map((r) => r.roleNm).join(', ')} readOnly />
            </div>
            {input('adminNm', '이름', { maxLength: 50, required: true })}
            {input('email', '이메일', { type: 'email', maxLength: 100, required: true })}
            {input('mobileNo', '휴대폰 번호', { maxLength: 11, placeholder: '숫자만' })}
            {input('deptNm', '부서', { maxLength: 100 })}
            {error && <div className="alert alert-danger" role="alert">{error}</div>}
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary">저장</button>
              <Link className="btn btn-outline-secondary" to="/password">비밀번호 변경</Link>
            </div>
          </form>
        </div>
      </div>
    </Layout>
  )
}
