// 기업정보관리 API (docs/06-api/01-company.md)
import { call, download } from './client'
import type { components } from './schema'

type S = components['schemas']
export type CompanyListItem = S['CompanyListItem']
export type CompanyPage = S['CompanyPageResponse']['data']
export type CompanyDetail = S['CompanyDetail']
export type CompanyRequest = S['CompanyRequest']
export type CompanyUsers = S['CompanyUsersResponse']['data']

export type CompanySearch = {
  companyNm?: string; bizRegNo?: string; ceoNm?: string; statusCd?: string; regDtFrom?: string; regDtTo?: string
  page?: number; size?: number; sort?: string
}

function query(search: CompanySearch): string {
  const params = new URLSearchParams()
  Object.entries(search).forEach(([k, v]) => { if (v !== undefined && v !== '') params.set(k, String(v)) })
  return params.toString()
}

export const listCompanies = (search: CompanySearch): Promise<CompanyPage> =>
  call({ method: 'GET', url: `/companies?${query(search)}` })

/** 목록과 같은 조건으로 엑셀을 내려받는다 (페이지 정보는 빼고) */
export const downloadExcel = ({ page: _page, size: _size, ...search }: CompanySearch): Promise<void> =>
  download(`/companies/excel?${query(search)}`)

export const getCompany = (companyId: number): Promise<CompanyDetail> =>
  call({ method: 'GET', url: `/companies/${companyId}` })

export const getUsers = (companyId: number, size = 10): Promise<CompanyUsers> =>
  call({ method: 'GET', url: `/companies/${companyId}/users?size=${size}` })

export const createCompany = (body: CompanyRequest): Promise<{ companyId: number }> =>
  call({ method: 'POST', url: '/companies', data: body })

export const updateCompany = (companyId: number, body: CompanyRequest): Promise<null> =>
  call({ method: 'PUT', url: `/companies/${companyId}`, data: body })

export const changeStatus = (companyId: number, statusCd: 'ACTIVE' | 'SUSPENDED', reason: string, modDt: string): Promise<null> =>
  call({ method: 'PATCH', url: `/companies/${companyId}/status`, data: { statusCd, reason, modDt } })

export const deleteCompany = (companyId: number): Promise<null> =>
  call({ method: 'DELETE', url: `/companies/${companyId}` })
