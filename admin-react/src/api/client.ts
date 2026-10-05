// REST API 호출과 토큰 관리 (docs/04-features/09-auth.md 1.1).
// - Access Token은 메모리에만 둔다 (XSS 대비, sessionStorage에 두지 않는다).
// - Refresh Token은 HttpOnly 쿠키라 코드가 읽지 않는다. 재발급 요청에 브라우저가 자동으로 싣는다.
// - 401 TOKEN_EXPIRED면 한 번 재발급받고 원래 요청을 다시 보낸다.
import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ErrorCode, FieldError, Token } from './types'

let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

/** 서버 공통 실패 응답 { success: false, error: { code, message, fieldErrors } } */
export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode | 'NETWORK_ERROR'
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: ApiError['code'], message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }

  /** 입력 칸 오류가 있으면 첫 번째 칸 메시지, 없으면 전체 메시지 */
  get displayMessage(): string {
    return this.fieldErrors[0]?.message ?? this.message
  }
}

type ErrorBody = { error?: { code: ErrorCode; message: string; fieldErrors?: FieldError[] } }

function toApiError(e: unknown): ApiError {
  if (e instanceof ApiError) return e
  if (e instanceof AxiosError && e.response) {
    const body = e.response.data as ErrorBody | undefined
    if (body?.error) {
      return new ApiError(e.response.status, body.error.code, body.error.message, body.error.fieldErrors ?? [])
    }
    return new ApiError(e.response.status, 'INTERNAL_ERROR', '일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.')
  }
  return new ApiError(0, 'NETWORK_ERROR', '서버에 연결할 수 없습니다.')
}

export const http = axios.create({ baseURL: '/api/v1', withCredentials: true })

http.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

let refreshing: Promise<Token> | null = null

/**
 * Access Token 재발급. 동시에 여러 번 불려도 실제 요청은 하나만 보낸다.
 * (Refresh Token은 한 번 쓰면 교체되므로, 같은 토큰으로 두 번 요청하면 서버가 재사용으로 보고 모두 폐기한다)
 */
export function refreshAccessToken(): Promise<Token> {
  if (!refreshing) {
    refreshing = http
      .post<{ data: Token }>('/auth/token/refresh')
      .then((res) => {
        setAccessToken(res.data.data.accessToken)
        return res.data.data
      })
      .catch((e: unknown) => {
        setAccessToken(null)
        throw toApiError(e)
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

http.interceptors.response.use(undefined, async (e: unknown) => {
  const error = toApiError(e)
  const config = (e as AxiosError).config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
  if (error.status === 401 && error.code === 'TOKEN_EXPIRED' && config && !config._retried) {
    config._retried = true
    await refreshAccessToken()
    return http.request(config)
  }
  throw error
})

/** 응답의 data만 꺼낸다 */
export async function call<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await http.request<{ data: T }>(config)
  return res.data.data
}
