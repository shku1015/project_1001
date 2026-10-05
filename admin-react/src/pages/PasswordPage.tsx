import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import * as authApi from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth, useMe } from '../auth/AuthContext'
import { Layout, type NoticeState } from '../components/Layout'

/**
 * SCR-AUTH-02 비밀번호 변경 (① React).
 * 임시 비밀번호면 헤더·메뉴 없이 보여 주고(로그아웃만 가능), 아니면 일반 레이아웃 안에 보여 준다.
 */
export function PasswordPage() {
  const me = useMe()
  const { reloadMe, logout } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', newPasswordConfirm: '' })
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (form.newPassword !== form.newPasswordConfirm) {
      setError('새 비밀번호 확인이 일치하지 않습니다.')
      return
    }
    try {
      await authApi.changePassword(form.currentPassword, form.newPassword)
      await reloadMe()
      navigate('/', { state: { notice: '비밀번호가 변경되었습니다' } satisfies NoticeState })
    } catch (err) {
      setError(err instanceof ApiError ? err.displayMessage : '비밀번호를 바꿀 수 없습니다.')
    }
  }

  const field = (name: keyof typeof form, label: string) => (
    <div className="mb-3">
      <label className="form-label" htmlFor={name}>{label}</label>
      <input className="form-control" type="password" id={name} required maxLength={name === 'currentPassword' ? undefined : 20}
             value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} />
    </div>
  )

  const card = (
    <div className="card">
      <div className="card-body">
        {me.pwdChangeRequired && (
          <div className="alert alert-warning" role="status">임시 비밀번호로 로그인했습니다. 비밀번호를 바꿔야 다른 화면을 쓸 수 있습니다.</div>
        )}
        <form onSubmit={onSubmit} noValidate>
          {field('currentPassword', '현재 비밀번호')}
          {field('newPassword', '새 비밀번호')}
          {field('newPasswordConfirm', '새 비밀번호 확인')}
          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          <button type="submit" className="btn btn-primary">변경</button>
        </form>
      </div>
    </div>
  )

  if (!me.pwdChangeRequired) {
    return <Layout title="비밀번호 변경">{card}</Layout>
  }
  return (
    <div className="page page-center login-page">
      <div className="container container-tight py-4">
        <h2 className="page-title mb-3">비밀번호 변경</h2>
        {card}
        <div className="mt-3 text-center">
          <button type="button" className="btn btn-link" onClick={() => logout()}>로그아웃</button>
        </div>
      </div>
    </div>
  )
}
