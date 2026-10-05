import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router'
import { useAuth, useMe } from '../auth/AuthContext'
import { MenuTree } from './MenuTree'

/** 화면 이동 후 한 번 보여 줄 알림 (②③의 Flash 메시지와 같은 역할) */
export type NoticeState = { notice?: string }

/**
 * 공통 레이아웃 (docs/05-ia-screens.md 3절). ②③의 layout.tag와 같은 마크업이다.
 */
export function Layout({ title, children }: { title: string; children: ReactNode }) {
  const me = useMe()
  const { logout } = useAuth()
  const notice = (useLocation().state as NoticeState | null)?.notice

  // 로그아웃하면 RequireAuth가 로그인 화면(/login?logout)으로 보낸다
  const onLogout = () => logout()

  return (
    <div className="page">
      <aside className="navbar navbar-vertical navbar-expand-lg" data-bs-theme="dark">
        <div className="container-fluid">
          <h1 className="navbar-brand">
            <Link to="/" className="text-white text-decoration-none">관리자 서비스</Link>
          </h1>
          <div className="navbar-collapse">
            <ul className="navbar-nav pt-lg-3" id="side-menu" aria-label="메뉴">
              <MenuTree menus={me.menus} />
            </ul>
          </div>
        </div>
      </aside>

      <header className="navbar navbar-expand-md d-print-none">
        <div className="container-xl justify-content-end">
          <div className="nav-item dropdown" id="user-menu">
            <a href="#" className="nav-link" data-bs-toggle="dropdown" data-bs-display="static" aria-label="내 메뉴"
               onClick={(e) => e.preventDefault()}>
              <span id="user-name">{me.adminNm}</span>
              <span className="text-secondary ms-1" id="user-role">{me.roles.length ? `(${me.roles[0].roleNm})` : ''}</span>
            </a>
            <div className="dropdown-menu dropdown-menu-end">
              <Link className="dropdown-item" to="/me">내 정보</Link>
              <Link className="dropdown-item" to="/password">비밀번호 변경</Link>
              <button type="button" className="dropdown-item" onClick={onLogout}>로그아웃</button>
            </div>
          </div>
        </div>
      </header>

      <div className="page-wrapper">
        <div className="page-header">
          <div className="container-xl">
            <ol className="breadcrumb" aria-label="경로">
              <li className="breadcrumb-item"><Link to="/">홈</Link></li>
              {title !== '홈' && <li className="breadcrumb-item active">{title}</li>}
            </ol>
            <h2 className="page-title">{title}</h2>
          </div>
        </div>
        <div className="page-body">
          <div className="container-xl">
            <div id="notice-area">
              {notice && <div className="alert alert-success" role="status">{notice}</div>}
            </div>
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}
