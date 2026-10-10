import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import * as api from '../api/user'
import type { UserCompanyOption, UserForm } from '../api/user'
import { ApiError } from '../api/client'
import { Layout } from '../components/Layout'
import type { UserNoticeState } from './UserDetailPage'

const EMPTY = { userTypeCd: 'PERSONAL', loginId: '', userNm: '', email: '', mobileNo: '', birthDate: '', companyId: '', deptNm: '', positionNm: '' }
type Field = keyof typeof EMPTY

/**
 * SCR-USR-03 회원 등록·수정 (① React). userId가 있으면 수정. 개인정보를 원문으로 다룬다 (CREATE·UPDATE + PRIVACY).
 * 소속 기업은 정상 기업 선택 상자(기업 회원만). ②③과 같은 마크업이다.
 */
export function UserFormPage() {
  const params = useParams()
  const userId = params.userId ? Number(params.userId) : null
  const navigate = useNavigate()
  const [original, setOriginal] = useState<UserForm | null>(null)
  const [companies, setCompanies] = useState<UserCompanyOption[]>([])
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const message = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')

  useEffect(() => {
    api.getCompanyOptions().then(setCompanies).catch((e) => setError(message(e)))
    if (userId == null) return
    api.getForm(userId).then((u) => {
      setOriginal(u)
      setForm({ userTypeCd: u.userTypeCd, loginId: u.loginId, userNm: u.userNm, email: u.email, mobileNo: u.mobileNo ?? '',
                birthDate: u.birthDate ?? '', companyId: u.companyId ? String(u.companyId) : '', deptNm: u.deptNm ?? '', positionNm: u.positionNm ?? '' })
    }).catch((e) => setError(message(e)))
  }, [userId])

  const set = (name: Field, value: string) => setForm((f) => ({ ...f, [name]: value }))
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const v = (s: string) => s.trim() || null
    const body = { userNm: form.userNm.trim(), email: form.email.trim(), mobileNo: v(form.mobileNo), birthDate: form.birthDate || null,
                   companyId: form.companyId ? Number(form.companyId) : null, deptNm: v(form.deptNm), positionNm: v(form.positionNm) }
    try {
      if (userId == null) {
        const created = await api.createUser({ ...body, userTypeCd: form.userTypeCd as 'PERSONAL' | 'CORPORATE', loginId: form.loginId.trim() })
        navigate(`/users/${created.userId}`, { state: { notice: '등록되었습니다', tempPassword: created.tempPassword } satisfies UserNoticeState })
        return
      }
      if (!original) return
      await api.updateUser(userId, { ...body, modDt: original.modDt })
      navigate(`/users/${userId}`, { state: { notice: '수정되었습니다' } satisfies UserNoticeState })
    } catch (err) { setError(message(err)) }
  }

  const isEdit = userId != null
  const input = (name: Field, label: string, props: { required?: boolean; maxLength?: number; type?: string; placeholder?: string; col?: number }) => (
    <div className={`col-md-${props.col ?? 6} mb-3`}>
      <label className={`form-label${props.required ? ' required' : ''}`} htmlFor={`user-${name}`}>{label}</label>
      <input className="form-control" id={`user-${name}`} type={props.type ?? 'text'} maxLength={props.maxLength} required={props.required}
             placeholder={props.placeholder} value={form[name]} onChange={(e) => set(name, e.target.value)} />
    </div>
  )
  // 지금 소속 기업이 정지돼 목록에 없으면 "(현재)"로 함께 보여 준다
  const currentMissing = original?.companyId && !companies.some((c) => c.companyId === original.companyId)

  return (
    <Layout title="사용자관리">
      <div className="card" id="user-form-card">
        <div className="card-header"><h3 className="card-title" id="user-form-title">{isEdit ? '회원 수정' : '회원 등록'}</h3></div>
        <form id="user-form" noValidate onSubmit={onSubmit}>
          <div className="card-body">
            <div className="row">
              <div className="col-md-6 mb-3">
                <label className={`form-label${isEdit ? '' : ' required'}`}>회원 구분</label>
                {isEdit ? <div className="form-control-plaintext" id="user-type">{form.userTypeCd === 'CORPORATE' ? '기업' : '개인'}</div> : (
                  <div id="user-type-choice">
                    {(['PERSONAL', 'CORPORATE'] as const).map((t) => (
                      <label className="form-check form-check-inline" key={t}>
                        <input className="form-check-input" type="radio" name="userTypeCd" id={`user-type-${t}`} checked={form.userTypeCd === t}
                               onChange={() => set('userTypeCd', t)} /> {t === 'PERSONAL' ? '개인' : '기업'}
                      </label>
                    ))}
                  </div>
                )}
              </div>
              <div className="col-md-6 mb-3">
                <label className={`form-label${isEdit ? '' : ' required'}`} htmlFor="user-loginId">로그인 아이디</label>
                <input className="form-control" id="user-loginId" maxLength={50} required placeholder="영문 소문자·숫자 4~50자" readOnly={isEdit}
                       value={form.loginId} onChange={(e) => set('loginId', e.target.value)} />
              </div>
              {input('userNm', '이름', { required: true, maxLength: 50 })}
              {input('email', '이메일', { required: true, maxLength: 100, type: 'email' })}
              {input('mobileNo', '휴대폰 번호', { maxLength: 11, placeholder: '숫자만 10~11자리' })}
              {input('birthDate', '생년월일', { type: 'date' })}
            </div>
            <div className="row" id="corporate-fields" hidden={form.userTypeCd !== 'CORPORATE'}>
              <div className="col-md-6 mb-3">
                <label className="form-label required" htmlFor="user-companyId">소속 기업</label>
                <select className="form-select" id="user-companyId" value={form.companyId} onChange={(e) => set('companyId', e.target.value)}>
                  <option value="">선택 (정상 기업)</option>
                  {companies.map((c) => <option value={c.companyId} key={c.companyId}>{c.companyNm}</option>)}
                  {currentMissing && <option value={original.companyId ?? ''}>{original.companyNm} (현재)</option>}
                </select>
              </div>
              {input('deptNm', '부서', { maxLength: 100, col: 3 })}
              {input('positionNm', '직위', { maxLength: 50, col: 3 })}
            </div>
            {!isEdit && <small className="form-hint d-block mb-2">비밀번호는 입력하지 않습니다. 저장하면 임시 비밀번호가 한 번 표시됩니다.</small>}
            <div id="user-message">{error && <div className="alert alert-danger" role="alert">{error}</div>}</div>
          </div>
          <div className="card-footer d-flex gap-2">
            <button type="submit" className="btn btn-primary" id="btn-save">저장</button>
            <Link className="btn" id="btn-cancel" to={isEdit ? `/users/${userId}` : '/users'}>취소</Link>
          </div>
        </form>
      </div>
    </Layout>
  )
}
