// 관리자관리 API (docs/06-api/06-admin.md)
import { call } from './client'
import type { components } from './schema'

type S = components['schemas']
export type AdminListItem = S['AdminListItem']
export type AdminPage = S['AdminPageResponse']['data']
export type AdminDetail = S['AdminDetail']
export type AdminRoleOption = S['AdminRoleOption']
export type AdminLoginHistory = S['AdminLoginHistory']
export type AdminCreateRequest = S['AdminCreateRequest']
export type AdminUpdateRequest = S['AdminUpdateRequest']

export type AdminSearch = {
  keyword?: string; loginId?: string; adminNm?: string; deptNm?: string; roleId?: string; statusCd?: string
  page?: number; size?: number; sort?: string
}

export function listAdmins(search: AdminSearch): Promise<AdminPage> {
  const params = new URLSearchParams()
  Object.entries(search).forEach(([k, v]) => { if (v !== undefined && v !== '') params.set(k, String(v)) })
  return call({ method: 'GET', url: `/admins?${params}` })
}

export const getAdmin = (adminId: number): Promise<AdminDetail> =>
  call({ method: 'GET', url: `/admins/${adminId}` })

export const getRoleOptions = (): Promise<AdminRoleOption[]> =>
  call({ method: 'GET', url: '/admins/role-options' })

export const getLoginHistories = (adminId: number): Promise<AdminLoginHistory[]> =>
  call({ method: 'GET', url: `/admins/${adminId}/login-histories` })

export const createAdmin = (body: AdminCreateRequest): Promise<{ adminId: number; tempPassword: string }> =>
  call({ method: 'POST', url: '/admins', data: body })

export const updateAdmin = (adminId: number, body: AdminUpdateRequest): Promise<null> =>
  call({ method: 'PUT', url: `/admins/${adminId}`, data: body })

export const grantRole = (adminId: number, roleId: number): Promise<null> =>
  call({ method: 'POST', url: `/admins/${adminId}/roles`, data: { roleId } })

export const revokeRole = (adminId: number, roleId: number): Promise<null> =>
  call({ method: 'DELETE', url: `/admins/${adminId}/roles/${roleId}` })

export const unlock = (adminId: number): Promise<null> =>
  call({ method: 'POST', url: `/admins/${adminId}/unlock` })

export const resetPassword = (adminId: number): Promise<{ tempPassword: string }> =>
  call({ method: 'POST', url: `/admins/${adminId}/password-reset` })

export const changeStatus = (adminId: number, statusCd: 'ACTIVE' | 'DISABLED', modDt: string): Promise<null> =>
  call({ method: 'PATCH', url: `/admins/${adminId}/status`, data: { statusCd, modDt } })
