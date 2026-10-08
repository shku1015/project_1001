/*
 * ② JSP + API 권한관리 (SCR-PRM-01). 메뉴를 고르면 역할 × 액션 표를 보여 주고, 바뀐 역할만 저장한다.
 * 마크업은 ③ ssr/permission.jsp + permMenuTree.tag와 같다. 체크 규칙(BR-02)은 admin-role-perm.js를 함께 쓴다.
 */
(function () {
  'use strict';

  var BASE = '/permissions';
  var NO_PERM = '권한이 없습니다';
  var ACTION_NM = { READ: '조회', CREATE: '등록', UPDATE: '수정', DELETE: '삭제', EXCEL: '엑셀', PRIVACY: '개인정보' };
  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };
  var canUpdate = false;
  var selectedId = null;

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  function dirty() {
    var table = byId('grant-table');
    return !!table && AdminRolePerm.changes(table).length > 0;
  }

  // ===================== 메뉴 트리 =====================

  function treeHtml(nodes) {
    return '<ul class="menu-tree">' + nodes.map(function (m) {
      var cls = 'menu-node-row' + (m.menuId === selectedId ? ' is-selected' : '') + (m.useYn === 'N' ? ' is-unused' : '');
      var label = m.menuTypeCd === 'PAGE'
        ? '<a class="menu-node-link" href="#" data-select="' + m.menuId + '">' + esc(m.menuNm) + '</a>'
        : '<span class="menu-node-link fw-bold">' + esc(m.menuNm) + '</span>';
      return '<li class="menu-node" data-menu-id="' + m.menuId + '"><div class="' + cls + '">' + label + '</div>' +
        (m.children.length ? treeHtml(m.children) : '') + '</li>';
    }).join('') + '</ul>';
  }

  function markSelected() {
    document.querySelectorAll('#perm-menu-tree .menu-node-row').forEach(function (row) {
      row.classList.toggle('is-selected', row.parentNode.dataset.menuId === String(selectedId));
    });
  }

  // ===================== 역할 × 액션 표 =====================

  async function select(menuId) {
    var g = await AdminApi.get(BASE + '/menus/' + menuId);
    selectedId = menuId;
    markSelected();
    var actions = g.menu.actions;
    var rows = g.roles.map(function (r) {
      var system = r.systemYn === 'Y';
      var cells = actions.map(function (a) {
        if (system) { return '<td class="text-center"><span class="perm-all" title="모든 권한">■</span></td>'; }
        var enabled = canUpdate && r.editable && g.grantableActions.indexOf(a) >= 0;
        return '<td class="text-center"><input type="checkbox" class="form-check-input m-0 perm-check" id="grant-' + r.roleId + '-' + a +
          '" data-action="' + a + '" aria-label="' + esc(r.roleNm) + ' ' + ACTION_NM[a] + '"' +
          (r.granted.indexOf(a) >= 0 ? ' checked' : '') + (enabled ? '' : ' disabled') + '></td>';
      }).join('');
      return '<tr class="grant-row' + (r.useYn === 'N' ? ' text-secondary' : '') + '" data-role-id="' + r.roleId + '"><td>' +
        esc(r.roleNm) + (system ? ' 🔒' : '') +
        (r.mine ? '<span class="badge bg-blue-lt ms-1">내 역할</span>' : '') +
        (r.useYn === 'N' ? '<span class="badge bg-secondary-lt ms-1">사용 안 함</span>' : '') + '</td>' + cells + '</tr>';
    }).join('');

    byId('grant-col').innerHTML = '<div class="card" id="grant-card"><div class="card-header"><h3 class="card-title" id="grant-title">' +
      esc(g.menu.menuNm) + ' <span class="text-secondary ms-1">(' + esc(g.menu.menuCd) + ')</span></h3></div>' +
      '<div class="table-responsive"><table class="table table-vcenter mb-0" id="grant-table"><thead><tr><th>역할</th>' +
      actions.map(function (a) { return '<th class="text-center">' + ACTION_NM[a] + '</th>'; }).join('') +
      '</tr></thead><tbody>' + rows + '</tbody></table></div>' +
      '<div class="card-footer"><div id="grant-message"></div><div class="text-end">' +
      '<button type="button" class="btn btn-primary" id="btn-grant-save"' + (canUpdate ? '' : ' disabled title="' + NO_PERM + '"') +
      '>저장</button></div></div></div>';

    var table = byId('grant-table');
    AdminRolePerm.bind(table);
    AdminRolePerm.snapshot(table);
    byId('btn-grant-save').addEventListener('click', save);
  }

  async function save() {
    var changes = AdminRolePerm.changes(byId('grant-table'));
    if (!changes.length) {
      byId('grant-message').innerHTML = '<div class="alert alert-info" role="note">바뀐 내용이 없습니다.</div>';
      return;
    }
    if (!confirm('역할 ' + changes.length + '개의 권한이 바뀝니다. 저장하시겠습니까?')) { return; }
    try {
      await AdminApi.put(BASE + '/menus/' + selectedId, { roles: changes });
      AdminJsp.showNotice('권한이 저장되었습니다');
      await select(selectedId);
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  }

  // ===================== 시작 =====================

  (async function () {
    var me = await AdminJsp.requireLogin();
    canUpdate = me.superAdmin || (me.permissions.PERMISSION || []).indexOf('UPDATE') >= 0;
    byId('perm-menu-tree').addEventListener('click', function (e) {
      var link = e.target.closest('[data-select]');
      if (!link) { return; }
      e.preventDefault();
      // 저장하지 않은 변경이 있으면 다른 메뉴로 가기 전에 확인한다
      if (dirty() && !confirm('저장하지 않은 변경이 있습니다. 이동하시겠습니까?')) { return; }
      select(Number(link.dataset.select)).catch(function (err) { AdminJsp.showNotice(message(err), 'danger'); });
    });
    try {
      byId('perm-menu-tree').innerHTML = treeHtml(await AdminApi.get(BASE));
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  })();
})();
