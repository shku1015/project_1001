/*
 * ② JSP + API 페이지 공통: 로그인 확인, 레이아웃(왼쪽 메뉴·내 이름) 채우기, 알림, 날짜 형식.
 * ③ SSR의 layout.tag/menu.tag와 같은 마크업을 만든다.
 */
(function (global) {
  'use strict';

  var PREFIX = '/jsp';

  function el(tag, attrs, text) {
    var node = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) { node.setAttribute(k, attrs[k]); });
    if (text !== undefined) {
      node.textContent = text;
    }
    return node;
  }

  /** 왼쪽 메뉴 트리 (menu.tag와 같은 구조) */
  function renderMenus(container, menus) {
    menus.forEach(function (m) {
      var li = el('li', { 'class': 'nav-item' });
      if (m.menuTypeCd === 'PAGE') {
        var a = el('a', { 'class': 'nav-link', href: PREFIX + m.menuUrl, 'data-menu-cd': m.menuCd });
        a.appendChild(el('span', { 'class': 'nav-link-title' }, m.menuNm));
        li.appendChild(a);
      } else {
        var folder = el('span', { 'class': 'nav-link menu-folder' });
        folder.appendChild(el('span', { 'class': 'nav-link-title' }, m.menuNm));
        var ul = el('ul', { 'class': 'navbar-nav menu-children' });
        renderMenus(ul, m.children);
        li.appendChild(folder);
        li.appendChild(ul);
      }
      container.appendChild(li);
    });
  }

  /** 홈 바로가기: 화면 메뉴 중 앞쪽 6개 */
  function pageMenus(menus, out) {
    out = out || [];
    menus.forEach(function (m) {
      if (m.menuTypeCd === 'PAGE') {
        out.push(m);
      } else {
        pageMenus(m.children, out);
      }
    });
    return out;
  }

  function renderLayout(me) {
    var side = document.getElementById('side-menu');
    if (side) {
      side.innerHTML = '';
      renderMenus(side, me.menus);
    }
    var name = document.getElementById('user-name');
    if (name) {
      name.textContent = me.adminNm;
    }
    var role = document.getElementById('user-role');
    if (role) {
      role.textContent = me.roles.length ? '(' + me.roles[0].roleNm + ')' : '';
    }
    var logout = document.getElementById('logout-button');
    if (logout) {
      logout.addEventListener('click', async function () {
        await AdminApi.logout();
        location.href = PREFIX + '/login?logout';
      });
    }
  }

  /**
   * 로그인이 필요한 페이지의 시작점.
   * 토큰을 재발급받고 내 정보를 읽는다. 실패하면 로그인 화면, 임시 비밀번호면 비밀번호 변경 화면으로 보낸다.
   * @param {{allowTempPassword?: boolean}} options
   * @returns {Promise<object>} 내 정보 (api/openapi.yaml Me)
   */
  async function requireLogin(options) {
    options = options || {};
    var me;
    try {
      await AdminApi.refresh();
      me = await AdminApi.me();
    } catch (e) {
      location.replace(PREFIX + '/login');
      return new Promise(function () {});
    }
    if (me.pwdChangeRequired && !options.allowTempPassword) {
      location.replace(PREFIX + '/password');
      return new Promise(function () {});
    }
    renderLayout(me);
    return me;
  }

  function showNotice(message, kind) {
    var area = document.getElementById('notice-area');
    if (!area) {
      return;
    }
    area.innerHTML = '';
    area.appendChild(el('div', { 'class': 'alert alert-' + (kind || 'success'), role: kind === 'danger' ? 'alert' : 'status' }, message));
  }

  /** 다른 페이지로 옮긴 뒤 한 번 보여 줄 알림 (③의 Flash 메시지와 같은 역할) */
  function flash(message) {
    sessionStorage.setItem('admin.notice', message);
  }

  function showFlash() {
    var message = sessionStorage.getItem('admin.notice');
    if (message) {
      sessionStorage.removeItem('admin.notice');
      showNotice(message);
    }
  }

  /** "2026-10-04T14:30:15" → "2026-10-04 14:30" (docs/05-ia-screens.md 4.1) */
  function formatDateTime(value) {
    return value ? value.replace('T', ' ').substring(0, 16) : '-';
  }

  /** "2026-10-04T14:30:15" → "2026-10-04 14:30:15" (상세 화면) */
  function formatDateTimeSec(value) {
    return value ? value.replace('T', ' ').substring(0, 19) : '-';
  }

  /** "01012345678" → "010-1234-5678" */
  function formatMobile(value) {
    if (!value) { return '-'; }
    return value.length === 11 ? value.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3')
      : value.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
  }

  function formatDate(value) {
    return value ? value.substring(0, 10) : '-';
  }

  global.AdminJsp = {
    PREFIX: PREFIX,
    requireLogin: requireLogin,
    pageMenus: pageMenus,
    showNotice: showNotice,
    flash: flash,
    showFlash: showFlash,
    formatDateTime: formatDateTime,
    formatDate: formatDate,
    formatDateTimeSec: formatDateTimeSec,
    formatMobile: formatMobile,
    el: el
  };
})(window);
