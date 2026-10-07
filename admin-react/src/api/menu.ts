// 메뉴관리 API (docs/06-api/03-menu.md)
import { call } from './client'
import type { components } from './schema'

type S = components['schemas']
export type MenuAdminNode = S['MenuAdminNode']
export type MenuDetail = S['MenuDetail']
export type MenuCreateRequest = S['MenuCreateRequest']
export type MenuUpdateRequest = S['MenuUpdateRequest']
export type RevokedRole = S['RevokedRole']
export type MenuOrderRequest = S['MenuOrderRequest']

export const getTree = (): Promise<MenuAdminNode[]> =>
  call({ method: 'GET', url: '/menus/tree' })

export const getMenu = (menuId: number): Promise<MenuDetail> =>
  call({ method: 'GET', url: `/menus/${menuId}` })

export const createMenu = (body: MenuCreateRequest): Promise<{ menuId: number }> =>
  call({ method: 'POST', url: '/menus', data: body })

/** dryRun이면 저장하지 않고 회수될 역할만 돌려준다 (BR-07) */
export const updateMenu = (menuId: number, body: MenuUpdateRequest, dryRun = false): Promise<{ revokedRoles: RevokedRole[] }> =>
  call({ method: 'PUT', url: `/menus/${menuId}${dryRun ? '?dryRun=Y' : ''}`, data: body })

export const moveMenu = (menuId: number, parentMenuId: number | null, modDt: string): Promise<null> =>
  call({ method: 'PATCH', url: `/menus/${menuId}/parent`, data: { parentMenuId, modDt } })

export const saveOrder = (body: MenuOrderRequest): Promise<null> =>
  call({ method: 'PUT', url: '/menus/order', data: body })

export const deleteMenu = (menuId: number): Promise<null> =>
  call({ method: 'DELETE', url: `/menus/${menuId}` })
