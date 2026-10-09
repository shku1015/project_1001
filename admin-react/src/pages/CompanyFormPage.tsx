import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import * as api from '../api/company'
import type { CompanyDetail } from '../api/company'
import { ApiError } from '../api/client'
import { Layout, type NoticeState } from '../components/Layout'
import { openPostcode } from '../lib/postcode'

const EMPTY = { companyNm: '', bizRegNo: '', ceoNm: '', bizType: '', bizItem: '', telNo: '', zipCd: '', addr: '', addrDtl: '' }
type Field = keyof typeof EMPTY

/** SCR-COM-03 기업 등록·수정 (① React). 우편번호·주소는 우편번호 검색(CMP-16)으로만 채운다. ②③과 같은 마크업 */
export function CompanyFormPage() {
  const params = useParams()
  const companyId = params.companyId ? Number(params.companyId) : null
  const navigate = useNavigate()
  const [original, setOriginal] = useState<CompanyDetail | null>(null)
  const [form, setForm] = useState(EMPTY)
  const [changed, setChanged] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const message = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')

  useEffect(() => {
    if (companyId == null) return
    api.getCompany(companyId).then((c) => {
      setOriginal(c)
      setForm({ companyNm: c.companyNm, bizRegNo: c.bizRegNo, ceoNm: c.ceoNm, bizType: c.bizType ?? '', bizItem: c.bizItem ?? '',
                telNo: c.telNo ?? '', zipCd: c.zipCd ?? '', addr: c.addr ?? '', addrDtl: c.addrDtl ?? '' })
    }).catch((e) => setError(message(e)))
  }, [companyId])

  const set = (name: Field, value: string) => { setForm((f) => ({ ...f, [name]: value })); setChanged(true) }
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const v = (s: string) => s.trim() || null
    const body = { companyNm: form.companyNm.trim(), ceoNm: form.ceoNm.trim(), bizType: v(form.bizType), bizItem: v(form.bizItem),
                   telNo: v(form.telNo), zipCd: v(form.zipCd), addr: v(form.addr), addrDtl: v(form.addrDtl) }
    try {
      if (companyId == null) {
        const created = await api.createCompany({ ...body, bizRegNo: form.bizRegNo.trim() })
        navigate(`/companies/${created.companyId}`, { state: { notice: '등록되었습니다' } satisfies NoticeState })
        return
      }
      if (!original) return
      await api.updateCompany(companyId, { ...body, modDt: original.modDt })
      navigate(`/companies/${companyId}`, { state: { notice: '수정되었습니다' } satisfies NoticeState })
    } catch (err) { setError(message(err)) }
  }

  const isEdit = companyId != null
  const input = (name: Field, label: string, props: { required?: boolean; maxLength: number; placeholder?: string; numeric?: boolean }) => (
    <div className="col-md-6 mb-3">
      <label className={`form-label${props.required ? ' required' : ''}`} htmlFor={`company-${name}`}>{label}</label>
      <input className="form-control" id={`company-${name}`} maxLength={props.maxLength} required={props.required} placeholder={props.placeholder}
             inputMode={props.numeric ? 'numeric' : undefined} value={form[name]} onChange={(e) => set(name, e.target.value)} />
    </div>
  )

  return (
    <Layout title="기업정보관리">
      <div className="card" id="company-form-card">
        <div className="card-header"><h3 className="card-title" id="company-form-title">{isEdit ? '기업 수정' : '기업 등록'}</h3></div>
        <form id="company-form" noValidate onSubmit={onSubmit}>
          <div className="card-body">
            <div className="row">
              {input('companyNm', '기업명', { required: true, maxLength: 100 })}
              <div className="col-md-6 mb-3">
                <label className={`form-label${isEdit ? '' : ' required'}`} htmlFor="company-bizRegNo">사업자등록번호</label>
                <input className="form-control" id="company-bizRegNo" maxLength={10} inputMode="numeric" required placeholder="숫자 10자리" readOnly={isEdit}
                       value={form.bizRegNo} onChange={(e) => set('bizRegNo', e.target.value)} />
              </div>
              {input('ceoNm', '대표자명', { required: true, maxLength: 50 })}
              {input('telNo', '대표 전화번호', { maxLength: 11, placeholder: '숫자만 9~11자리', numeric: true })}
              {input('bizType', '업태', { maxLength: 100 })}
              {input('bizItem', '종목', { maxLength: 100 })}
              <div className="col-12 mb-3">
                <label className="form-label" htmlFor="company-zipCd">주소</label>
                <div className="input-group mb-2">
                  <input className="form-control" id="company-zipCd" readOnly placeholder="우편번호" aria-label="우편번호" value={form.zipCd} />
                  <button type="button" className="btn" id="btn-postcode"
                          onClick={() => openPostcode((zipCd, addr) => { set('zipCd', zipCd); set('addr', addr); document.getElementById('company-addrDtl')?.focus() })
                            .catch((e: Error) => setError(e.message))}>우편번호 검색</button>
                </div>
                <input className="form-control mb-2" id="company-addr" readOnly placeholder="주소" aria-label="주소" value={form.addr} />
                <input className="form-control" id="company-addrDtl" maxLength={200} placeholder="상세주소" aria-label="상세주소"
                       value={form.addrDtl} onChange={(e) => set('addrDtl', e.target.value)} />
              </div>
            </div>
            <div id="company-message">{error && <div className="alert alert-danger" role="alert">{error}</div>}</div>
          </div>
          <div className="card-footer d-flex gap-2">
            <button type="submit" className="btn btn-primary" id="btn-save">저장</button>
            <Link className="btn" id="btn-cancel" to={isEdit ? `/companies/${companyId}` : '/companies'}
                  onClick={(e) => { if (changed && !confirm('입력한 내용이 사라집니다. 취소하시겠습니까?')) e.preventDefault() }}>취소</Link>
          </div>
        </form>
      </div>
    </Layout>
  )
}
