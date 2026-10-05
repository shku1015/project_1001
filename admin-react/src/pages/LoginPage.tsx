import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/** SCR-AUTH-01 로그인 (① React). ②③ 로그인과 같은 구조 */
export function LoginPage() {
  const { state, login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (state.status === 'authenticated' && !submitting) {
    return <Navigate to="/" replace />
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const token = await login(loginId, password)
      navigate(token.pwdChangeRequired ? '/password' : '/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.displayMessage : '로그인할 수 없습니다.')
      setSubmitting(false)
    }
  }

  return (
    <div className="page page-center login-page">
      <div className="container container-tight py-4">
        <div className="text-center mb-4"><h1>관리자 서비스</h1></div>
        <div className="card card-md">
          <div className="card-body">
            <h2 className="h2 text-center mb-4">로그인</h2>
            <form onSubmit={onSubmit} noValidate>
              <div className="mb-3">
                <label className="form-label" htmlFor="loginId">아이디</label>
                <input className="form-control" type="text" id="loginId" required autoFocus
                       value={loginId} onChange={(e) => setLoginId(e.target.value)} />
              </div>
              <div className="mb-3">
                <label className="form-label" htmlFor="password">비밀번호</label>
                <input className="form-control" type="password" id="password" required
                       value={password} onChange={(e) => setPassword(e.target.value)} />
              </div>
              {error && <div className="alert alert-danger" role="alert">{error}</div>}
              {!error && params.has('logout') && <div className="alert alert-info" role="status">로그아웃되었습니다.</div>}
              <button type="submit" className="btn btn-primary w-100" disabled={submitting}>로그인</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
