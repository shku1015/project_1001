// 마스킹 설정 API (docs/06-api/10-masking.md)
import { call } from './client'
import type { components } from './schema'

type S = components['schemas']
export type MaskingPolicy = S['MaskingPolicy']
export type MaskingPolicySaveRequest = S['MaskingPolicySaveRequest']

export const getPolicies = (): Promise<MaskingPolicy[]> =>
  call({ method: 'GET', url: '/masking-policies' })

export const savePolicies = (body: MaskingPolicySaveRequest): Promise<null> =>
  call({ method: 'PUT', url: '/masking-policies', data: body })
