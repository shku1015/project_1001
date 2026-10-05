import { createContext, useContext } from 'react'
import type { Me, Token } from '../api/types'

export type AuthState =
  | { status: 'loading' }
  /** loggedOut: 로그아웃해서 비로그인 상태가 됨 (로그인 화면에 "로그아웃되었습니다"를 보여 준다) */
  | { status: 'anonymous'; loggedOut?: boolean }
  | { status: 'authenticated'; me: Me }

export type AuthContextValue = {
  state: AuthState
  login: (loginId: string, password: string) => Promise<Token>
  logout: () => Promise<void>
  /** 내 정보를 다시 읽는다 (내 정보 수정·비밀번호 변경 후) */
  reloadMe: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('AuthProvider 안에서만 쓸 수 있습니다')
  }
  return value
}

/** 로그인한 화면에서만 쓴다 (RequireAuth 아래) */
export function useMe(): Me {
  const { state } = useAuth()
  if (state.status !== 'authenticated') {
    throw new Error('로그인한 상태가 아닙니다')
  }
  return state.me
}
