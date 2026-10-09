import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import * as api from '../api/company'
import type { CompanyDetail, CompanyUsers } from '../api/company'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout, type NoticeState } from '../components/Layout'
import { Modal } from '../components/Modal'
import { formatBizRegNo, formatDateTime, formatDateTimeSec } from '../lib/format'

const NO_PERM = '권한이 없습니다'

/** SCR-COM-02 기업 상세 (① React). 정지·정지 해제는 사유 입력창(CMP-07). ②③과 같은 마크업 */
export function CompanyDetailPage() {
  const me = useMe()
  const companyId = Number(useParams().companyId)
  const location = useLocation()
  const navigate = useNavigate()
  const perm = { update: can(me, 'COMPANY', 'UPDATE'), delete: can(me, 'COMPANY', 'DELETE') }
  const [company, setCompany] = useState<CompanyDetail | null>(null)
  const [users, setUsers] = useState<CompanyUsers | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reason, setReason] = useState<{ statusCd: 'ACTIVE' | 'SUSPENDED'; title: string } | null>(null)
  const [reasonText, setReasonText] = useState('')
  const [reasonError, setReasonError] = useState<string | null>(null)
  const message = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')

  const load = useCallback(async () => {
    const [c, u] = await Promise.all([api.getCompany(companyId), api.getUsers(companyId, 10)])
    setCompany(c); setUsers(u)
  }, [companyId])
  useEffect(() => { load().catch((e) => setError(message(e))) }, [load])

  const openReason = (statusCd: 'ACTIVE' | 'SUSPENDED', title: string) => {
    setReasonText(''); setReasonError(null); setReason({ statusCd, title })
  }
  const submitReason = async (e: FormEvent) => {
    e.preventDefault()
    if (!company || !reason) return
    if (!reasonText.trim()) { setReasonError('사유를 입력하세요.'); return }
    try {
      await api.changeStatus(company.companyId, reason.statusCd, reasonText.trim(), company.modDt)
      setReason(null)
      await load()
      navigate(location.pathname, { replace: true, state: { notice: reason.statusCd === 'SUSPENDED' ? '정지되었습니다' : '정지 해제되었습니다' } satisfies NoticeState })
    } catch (err) { setReasonError(message(err)) }
  }
  const remove = async () => {
    if (!company || !confirm('기업을 삭제하시겠습니까?')) return
    try {
      await api.deleteCompany(company.companyId)
      navigate('/companies', { state: { notice: '삭제되었습니다' } satisfies NoticeState })
    } catch (e) { setError(message(e)) }
  }

  if (!company || !users) {
    return <Layout title="기업정보관리">{error && <div className="alert alert-danger" role="alert">{error}</div>}</Layout>
  }
  const c = company
  const permProps = (allowed: boolean) => (allowed ? {} : { disabled: true, title: NO_PERM })
  const info: [string, string, string | number][] = [
    ['기업명', 'companyNm', c.companyNm], ['사업자등록번호', 'bizRegNo', formatBizRegNo(c.bizRegNo)],
    ['대표자명', 'ceoNm', c.ceoNm], ['상태', 'status', c.statusNm ?? ''],
    ['업태', 'bizType', c.bizType || '-'], ['종목', 'bizItem', c.bizItem || '-'],
    ['대표 전화번호', 'telNo', c.telNo || '-'], ['소속 회원 수', 'memberCnt', c.memberCnt],
  ]

  return (
    <Layout title="기업정보관리">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="card mb-3" id="company-info">
        <div className="card-header">
          <h3 className="card-title" id="company-title">{c.companyNm}</h3>
          <div className="card-actions btn-list">
            <Link className="btn btn-sm" id="btn-list" to="/companies">목록</Link>
            {perm.update
              ? <Link className="btn btn-sm" id="btn-edit" to={`/companies/${c.companyId}/edit`}>수정</Link>
              : <button type="button" className="btn btn-sm" id="btn-edit" {...permProps(false)}>수정</button>}
            {c.statusCd === 'ACTIVE'
              ? <button type="button" className="btn btn-sm btn-ghost-danger" id="btn-suspend" {...permProps(perm.update)} onClick={() => openReason('SUSPENDED', '기업 정지')}>정지</button>
              : <button type="button" className="btn btn-sm" id="btn-resume" {...permProps(perm.update)} onClick={() => openReason('ACTIVE', '정지 해제')}>정지 해제</button>}
            {c.memberCnt === 0 && <button type="button" className="btn btn-sm btn-ghost-danger" id="btn-delete" {...permProps(perm.delete)} onClick={remove}>삭제</button>}
          </div>
        </div>
        <div className="card-body">
          {c.memberCnt > 0 && <div className="alert alert-info" role="note" id="delete-note">소속 회원이 있어 삭제할 수 없습니다.</div>}
          <dl className="row mb-0">
            {info.map(([label, id, value]) => [
              <dt className="col-sm-2" key={`${id}-t`}>{label}</dt>,
              <dd className="col-sm-4" key={`${id}-d`} id={`company-${id}`}>{value}</dd>,
            ])}
            <dt className="col-sm-2">주소</dt>
            <dd className="col-sm-10" id="company-addr">{c.addr ? `(${c.zipCd}) ${c.addr} ${c.addrDtl ?? ''}` : '-'}</dd>
            <dt className="col-sm-2">등록</dt><dd className="col-sm-4">{c.regNm || '-'} · {formatDateTimeSec(c.regDt)}</dd>
            <dt className="col-sm-2">수정</dt><dd className="col-sm-4">{c.modNm || '-'} · {formatDateTimeSec(c.modDt)}</dd>
          </dl>
        </div>
      </div>

      <div className="card" id="company-users">
        <div className="card-header">
          <h3 className="card-title">소속 회원 <span className="text-secondary" id="users-total">({users.totalCount}명)</span></h3>
          <div className="card-actions"><Link className="btn btn-sm" id="btn-users-all" to={`/users?companyId=${c.companyId}`}>전체 보기</Link></div>
        </div>
        <div className="table-responsive">
          <table className="table table-vcenter mb-0" id="users-table">
            <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서 / 직위</th><th>상태</th><th>가입일시</th></tr></thead>
            <tbody>
              {users.items.map((u) => (
                <tr key={u.userId}>
                  <td><Link to={`/users/${u.userId}`}>{u.loginId}</Link></td><td>{u.userNm}</td>
                  <td>{u.deptNm || '-'} / {u.positionNm || '-'}</td><td>{u.statusNm}</td><td>{formatDateTime(u.joinDt)}</td>
                </tr>
              ))}
              {users.items.length === 0 && <tr><td colSpan={5} className="text-secondary text-center">소속 회원이 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <Modal id="reason-modal" open={!!reason} title={reason?.title ?? '사유 입력'} onClose={() => setReason(null)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="reason-form" className="btn btn-primary" id="btn-reason-save">확인</button></>}>
        <form className="modal-body" id="reason-form" noValidate onSubmit={submitReason}>
          <label className="form-label required" htmlFor="reason-input">사유</label>
          <textarea className="form-control" id="reason-input" rows={3} maxLength={500} value={reasonText} onChange={(e) => setReasonText(e.target.value)} />
          <div id="reason-message">{reasonError && <div className="alert alert-danger mt-2" role="alert">{reasonError}</div>}</div>
        </form>
      </Modal>
    </Layout>
  )
}
