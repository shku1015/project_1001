// 권한관리 API (docs/06-api/07-permission.md)
import { call } from './client'
import type { components } from './schema'

type S = components['schemas']
export type PermissionMenuNode = S['PermissionMenuNode']
export type MenuRoleGrant = S['MenuRoleGrant']
export type MenuRoleGrants = S['MenuRoleGrantResponse']['data']
export type MenuRoleGrantSaveRequest = S['MenuRoleGrantSaveRequest']
export type RoleGrantChange = S['RoleGrantChange']

export const getTree = (): Promise<PermissionMenuNode[]> =>
  call({ method: 'GET', url: '/permissions' })

export const getMenuGrants = (menuId: number): Promise<MenuRoleGrants> =>
  call({ method: 'GET', url: `/permissions/menus/${menuId}` })

export const saveMenuGrants = (menuId: number, body: MenuRoleGrantSaveRequest): Promise<{ changes: RoleGrantChange[] }> =>
  call({ method: 'PUT', url: `/permissions/menus/${menuId}`, data: body })
