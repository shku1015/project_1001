import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import * as api from '../api/admin'
import type { AdminDetail, AdminLoginHistory, AdminRoleOption } from '../api/admin'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout, type NoticeState } from '../components/Layout'
import { formatDateTime, formatDateTimeSec, formatMobile } from '../lib/format'

/** 상세로 옮길 때의 알림과, 등록·비밀번호 초기화 직후 한 번만 보여 줄 임시 비밀번호 */
export type AdminNoticeState = NoticeState & { tempPassword?: string }

const NO_PERM = '권한이 없습니다'

/**
 * SCR-ADM-02 관리자 상세 (① React). ②③과 같은 마크업이다.
 * 보호 규칙(R3, R5)은 화면에서 미리 판단하지 않고, 서버가 거부하면 이유를 보여 준다.
 */
export function AdminDetailPage() {
  const me = useMe()
  const adminId = Number(useParams().adminId)
  const location = useLocation()
  const navigate = useNavigate()
  const perm = { update: can(me, 'ADMIN', 'UPDATE'), delete: can(me, 'ADMIN', 'DELETE') }
  const [admin, setAdmin] = useState<AdminDetail | null>(null)
  const [options, setOptions] = useState<AdminRoleOption[]>([])
  const [histories, setHistories] = useState<AdminLoginHistory[]>([])
  const [error, setError] = useState<string | null>(null)
  const [tempPassword, setTempPassword] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [roleToAdd, setRoleToAdd] = useState('')

  // 임시 비밀번호는 한 번만: 받아 둔 뒤 주소 상태에서 지운다 (새로 고쳐도 다시 보이지 않게)
  useEffect(() => {
    const state = location.state as AdminNoticeState | null
    if (state?.tempPassword) {
      setTempPassword(state.tempPassword)
      navigate(location.pathname, { replace: true, state: { notice: state.notice } satisfies NoticeState })
    }
  }, [location, navigate])

  const load = useCallback(async () => {
    const [a, o, h] = await Promise.all([api.getAdmin(adminId), api.getRoleOptions(), api.getLoginHistories(adminId)])
    setAdmin(a); setOptions(o); setHistories(h); setError(null)
  }, [adminId])
  useEffect(() => { load().catch((e) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.')) }, [load])

  /** 작업 뒤 상세를 다시 읽어 알림을 보여 준다. 서버가 거부하면 이유를 보여 준다 */
  const act = async (question: string | null, call: () => Promise<unknown>, notice: string) => {
    if (question && !confirm(question)) return
    try {
      await call()
      await load()
      navigate(location.pathname, { replace: true, state: { notice } satisfies NoticeState })
    } catch (e) { setError(e instanceof ApiError ? e.displayMessage : '처리할 수 없습니다.') }
  }

  if (!admin) {
    return <Layout title="관리자">{error && <div className="alert alert-danger" role="alert">{error}</div>}</Layout>
  }
  const a = admin
  const permProps = (allowed: boolean) => (allowed ? {} : { disabled: true, title: NO_PERM })
  const addable = options.filter((o) => o.assignable && !a.roles.some((r) => r.roleId === o.roleId))
  const onAddRole = (e: FormEvent) => {
    e.preventDefault()
    const roleId = Number(roleToAdd || addable[0]?.roleId)
    if (roleId) void act(null, () => api.grantRole(a.adminId, roleId), '역할이 부여되었습니다')
  }
  const resetPassword = () => act('비밀번호를 초기화하시겠습니까? 현재 로그인도 종료됩니다.', async () => {
    const r = await api.resetPassword(a.adminId)
    setTempPassword(r.tempPassword); setCopied(false)
  }, '비밀번호가 초기화되었습니다')

  const info: [string, string | null, string][] = [
    ['로그인 아이디', 'loginId', a.loginId], ['상태', 'status', a.statusNm ?? ''],
    ['이름', 'adminNm', a.adminNm], ['이메일', 'email', a.email],
    ['휴대폰 번호', 'mobileNo', formatMobile(a.mobileNo)], ['부서', 'deptNm', a.deptNm || '-'],
    ['로그인 실패', 'loginFailCnt', `${a.loginFailCnt}회`], ['임시 비밀번호', 'pwdTempYn', a.pwdTempYn === 'Y' ? '예' : '아니오'],
    ['비밀번호 변경', null, formatDateTimeSec(a.pwdChangedDt)], ['마지막 로그인', null, formatDateTimeSec(a.lastLoginDt)],
    ['등록', null, `${a.regNm || '-'} · ${formatDateTimeSec(a.regDt)}`], ['수정', null, `${a.modNm || '-'} · ${formatDateTimeSec(a.modDt)}`],
  ]

  return (
    <Layout title="관리자">
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      {tempPassword && (
        <div className="alert alert-warning" role="alert" id="temp-password-box">
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <span>임시 비밀번호:</span><code className="fs-3" id="temp-password">{tempPassword}</code>
            <button type="button" className="btn btn-sm" id="btn-temp-copy"
                    onClick={() => { void navigator.clipboard.writeText(tempPassword); setCopied(true) }}>{copied ? '복사됨' : '복사'}</button>
            <button type="button" className="btn btn-sm ms-auto" id="btn-temp-close" onClick={() => setTempPassword(null)}>닫기</button>
          </div>
          <div className="mt-1 small">이 창을 닫거나 화면을 옮기면 다시 볼 수 없습니다. 관리자에게 전달하세요.</div>
        </div>
      )}

      <div className="card mb-3" id="admin-info">
        <div className="card-header">
          <h3 className="card-title" id="admin-title">{a.adminNm} <span className="text-secondary ms-1">({a.loginId})</span>
            {a.self && <span className="badge bg-blue-lt ms-2">본인</span>}</h3>
          <div className="card-actions btn-list">
            <Link className="btn btn-sm" id="btn-list" to="/admins">목록</Link>
            {perm.update
              ? <Link className="btn btn-sm" id="btn-edit" to={`/admins/${a.adminId}/edit`}>수정</Link>
              : <button type="button" className="btn btn-sm" id="btn-edit" {...permProps(false)}>수정</button>}
            {a.statusCd === 'LOCKED' && (
              <button type="button" className="btn btn-sm" id="btn-unlock" {...permProps(perm.update)}
                      onClick={() => act('잠금을 해제하시겠습니까?', () => api.unlock(a.adminId), '잠금이 해제되었습니다')}>잠금 해제</button>
            )}
            {a.statusCd !== 'DISABLED' && (
              <button type="button" className="btn btn-sm" id="btn-password-reset" {...permProps(perm.update)} onClick={resetPassword}>비밀번호 초기화</button>
            )}
            {!a.self && a.statusCd !== 'DISABLED' && (
              <button type="button" className="btn btn-sm btn-ghost-danger" id="btn-disable" {...permProps(perm.delete)}
                      onClick={() => act('사용중지하시겠습니까? 현재 로그인도 종료됩니다.', () => api.changeStatus(a.adminId, 'DISABLED', a.modDt), '사용중지되었습니다')}>사용중지</button>
            )}
            {a.statusCd === 'DISABLED' && (
              <button type="button" className="btn btn-sm" id="btn-enable" {...permProps(perm.delete)}
                      onClick={() => act('다시 사용하게 하시겠습니까?', () => api.changeStatus(a.adminId, 'ACTIVE', a.modDt), '재사용 처리되었습니다')}>재사용</button>
            )}
          </div>
        </div>
        <div className="card-body">
          <dl className="row mb-0">
            {info.map(([label, id, value]) => [
              <dt className="col-sm-2" key={`${label}-t`}>{label}</dt>,
              <dd className="col-sm-4" key={`${label}-d`} id={id ? `admin-${id}` : undefined}>{value}</dd>,
            ])}
          </dl>
        </div>
      </div>

      <div className="card mb-3" id="admin-roles">
        <div className="card-header">
          <h3 className="card-title">역할</h3>
          {!a.self && (
            <div className="card-actions">
              <form className="d-flex gap-2" id="role-add-form" onSubmit={onAddRole}>
                <select className="form-select form-select-sm" id="role-add-select" aria-label="추가할 역할" disabled={!perm.update}
                        value={roleToAdd} onChange={(e) => setRoleToAdd(e.target.value)}>
                  {addable.map((o) => <option value={o.roleId} key={o.roleId}>{o.roleNm}</option>)}
                </select>
                <button type="submit" className="btn btn-sm btn-primary text-nowrap" id="btn-role-add"
                        disabled={!perm.update || addable.length === 0} title={perm.update ? undefined : NO_PERM}>역할 추가</button>
              </form>
            </div>
          )}
        </div>
        <div className="table-responsive">
          <table className="table table-vcenter mb-0" id="role-table">
            <thead><tr><th>역할명</th><th>부여자</th><th>부여일시</th><th /></tr></thead>
            <tbody>
              {a.roles.map((r) => (
                <tr data-role-id={r.roleId} key={r.roleId}>
                  <td>{r.roleNm}{r.useYn === 'N' && <span className="badge bg-secondary-lt ms-1">사용 안 함</span>}</td>
                  <td>{r.regNm || '-'}</td><td>{formatDateTime(r.regDt)}</td>
                  <td className="text-end">
                    {!a.self && a.roles.length >= 2 && (
                      <button type="button" className="btn btn-sm btn-ghost-danger btn-role-revoke" {...permProps(perm.update)}
                              onClick={() => act('역할을 회수하시겠습니까?', () => api.revokeRole(a.adminId, r.roleId), '역할이 회수되었습니다')}>회수</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card" id="login-history">
        <div className="card-header"><h3 className="card-title">최근 로그인 이력</h3></div>
        <div className="table-responsive">
          <table className="table table-vcenter mb-0" id="history-table">
            <thead><tr><th>일시</th><th>결과</th><th>방식</th><th>IP</th></tr></thead>
            <tbody>
              {histories.map((h, i) => (
                <tr key={i}><td>{formatDateTimeSec(h.regDt)}</td><td>{h.resultNm}</td><td>{h.authTypeCd === 'SESSION' ? '세션' : '토큰'}</td><td>{h.ipAddr}</td></tr>
              ))}
              {histories.length === 0 && <tr><td colSpan={4} className="text-secondary text-center">로그인 이력이 없습니다</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  )
}
