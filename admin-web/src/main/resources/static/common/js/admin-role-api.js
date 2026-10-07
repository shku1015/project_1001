/*
 * ② JSP + API 역할관리 (SCR-ROL-01~03). /api/v1/roles 로 목록·상세·등록·수정을 처리한다.
 * 화면 구조·입력 칸 id는 ③ SSR(ssr/role-*.jsp)·① React(Role*Page.tsx)와 같다.
 * 권한 표의 체크 규칙(BR-03)과 저장 값 모으기는 admin-role-perm.js를 함께 쓴다.
 */
(function () {
  'use strict';

  var BASE = '/roles';
  var PAGE = '/jsp/roles';
  var NO_PERM = '권한이 없습니다';
  var ACTIONS = ['READ', 'CREATE', 'UPDATE', 'DELETE', 'EXCEL', 'PRIVACY'];
  var ACTION_NM = { READ: '조회', CREATE: '등록', UPDATE: '수정', DELETE: '삭제', EXCEL: '엑셀', PRIVACY: '개인정보' };
  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };
  var perm = {};

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  function alertHtml(text) {
    return '<div class="alert alert-danger" role="alert">' + esc(text) + '</div>';
  }

  /** 권한이 없으면 비활성 + "권한이 없습니다" (docs/05-ia-screens.md 4.2) */
  function permAttrs(allowed) {
    return allowed ? '' : ' disabled title="' + NO_PERM + '"';
  }

  // ===================== 목록 (SCR-ROL-01) =====================

  async function initList() {
    var create = byId('btn-role-create');
    if (perm.create) {
      create.classList.remove('disabled');
      create.removeAttribute('aria-disabled');
    } else {
      create.title = NO_PERM;
    }
    byId('role-search').addEventListener('submit', function (e) { e.preventDefault(); loadList(); });
    await loadList();
  }

  async function loadList() {
    var params = new URLSearchParams();
    var keyword = byId('search-keyword').value.trim();
    if (keyword) { params.set('keyword', keyword); }
    if (byId('search-useYn').value) { params.set('useYn', byId('search-useYn').value); }
    var roles = await AdminApi.get(BASE + (params.toString() ? '?' + params : ''));
    byId('role-tbody').innerHTML = roles.length ? roles.map(function (r) {
      return '<tr><td><a href="' + PAGE + '/' + r.roleId + '">' + (r.systemYn === 'Y' ? '🔒 ' : '') + esc(r.roleCd) + '</a></td>' +
        '<td>' + esc(r.roleNm) + '</td><td>' + esc(r.description) + '</td>' +
        '<td class="text-center">' + r.adminCnt + '</td><td class="text-center">' + (r.useYn === 'Y' ? '사용' : '사용 안 함') + '</td></tr>';
    }).join('') : '<tr><td colspan="5" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr>';
  }

  // ===================== 상세 (SCR-ROL-02) =====================

  var role;

  async function initDetail(roleId) {
    role = await AdminApi.get(BASE + '/' + roleId);
    renderInfo();
    var permissions = await AdminApi.get(BASE + '/' + roleId + '/permissions');
    var editable = role.editable && perm.update;
    byId('perm-tbody').innerHTML = permRows(permissions, editable);
    AdminRolePerm.bind(byId('perm-table'));
    if (role.editable) {
      byId('perm-footer').hidden = false;
      var save = byId('btn-perm-save');
      save.disabled = !perm.update;
      save.title = perm.update ? '' : NO_PERM;
      save.addEventListener('click', savePermissions);
    }
    var admins = await AdminApi.get(BASE + '/' + roleId + '/admins');
    byId('tab-admins').textContent = '관리자 (' + role.adminCnt + ')';
    byId('admin-tbody').innerHTML = admins.length ? admins.map(function (a) {
      return '<tr><td>' + esc(a.loginId) + '</td><td>' + esc(a.adminNm) + '</td><td>' + esc(a.deptNm || '-') + '</td>' +
        '<td>' + esc(a.statusNm) + '</td><td>' + AdminJsp.formatDateTime(a.grantedDt) + '</td></tr>';
    }).join('') : '<tr><td colspan="5" class="text-secondary text-center">이 역할을 가진 관리자가 없습니다</td></tr>';

    byId('copy-guide').textContent = role.roleNm + '의 설명·권한을 그대로 가진 새 역할을 만듭니다.';
    byId('copy-form').addEventListener('submit', copy);
  }

  function renderInfo() {
    var sys = role.systemYn === 'Y';
    var actions = '<a class="btn btn-sm" id="btn-list" href="' + PAGE + '">목록</a>';
    if (role.editable) {
      actions += perm.update
        ? ' <a class="btn btn-sm" id="btn-edit" href="' + PAGE + '/' + role.roleId + '/edit">수정</a>'
        : ' <button type="button" class="btn btn-sm" id="btn-edit"' + permAttrs(false) + '>수정</button>';
    }
    actions += ' <button type="button" class="btn btn-sm" id="btn-copy" data-bs-toggle="modal" data-bs-target="#copy-modal"' +
      permAttrs(perm.create) + '>복사</button>';
    if (!sys && role.adminCnt === 0) {
      actions += ' <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-delete"' + permAttrs(perm.delete) + '>삭제</button>';
    }
    var html = '<div class="card-header"><h3 class="card-title" id="role-title">' + (sys ? '🔒 ' : '') + esc(role.roleNm) +
      (role.mine ? '<span class="badge bg-blue-lt ms-2">내 역할</span>' : '') + '</h3>' +
      '<div class="card-actions btn-list">' + actions + '</div></div><div class="card-body">';
    if (!role.editable) {
      html += '<div class="alert alert-info" role="note">' + (sys ? '시스템 역할' : '내가 가진 역할') + '은 수정하거나 권한을 바꿀 수 없습니다.</div>';
    }
    html += '<dl class="row mb-0">' +
      '<dt class="col-sm-2">역할 코드</dt><dd class="col-sm-4" id="role-roleCd">' + esc(role.roleCd) + '</dd>' +
      '<dt class="col-sm-2">사용 여부</dt><dd class="col-sm-4" id="role-useYn">' + (role.useYn === 'Y' ? '사용' : '사용 안 함') + '</dd>' +
      '<dt class="col-sm-2">설명</dt><dd class="col-sm-10" id="role-description">' + esc(role.description || '-') + '</dd>' +
      '<dt class="col-sm-2">관리자 수</dt><dd class="col-sm-4" id="role-adminCnt">' + role.adminCnt + '</dd>' +
      '<dt class="col-sm-2">수정</dt><dd class="col-sm-4">' + esc(role.modNm || '-') + ' · ' + AdminJsp.formatDateTime(role.modDt) + '</dd>' +
      '</dl></div>';
    byId('role-info').innerHTML = html;
    var del = byId('btn-delete');
    if (del) { del.addEventListener('click', remove); }
  }

  /** 권한 표 행 (ssr: rolePermRows.tag와 같은 마크업) */
  function permRows(nodes, editable) {
    return nodes.map(function (n) {
      var folder = n.menuTypeCd === 'FOLDER';
      var html = '<tr class="' + (folder ? 'perm-folder' : 'perm-row') + (n.useYn === 'N' ? ' text-secondary' : '') +
        '" data-menu-id="' + n.menuId + '"><td class="perm-depth-' + n.depth + (folder ? ' fw-bold' : '') + '">' + esc(n.menuNm) + '</td>';
      ACTIONS.forEach(function (a) {
        html += '<td class="text-center">';
        if (n.actions.indexOf(a) >= 0) {
          html += '<input type="checkbox" class="form-check-input m-0 perm-check" id="perm-' + n.menuId + '-' + a + '" data-action="' + a +
            '" aria-label="' + esc(n.menuNm) + ' ' + ACTION_NM[a] + '"' + (n.granted.indexOf(a) >= 0 ? ' checked' : '') +
            (editable && n.grantableActions.indexOf(a) >= 0 ? '' : ' disabled') + '>';
        }
        html += '</td>';
      });
      return html + '</tr>' + permRows(n.children, editable);
    }).join('');
  }

  async function savePermissions() {
    if (!confirm('이 역할을 가진 관리자 ' + role.adminCnt + '명에게 바로 반영됩니다. 저장하시겠습니까?')) { return; }
    try {
      await AdminApi.put(BASE + '/' + role.roleId + '/permissions',
          { permissions: AdminRolePerm.collect(byId('perm-table')), modDt: role.modDt });
      AdminJsp.flash('권한이 저장되었습니다');
      location.reload();
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  }

  async function copy(e) {
    e.preventDefault();
    var form = e.target;
    byId('copy-message').innerHTML = '';
    try {
      var created = await AdminApi.post(BASE + '/' + role.roleId + '/copy',
          { roleCd: form.roleCd.value.trim(), roleNm: form.roleNm.value.trim() });
      AdminJsp.flash('복사되었습니다');
      location.href = PAGE + '/' + created.roleId;
    } catch (err) {
      byId('copy-message').innerHTML = alertHtml(message(err));
    }
  }

  async function remove() {
    if (!confirm('역할을 삭제하시겠습니까?')) { return; }
    try {
      await AdminApi.del(BASE + '/' + role.roleId);
      AdminJsp.flash('삭제되었습니다');
      location.href = PAGE;
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  }

  // ===================== 등록·수정 (SCR-ROL-03) =====================

  async function initForm(roleId) {
    var form = byId('role-form');
    var original = null;
    if (roleId) {
      original = await AdminApi.get(BASE + '/' + roleId);
      form.roleCd.value = original.roleCd;
      form.roleNm.value = original.roleNm;
      form.description.value = original.description || '';
      byId('role-useYn-' + original.useYn).checked = true;
      byId('btn-cancel').href = PAGE + '/' + roleId;
    }
    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      byId('role-message').innerHTML = '';
      var useYn = form.querySelector('input[name="useYn"]:checked').value;
      var body = { roleNm: form.roleNm.value.trim(), description: form.description.value.trim() || null, useYn: useYn };
      try {
        if (!original) {
          body.roleCd = form.roleCd.value.trim();
          var created = await AdminApi.post(BASE, body);
          AdminJsp.flash('등록되었습니다');
          location.href = PAGE + '/' + created.roleId;
          return;
        }
        // 사용 안 함으로 바꾸면 그 역할을 가진 관리자의 권한이 빠진다 (BR-07)
        if (original.useYn === 'Y' && useYn === 'N' && original.adminCnt > 0
            && !confirm('이 역할의 권한이 관리자 ' + original.adminCnt + '명에게서 빠집니다. 저장하시겠습니까?')) {
          return;
        }
        body.modDt = original.modDt;
        await AdminApi.put(BASE + '/' + roleId, body);
        AdminJsp.flash('수정되었습니다');
        location.href = PAGE + '/' + roleId;
      } catch (err) {
        byId('role-message').innerHTML = alertHtml(message(err));
      }
    });
  }

  // ===================== 시작 =====================

  (async function () {
    var me = await AdminJsp.requireLogin();
    var granted = me.permissions.ROLE || [];
    perm = {
      create: me.superAdmin || granted.indexOf('CREATE') >= 0,
      update: me.superAdmin || granted.indexOf('UPDATE') >= 0,
      delete: me.superAdmin || granted.indexOf('DELETE') >= 0
    };
    AdminJsp.showFlash();
    var page = byId('role-page');
    var roleId = page.dataset.roleId ? Number(page.dataset.roleId) : null;
    try {
      if (page.dataset.page === 'list') { await initList(); }
      if (page.dataset.page === 'detail') { await initDetail(roleId); }
      if (page.dataset.page === 'form') { await initForm(roleId); }
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  })();
})();
