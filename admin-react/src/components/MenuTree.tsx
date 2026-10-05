import { NavLink } from 'react-router'
import type { MenuNode } from '../api/types'

/** 왼쪽 메뉴 트리. ②③의 menu.tag / admin-jsp.js와 같은 마크업 */
export function MenuTree({ menus }: { menus: MenuNode[] }) {
  return (
    <>
      {menus.map((m) => (
        <li className="nav-item" key={m.menuId}>
          {m.menuTypeCd === 'PAGE' ? (
            <NavLink className="nav-link" to={m.menuUrl ?? '/'} data-menu-cd={m.menuCd}>
              <span className="nav-link-title">{m.menuNm}</span>
            </NavLink>
          ) : (
            <>
              <span className="nav-link menu-folder"><span className="nav-link-title">{m.menuNm}</span></span>
              <ul className="navbar-nav menu-children">
                <MenuTree menus={m.children} />
              </ul>
            </>
          )}
        </li>
      ))}
    </>
  )
}
