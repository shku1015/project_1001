import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { refreshAccessToken } from '../api/client'
import { AuthContext, type AuthContextValue, type AuthState } from './AuthContext'

/**
 * 로그인 상태. 앱을 열면 Refresh Token 쿠키로 Access Token을 재발급받아 로그인 상태를 되살린다.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    refreshAccessToken()
      .then(() => authApi.getMe())
      .then((me) => !cancelled && setState({ status: 'authenticated', me }))
      .catch(() => !cancelled && setState({ status: 'anonymous' }))
    return () => {
      cancelled = true
    }
  }, [])

  const reloadMe = useCallback(async () => {
    const me = await authApi.getMe()
    setState({ status: 'authenticated', me })
  }, [])

  const login = useCallback(async (loginId: string, password: string) => {
    const token = await authApi.login(loginId, password)
    await reloadMe()
    return token
  }, [reloadMe])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setState({ status: 'anonymous', loggedOut: true })
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({ state, login, logout, reloadMe }), [state, login, logout, reloadMe])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
