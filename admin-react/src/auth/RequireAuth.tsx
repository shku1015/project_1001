import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

/**
 * 로그인이 필요한 화면. 로그인하지 않았으면 로그인 화면으로,
 * 임시 비밀번호 상태면 비밀번호 변경 화면으로 보낸다 (AUTH-03).
 */
export function RequireAuth({ children, allowTempPassword = false }: { children: ReactNode; allowTempPassword?: boolean }) {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return <div className="page page-center"><div className="spinner-border" role="status" aria-label="불러오는 중" /></div>
  }
  if (state.status === 'anonymous') {
    return <Navigate to={state.loggedOut ? '/login?logout' : '/login'} replace state={{ from: location.pathname }} />
  }
  if (state.me.pwdChangeRequired && !allowTempPassword) {
    return <Navigate to="/password" replace />
  }
  return <>{children}</>
}
