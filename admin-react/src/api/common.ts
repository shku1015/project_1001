// 공통 조회 API (docs/06-api-spec.md 9절)
import { call } from './client'
import type { components } from './schema'

export type CodeItem = components['schemas']['CodeItem']

/** 그룹코드의 상세코드 목록 (코드 콤보 CMP-04) */
export const getCodes = (groupCd: string): Promise<CodeItem[]> =>
  call({ method: 'GET', url: `/common/codes/${groupCd}` })
