// 역할관리 API (docs/06-api/08-role.md)
import { call } from './client'
import type { components } from './schema'

type S = components['schemas']
export type RoleListItem = S['RoleListItem']
export type RoleDetail = S['RoleDetail']
export type RolePermissionNode = S['RolePermissionNode']
export type RoleAdmin = S['RoleAdmin']
export type RoleCreateRequest = S['RoleCreateRequest']
export type RoleUpdateRequest = S['RoleUpdateRequest']
export type RolePermissionSaveRequest = S['RolePermissionSaveRequest']
export type PermissionChange = S['PermissionChange']

export function listRoles(keyword?: string, useYn?: string): Promise<RoleListItem[]> {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  if (useYn) params.set('useYn', useYn)
  const q = params.toString()
  return call({ method: 'GET', url: `/roles${q ? `?${q}` : ''}` })
}

export const getRole = (roleId: number): Promise<RoleDetail> =>
  call({ method: 'GET', url: `/roles/${roleId}` })

export const getPermissions = (roleId: number): Promise<RolePermissionNode[]> =>
  call({ method: 'GET', url: `/roles/${roleId}/permissions` })

export const getAdmins = (roleId: number): Promise<RoleAdmin[]> =>
  call({ method: 'GET', url: `/roles/${roleId}/admins` })

export const createRole = (body: RoleCreateRequest): Promise<{ roleId: number }> =>
  call({ method: 'POST', url: '/roles', data: body })

export const copyRole = (roleId: number, roleCd: string, roleNm: string): Promise<{ roleId: number }> =>
  call({ method: 'POST', url: `/roles/${roleId}/copy`, data: { roleCd, roleNm } })

export const updateRole = (roleId: number, body: RoleUpdateRequest): Promise<null> =>
  call({ method: 'PUT', url: `/roles/${roleId}`, data: body })

export const savePermissions = (roleId: number, body: RolePermissionSaveRequest): Promise<{ added: PermissionChange[]; removed: PermissionChange[] }> =>
  call({ method: 'PUT', url: `/roles/${roleId}/permissions`, data: body })

export const deleteRole = (roleId: number): Promise<null> =>
  call({ method: 'DELETE', url: `/roles/${roleId}` })
