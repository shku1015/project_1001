// 코드관리 API (docs/06-api/04-code.md)
import { call } from './client'
import type { components } from './schema'

type S = components['schemas']
export type CodeGroupSummary = S['CodeGroupSummary']
export type CodeGroup = S['CodeGroup']
export type CodeDetail = S['CodeDetail']
export type CodeGroupRequest = S['CodeGroupRequest']
export type CodeGroupUpdateRequest = S['CodeGroupUpdateRequest']
export type CodeDetailRequest = S['CodeDetailRequest']
export type CodeDetailUpdateRequest = S['CodeDetailUpdateRequest']

export function listGroups(keyword?: string, useYn?: string): Promise<CodeGroupSummary[]> {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  if (useYn) params.set('useYn', useYn)
  const q = params.toString()
  return call({ method: 'GET', url: `/code-groups${q ? `?${q}` : ''}` })
}

export const getGroup = (groupCd: string): Promise<CodeGroup> =>
  call({ method: 'GET', url: `/code-groups/${groupCd}` })

export const createGroup = (body: CodeGroupRequest): Promise<null> =>
  call({ method: 'POST', url: '/code-groups', data: body })

export const updateGroup = (groupCd: string, body: CodeGroupUpdateRequest): Promise<null> =>
  call({ method: 'PUT', url: `/code-groups/${groupCd}`, data: body })

export const deleteGroup = (groupCd: string): Promise<null> =>
  call({ method: 'DELETE', url: `/code-groups/${groupCd}` })

export const listDetails = (groupCd: string): Promise<CodeDetail[]> =>
  call({ method: 'GET', url: `/code-groups/${groupCd}/codes` })

export const createDetail = (groupCd: string, body: CodeDetailRequest): Promise<null> =>
  call({ method: 'POST', url: `/code-groups/${groupCd}/codes`, data: body })

export const updateDetail = (groupCd: string, code: string, body: CodeDetailUpdateRequest): Promise<null> =>
  call({ method: 'PUT', url: `/code-groups/${groupCd}/codes/${code}`, data: body })

export const deleteDetail = (groupCd: string, code: string): Promise<null> =>
  call({ method: 'DELETE', url: `/code-groups/${groupCd}/codes/${code}` })
