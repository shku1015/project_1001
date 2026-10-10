// 사용자관리 API (docs/06-api/02-user.md)
import { call, download } from './client'
import type { components } from './schema'

type S = components['schemas']
export type UserListItem = S['UserListItem']
export type UserPage = S['UserPageResponse']['data']
export type UserDetail = S['UserDetail']
export type UserForm = S['UserForm']
export type UserRequest = S['UserRequest']
export type UserPrivacy = S['UserPrivacyResponse']['data']
export type UserStatusHistory = S['UserStatusHistory']
export type UserCompanyOption = S['UserCompanyOption']

export type UserSearch = {
  userTypeCd?: string; loginId?: string; userNm?: string; email?: string; mobileNo?: string; companyId?: string
  companyNm?: string; statusCd?: string; joinPath?: string; joinDtFrom?: string; joinDtTo?: string
  page?: number; size?: number; sort?: string
}

function query(search: UserSearch): string {
  const params = new URLSearchParams()
  Object.entries(search).forEach(([k, v]) => { if (v !== undefined && v !== '') params.set(k, String(v)) })
  return params.toString()
}

export const listUsers = (search: UserSearch): Promise<UserPage> =>
  call({ method: 'GET', url: `/users?${query(search)}` })

/** 목록과 같은 조건으로 엑셀을 내려받는다 (페이지 정보는 빼고) */
export const downloadExcel = ({ page: _page, size: _size, ...search }: UserSearch): Promise<void> =>
  download(`/users/excel?${query(search)}`)

export const getUser = (userId: number): Promise<UserDetail> =>
  call({ method: 'GET', url: `/users/${userId}` })

export const getStatusHistories = (userId: number): Promise<UserStatusHistory[]> =>
  call({ method: 'GET', url: `/users/${userId}/status-histories` })

export const viewPrivacy = (userId: number, reasonCd: string, reasonEtc: string | null): Promise<UserPrivacy> =>
  call({ method: 'POST', url: `/users/${userId}/privacy`, data: { reasonCd, reasonEtc } })

export const getForm = (userId: number): Promise<UserForm> =>
  call({ method: 'GET', url: `/users/${userId}/form` })

export const getCompanyOptions = (): Promise<UserCompanyOption[]> =>
  call({ method: 'GET', url: '/users/company-options' })

export const createUser = (body: UserRequest): Promise<{ userId: number; tempPassword: string }> =>
  call({ method: 'POST', url: '/users', data: body })

export const updateUser = (userId: number, body: UserRequest): Promise<null> =>
  call({ method: 'PUT', url: `/users/${userId}`, data: body })

export const changeStatus = (userId: number, statusCd: 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN', reason: string, modDt: string): Promise<null> =>
  call({ method: 'PATCH', url: `/users/${userId}/status`, data: { statusCd, reason, modDt } })

export const resetPassword = (userId: number): Promise<{ tempPassword: string }> =>
  call({ method: 'POST', url: `/users/${userId}/password-reset` })

export const deleteUser = (userId: number, reason: string): Promise<null> =>
  call({ method: 'DELETE', url: `/users/${userId}`, data: { reason } })
