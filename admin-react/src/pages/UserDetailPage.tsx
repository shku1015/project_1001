import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import * as api from '../api/user'
import type { UserDetail, UserPrivacy, UserStatusHistory } from '../api/user'
import { getCodes, type CodeItem } from '../api/common'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout, type NoticeState } from '../components/Layout'
import { Modal } from '../components/Modal'
import { formatDateTimeSec, formatMobile } from '../lib/format'

/** 상세로 옮길 때의 알림과, 등록·비밀번호 초기화 직후 한 번만 보여 줄 임시 비밀번호 */
export type UserNoticeState = NoticeState & { tempPassword?: string }

const NO_PERM = '권한이 없습니다'
const mobile = (v: string | null | undefined) => (v && !v.includes('*') ? formatMobile(v) : v || '-')
type ReasonMode = { statusCd: 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN' | null; title: string; warning?: string }

/**
 * SCR-USR-02 회원 상세 (① React). 개인정보는 마스킹 설정대로, [원문 보기](CMP-08)로 이 화면에서만 원문을 본다.
 * 상태 변경·삭제는 사유 입력창(CMP-07). ②③과 같은 마크업이다.
 */
export function UserDetailPage() {
  const me = useMe()
  const userId = Number(useParams().userId)
  const location = useLocation()
  const navigate = useNavigate()
  const perm = {
    edit: can(me, 'USER', 'UPDATE') && can(me, 'USER', 'PRIVACY'), update: can(me, 'USER', 'UPDATE'),
    delete: can(me, 'USER', 'DELETE'), privacy: can(me, 'USER', 'PRIVACY'),
  }
  const [user, setUser] = useState<UserDetail | null>(null)
  const [histories, setHistories] = useState<UserStatusHistory[]>([])
  const [reasons, setReasons] = useState<CodeItem[]>([])
  const [privacy, setPrivacy] = useState<UserPrivacy | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [tempPassword, setTempPassword] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [privacyOpen, setPrivacyOpen] = useState(false)
  const [privacyForm, setPrivacyForm] = useState({ reasonCd: '', reasonEtc: '' })
  const [privacyError, setPrivacyError] = useState<string | null>(null)
  const [reason, setReason] = useState<ReasonMode | null>(null)
  const [reasonText, setReasonText] = useState('')
  const [reasonError, setReasonError] = useState<string | null>(null)
  const message = (e: unknown) => (e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.')

  // 임시 비밀번호는 한 번만: 받아 둔 뒤 주소 상태에서 지운다
  useEffect(() => {
    const state = location.state as UserNoticeState | null
    if (state?.tempPassword) {
      setTempPassword(state.tempPassword)
      navigate(location.pathname, { replace: true, state: { notice: state.notice } satisfies NoticeState })
    }
  }, [location, navigate])

  const load = useCallback(async () => {
    const [u, h] = await Promise.all([api.getUser(userId), api.getStatusHistories(userId)])
    setUser(u); setHistories(h); setPrivacy(null)
  }, [userId])
  useEffect(() => { load().catch((e) => setError(message(e))) }, [load])
  useEffect(() => { getCodes('PRIVACY_REASON').then(setReasons).catch(() => setReasons([])) }, [])

  const notice = (text: string) => navigate(location.pathname, { replace: true, state: { notice: text } satisfies NoticeState })

  const submitPrivacy = async (e: FormEvent) => {
    e.preventDefault()
    setPrivacyError(null)
    try {
      setPrivacy(await api.viewPrivacy(userId, privacyForm.reasonCd, privacyForm.reasonEtc.trim() || null))
      setPrivacyOpen(false)
    } catch (err) { setPrivacyError(message(err)) }
  }
  const openReason = (mode: ReasonMode) => { setReasonText(''); setReasonError(null); setReason(mode) }
  const submitReason = async (e: FormEvent) => {
    e.preventDefault()
    if (!user || !reason) return
    if (!reasonText.trim()) { setReasonError('사유를 입력하세요.'); return }
    try {
      if (reason.statusCd == null) {
        await api.deleteUser(user.userId, reasonText.trim())
        navigate('/users', { state: { notice: '삭제되었습니다' } satisfies NoticeState })
        return
      }
      await api.changeStatus(user.userId, reason.statusCd, reasonText.trim(), user.modDt)
      setReason(null)
      await load()
      notice('상태가 변경되었습니다')
    } catch (err) { setReasonError(message(err)) }
  }
  const resetPassword = async () => {
    if (!user || !confirm('비밀번호를 초기화하시겠습니까?')) return
    try {
      const r = await api.resetPassword(user.userId)
      setTempPassword(r.tempPassword); setCopied(false)
      await load()
      notice('비밀번호가 초기화되었습니다')
    } catch (e) { setError(message(e)) }
  }

  if (!user) {
    return <Layout title="사용자관리">{error && <div className="alert alert-danger" role="alert">{error}</div>}</Layout>
  }
  const u = user
  const withdrawn = u.statusCd === 'WITHDRAWN'
  const permProps = (allowed: boolean) => (allowed ? {} : { disabled: true, title: NO_PERM })
  const reasonButton = (id: string, cls: string, mode: ReasonMode, label: string) => (
    <button type="button" className={`btn btn-sm${cls}`} id={id} {...permProps(mode.statusCd ? perm.update : perm.delete)}
            onClick={() => openReason(mode)}>{label}</button>
  )
  const dl = (rows: [string, string | null, ReactNode][], last = false) => (
    <dl className={`row${last ? ' mb-0' : ''}`}>
      {rows.map(([label, id, value]) => [
        <dt className="col-sm-2" key={`${label}-t`}>{label}</dt>,
        <dd className="col-sm-4" key={`${label}-d`} id={id ? `user-${id}` : undefined}>{value}</dd>,
      ])}
    </dl>
  )
  const p = privacy ?? u

  return (
    <Layout title="사용자관리">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      {tempPassword && (
        <div className="alert alert-warning" role="alert" id="temp-password-box">
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <span>임시 비밀번호:</span><code className="fs-3" id="temp-password">{tempPassword}</code>
            <button type="button" className="btn btn-sm" id="btn-temp-copy"
                    onClick={() => { void navigator.clipboard.writeText(tempPassword); setCopied(true) }}>{copied ? '복사됨' : '복사'}</button>
            <button type="button" className="btn btn-sm ms-auto" id="btn-temp-close" onClick={() => setTempPassword(null)}>닫기</button>
          </div>
          <div className="mt-1 small">이 창을 닫거나 화면을 옮기면 다시 볼 수 없습니다. 회원에게 전달하세요.</div>
        </div>
      )}
      {withdrawn && <div className="alert alert-info" role="note" id="withdrawn-note">탈퇴한 회원입니다. 정보를 수정하거나 상태를 바꿀 수 없습니다.</div>}

      <div className="card mb-3" id="user-info">
        <div className="card-header">
          <h3 className="card-title" id="user-title">{u.loginId}</h3>
          <div className="card-actions btn-list">
            <Link className="btn btn-sm" id="btn-list" to="/users">목록</Link>
            {u.maskedFields.length > 0 && !privacy && (
              <button type="button" className="btn btn-sm" id="btn-privacy" {...permProps(perm.privacy)}
                      onClick={() => { setPrivacyForm({ reasonCd: '', reasonEtc: '' }); setPrivacyError(null); setPrivacyOpen(true) }}>원문 보기</button>
            )}
            {!withdrawn && <>
              {perm.edit
                ? <Link className="btn btn-sm" id="btn-edit" to={`/users/${u.userId}/edit`}>수정</Link>
                : <button type="button" className="btn btn-sm" id="btn-edit" {...permProps(false)}>수정</button>}
              {u.statusCd === 'ACTIVE' && reasonButton('btn-suspend', '', { statusCd: 'SUSPENDED', title: '회원 정지' }, '정지')}
              {u.statusCd === 'SUSPENDED' && reasonButton('btn-resume', '', { statusCd: 'ACTIVE', title: '정지 해제' }, '정지 해제')}
              {u.statusCd === 'DORMANT' && reasonButton('btn-wake', '', { statusCd: 'ACTIVE', title: '휴면 해제' }, '휴면 해제')}
              {reasonButton('btn-withdraw', ' btn-ghost-danger', { statusCd: 'WITHDRAWN', title: '강제 탈퇴', warning: '강제 탈퇴는 되돌릴 수 없습니다.' }, '강제 탈퇴')}
              <button type="button" className="btn btn-sm" id="btn-password-reset" {...permProps(perm.update)} onClick={resetPassword}>비밀번호 초기화</button>
            </>}
            {reasonButton('btn-delete', ' btn-ghost-danger', { statusCd: null, title: '회원 삭제' }, '삭제')}
          </div>
        </div>
        <div className="card-body">
          <h4 className="mb-2">기본 정보</h4>
          {dl([
            ['회원 구분', 'userType', u.userTypeNm], ['로그인 아이디', 'loginId', u.loginId],
            ['상태', 'status', u.statusNm], ['가입 경로', 'joinPath', u.joinPath === 'ADMIN' ? '관리자 등록' : '사용자 서비스'],
            ['가입일시', null, formatDateTimeSec(u.joinDt)], ['최근 로그인', null, formatDateTimeSec(u.lastLoginDt)],
            ['탈퇴일시', 'withdrawDt', formatDateTimeSec(u.withdrawDt)], ['임시 비밀번호', 'pwdTempYn', u.pwdTempYn === 'Y' ? '예' : '아니오'],
          ])}
          <h4 className="mb-2">개인정보{privacy && <span className="badge bg-red-lt ms-1" id="privacy-revealed">원문</span>}</h4>
          <div id="user-privacy">
            {dl([
              ['이름', 'userNm', p.userNm], ['이메일', 'email', p.email],
              ['휴대폰 번호', 'mobileNo', mobile(p.mobileNo)], ['생년월일', 'birthDate', p.birthDate || '-'],
            ])}
          </div>
          {u.userTypeCd === 'CORPORATE' && <>
            <h4 className="mb-2">소속 정보</h4>
            {dl([
              ['소속 기업', 'company', <Link key="company" to={`/companies/${u.companyId}`}>{u.companyNm}</Link>],
              ['부서 / 직위', 'dept', `${u.deptNm || '-'} / ${u.positionNm || '-'}`],
            ])}
          </>}
          <h4 className="mb-2">관리 정보</h4>
          {dl([['등록', null, `${u.regNm || '-'} · ${formatDateTimeSec(u.regDt)}`], ['수정', null, `${u.modNm || '-'} · ${formatDateTimeSec(u.modDt)}`]], true)}
        </div>
      </div>

      <div className="card" id="status-history">
        <div className="card-header"><h3 className="card-title">상태 변경 이력</h3></div>
        <div className="table-responsive">
          <table className="table table-vcenter mb-0" id="history-table">
            <thead><tr><th>변경일시</th><th>변경 전 → 후</th><th>사유</th><th>처리자</th></tr></thead>
            <tbody id="history-tbody">
              {histories.map((h, i) => (
                <tr key={i}><td>{formatDateTimeSec(h.regDt)}</td><td>{h.beforeStatusNm} → {h.afterStatusNm}</td><td>{h.reason}</td><td>{h.regNm || '-'}</td></tr>
              ))}
              {histories.length === 0 && <tr><td colSpan={4} className="text-secondary text-center">상태 변경 이력이 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <Modal id="privacy-modal" open={privacyOpen} title="개인정보 원문 보기" onClose={() => setPrivacyOpen(false)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="privacy-form" className="btn btn-primary" id="btn-privacy-save">원문 보기</button></>}>
        <form className="modal-body" id="privacy-form" noValidate onSubmit={submitPrivacy}>
          <p className="text-secondary">원문 보기는 감사로그에 남습니다.</p>
          <label className="form-label required" htmlFor="privacy-reasonCd">열람 사유</label>
          <select className="form-select" id="privacy-reasonCd" value={privacyForm.reasonCd} onChange={(e) => setPrivacyForm({ ...privacyForm, reasonCd: e.target.value })}>
            <option value="">선택</option>
            {reasons.map((r) => <option value={r.code} key={r.code}>{r.codeNm}</option>)}
          </select>
          <div className="mt-2" id="privacy-etc-wrap" hidden={privacyForm.reasonCd !== 'ETC'}>
            <label className="form-label required" htmlFor="privacy-reasonEtc">기타 사유</label>
            <input className="form-control" id="privacy-reasonEtc" maxLength={200} value={privacyForm.reasonEtc} onChange={(e) => setPrivacyForm({ ...privacyForm, reasonEtc: e.target.value })} />
          </div>
          <div id="privacy-message">{privacyError && <div className="alert alert-danger mt-2" role="alert">{privacyError}</div>}</div>
        </form>
      </Modal>

      <Modal id="reason-modal" open={!!reason} title={reason?.title ?? '사유 입력'} onClose={() => setReason(null)}
             footer={<><button type="button" className="btn" data-bs-dismiss="modal">취소</button><button type="submit" form="reason-form" className="btn btn-primary" id="btn-reason-save">확인</button></>}>
        <form className="modal-body" id="reason-form" noValidate onSubmit={submitReason}>
          <div className="alert alert-warning" role="alert" id="reason-warning" hidden={!reason?.warning}>{reason?.warning}</div>
          <label className="form-label required" htmlFor="reason-input">사유</label>
          <textarea className="form-control" id="reason-input" rows={3} maxLength={500} value={reasonText} onChange={(e) => setReasonText(e.target.value)} />
          <div id="reason-message">{reasonError && <div className="alert alert-danger mt-2" role="alert">{reasonError}</div>}</div>
        </form>
      </Modal>
    </Layout>
  )
}
