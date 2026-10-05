// 인증·내 정보 API (docs/06-api/00-auth.md)
import { call, setAccessToken } from './client'
import type { Me, Token, UpdateMyInfoRequest } from './types'

export async function login(loginId: string, password: string): Promise<Token> {
  const token = await call<Token>({ method: 'POST', url: '/auth/token', data: { loginId, password } })
  setAccessToken(token.accessToken)
  return token
}

export async function logout(): Promise<void> {
  try {
    await call<null>({ method: 'DELETE', url: '/auth/token' })
  } finally {
    setAccessToken(null)
  }
}

export function getMe(): Promise<Me> {
  return call<Me>({ method: 'GET', url: '/auth/me' })
}

export function updateMe(body: UpdateMyInfoRequest): Promise<null> {
  return call<null>({ method: 'PUT', url: '/me', data: body })
}

/** 성공하면 새 토큰을 받는다 (다른 로그인은 서버가 끊는다) */
export async function changePassword(currentPassword: string, newPassword: string): Promise<Token> {
  const token = await call<Token>({ method: 'PUT', url: '/me/password', data: { currentPassword, newPassword } })
  setAccessToken(token.accessToken)
  return token
}
