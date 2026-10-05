import { Link } from 'react-router'
import { useMe } from '../auth/AuthContext'
import { Layout } from '../components/Layout'
import { formatDate, formatDateTime, pageMenus } from '../lib/format'

/** SCR-HOME 홈 (① React) */
export function HomePage() {
  const me = useMe()
  const shortcuts = pageMenus(me.menus).slice(0, 6)

  return (
    <Layout title="홈">
      <div className="row row-cards">
        <div className="col-md-6">
          <div className="card">
            <div className="card-header"><h3 className="card-title">내 정보</h3></div>
            <div className="card-body">
              <dl className="row mb-0">
                <dt className="col-5">이름</dt>
                <dd className="col-7" id="home-name">{me.adminNm}</dd>
                <dt className="col-5">역할</dt>
                <dd className="col-7" id="home-roles">{me.roles.length ? me.roles.map((r) => r.roleNm).join(', ') : '-'}</dd>
                <dt className="col-5">마지막 로그인</dt>
                <dd className="col-7" id="home-last-login">
                  {me.lastLoginDt ? `${formatDateTime(me.lastLoginDt)} (${me.lastLoginIp})` : '이전 로그인 기록이 없습니다'}
                </dd>
                <dt className="col-5">비밀번호 변경일</dt>
                <dd className="col-7" id="home-pwd-changed">{formatDate(me.pwdChangedDt)}</dd>
              </dl>
            </div>
          </div>
        </div>
        <div className="col-md-6">
          <div className="card">
            <div className="card-header"><h3 className="card-title">바로가기</h3></div>
            <div className="card-body shortcut-list d-flex flex-wrap gap-2" id="home-shortcuts">
              {shortcuts.map((m) => (
                <Link key={m.menuId} className="btn btn-outline-primary" to={m.menuUrl ?? '/'}>{m.menuNm}</Link>
              ))}
              {shortcuts.length === 0 && (
                <p className="text-secondary mb-0">사용 가능한 메뉴가 없습니다. 관리자에게 권한을 요청하세요</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}
