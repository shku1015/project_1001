/*
 * ② JSP + API 메뉴관리 (SCR-MNU-01). 트리·상세·저장 모두 /api/v1/menus 로 처리한다.
 * 마크업은 ③ ssr/menu.jsp + menuAdminTree.tag와 같다. 트리 순서 조작은 admin-menu-tree.js를 함께 쓴다.
 */
(function () {
  'use strict';

  var ACTIONS = ['READ', 'CREATE', 'UPDATE', 'DELETE', 'EXCEL', 'PRIVACY'];
  var ACTION_NM = { READ: '조회', CREATE: '등록', UPDATE: '수정', DELETE: '삭제', EXCEL: '엑셀', PRIVACY: '개인정보열람' };
  var NO_PERM = '권한이 없습니다';
  var el = AdminJsp.el;

  var perm = {};
  var tree = [];
  var state = { mode: null, selected: null, parent: null };    // mode: 'create' | 'edit'

  function esc(value) {
    var div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
  }

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  function showError(text) {
    var area = document.getElementById('page-message');
    area.innerHTML = '';
    area.appendChild(el('div', { 'class': 'alert alert-danger', role: 'alert' }, text));
  }

  function find(nodes, id) {
    for (var i = 0; i < nodes.length; i++) {
      if (nodes[i].menuId === id) { return nodes[i]; }
      var f = find(nodes[i].children, id);
      if (f) { return f; }
    }
    return null;
  }

  // ===================== 트리 =====================

  function treeHtml(nodes, parentId) {
    var html = '<ul class="menu-tree" data-parent-id="' + (parentId == null ? '' : parentId) + '">';
    nodes.forEach(function (m) {
      var cls = 'menu-node-row' + (state.selected && state.selected.menuId === m.menuId ? ' is-selected' : '')
          + (m.useYn === 'N' ? ' is-unused' : '');
      html += '<li class="menu-node" data-menu-id="' + m.menuId + '"><div class="' + cls + '">'
          + '<span class="drag-handle" title="끌어서 순서 변경" aria-hidden="true">⠿</span>'
          + '<a class="menu-node-link" href="#" data-select="' + m.menuId + '">' + esc(m.menuNm) + '</a>'
          + (m.systemYn === 'Y' ? '<span class="badge bg-secondary-lt" title="시스템 메뉴">🔒</span>' : '')
          + (m.boardAutoYn === 'Y' ? '<span class="badge bg-azure-lt">자동</span>' : '')
          + '<span class="badge bg-yellow-lt changed-badge" hidden>변경됨</span></div>'
          + (m.children.length ? treeHtml(m.children, m.menuId) : '') + '</li>';
    });
    return html + '</ul>';
  }

  async function loadTree() {
    tree = await AdminApi.get('/menus/tree');
    document.getElementById('menu-tree-root').innerHTML = treeHtml(tree, null);
    AdminMenuTree.init({ root: document.getElementById('menu-tree-card'), editable: perm.update, onChange: onDirty });
  }

  /** 저장하지 않은 순서 변경이 있으면 트리 구조를 바꾸는 작업(추가·이동·삭제)을 막는다 */
  function onDirty(dirty) {
    document.querySelectorAll('[data-structure-action]').forEach(function (b) {
      if (b.dataset.noPermission !== 'true') { b.disabled = dirty; }
    });
  }

  function setPermissionButton(button, allowed) {
    if (!button) { return; }
    button.disabled = !allowed;
    button.dataset.noPermission = String(!allowed);
    button.title = allowed ? '' : NO_PERM;
  }

  // ===================== 상세·입력 =====================

  function renderDetail() {
    var col = document.getElementById('menu-detail-col');
    var create = state.mode === 'create';
    var sel = state.selected;
    if (!state.mode) {
      col.innerHTML = '<div class="card"><div class="card-body text-secondary">왼쪽에서 메뉴를 선택하세요.</div></div>';
      return;
    }
    var parent = create ? state.parent : null;
    var depth = create ? (parent ? parent.depth + 1 : 1) : sel.depth;
    var type = create ? (depth === 3 ? 'PAGE' : 'FOLDER') : sel.menuTypeCd;
    var prot = !create && (sel.systemYn === 'Y' || sel.boardAutoYn === 'Y');
    var actions = create ? ['READ'] : sel.actions;
    var useYn = create ? 'Y' : sel.useYn;
    var parentNm = create ? (parent ? parent.menuNm : '-') : (sel.parentMenuNm || '-');

    var html = '<div class="card" id="menu-detail-card"><div class="card-header"><h3 class="card-title">'
        + (create ? '메뉴 등록' : '메뉴 상세') + '</h3></div><form id="menu-form" novalidate><div class="card-body">';
    if (prot) {
      html += '<div class="alert alert-info" role="note">' + (sel.systemYn === 'Y' ? '시스템 메뉴' : '게시판별 자동 메뉴')
          + '는 메뉴명·아이콘만 바꿀 수 있습니다.</div>';
    }
    html += '<div class="mb-3"><label class="form-label" for="menu-parent">상위 메뉴</label>'
        + '<input class="form-control" id="menu-parent" readonly value="' + esc(parentNm) + '"></div>';
    html += '<div class="mb-3"><label class="form-label' + (create ? ' required' : '') + '" for="menu-menuCd">메뉴 코드</label>'
        + (create ? '<input class="form-control" id="menu-menuCd" name="menuCd" maxlength="50" required placeholder="영문 대문자·숫자·_">'
                  : '<input class="form-control" id="menu-menuCd" readonly value="' + esc(sel.menuCd) + '">') + '</div>';
    html += '<div class="mb-3"><label class="form-label required" for="menu-menuNm">메뉴명</label>'
        + '<input class="form-control" id="menu-menuNm" name="menuNm" maxlength="100" required value="' + esc(create ? '' : sel.menuNm) + '"></div>';
    html += '<div class="mb-3"><label class="form-label">종류</label>' + (create
        ? '<div><label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="menuTypeCd" value="FOLDER" id="menu-type-FOLDER"'
          + (type === 'FOLDER' ? ' checked' : '') + (depth === 3 ? ' disabled' : '') + '> 폴더</label>'
          + '<label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="menuTypeCd" value="PAGE" id="menu-type-PAGE"'
          + (type === 'PAGE' ? ' checked' : '') + '> 화면</label></div>'
        : '<div class="form-control-plaintext" id="menu-type">' + (type === 'FOLDER' ? '폴더' : '화면') + '</div>') + '</div>';
    html += '<div data-page-only' + (type === 'PAGE' ? '' : ' hidden') + '>'
        + '<div class="mb-3"><label class="form-label required" for="menu-menuUrl">URL</label>'
        + '<input class="form-control" id="menu-menuUrl" name="menuUrl" maxlength="200" placeholder="/로 시작"' + (prot ? ' readonly' : '')
        + ' value="' + esc(create ? '' : (sel.menuUrl || '')) + '"></div>'
        + '<div class="mb-3"><label class="form-label">사용 액션</label><div>';
    ACTIONS.forEach(function (a) {
      html += '<label class="form-check form-check-inline"><input class="form-check-input" type="checkbox" name="actions" value="' + a
          + '" id="action-' + a + '"' + (a === 'READ' || actions.indexOf(a) >= 0 ? ' checked' : '')
          + (a === 'READ' || prot ? ' disabled' : '') + '> ' + ACTION_NM[a] + '</label>';
    });
    html += '</div></div></div>';
    if (depth === 1) {
      html += '<div class="mb-3"><label class="form-label" for="menu-icon">아이콘</label>'
          + '<input class="form-control" id="menu-icon" name="icon" maxlength="50" value="' + esc(create ? '' : (sel.icon || '')) + '"></div>';
    }
    html += '<div class="mb-3"><label class="form-label">사용 여부</label><div>'
        + '<label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="Y"' + (useYn === 'Y' ? ' checked' : '') + (prot ? ' disabled' : '') + '> 사용</label>'
        + '<label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="N"' + (useYn === 'N' ? ' checked' : '') + (prot ? ' disabled' : '') + '> 사용 안 함</label>'
        + '</div></div><div id="menu-message"></div></div>';

    html += '<div class="card-footer d-flex flex-wrap gap-2"><button type="submit" class="btn btn-primary" id="btn-save">저장</button>';
    if (!create) {
      if (sel.menuTypeCd === 'FOLDER' && sel.depth < 3) {
        html += '<button type="button" class="btn" id="btn-child-create" data-structure-action>하위 메뉴 추가</button>';
      }
      if (!prot) {
        html += '<button type="button" class="btn" id="btn-move" data-structure-action>상위 메뉴 변경</button>';
      }
    }
    html += '</div></form>';
    var node = sel ? find(tree, sel.menuId) : null;
    if (!create && !prot && node && node.children.length === 0) {
      html += '<div class="card-footer border-top-0 pt-0"><button type="button" class="btn btn-outline-danger" id="btn-delete" data-structure-action>삭제</button></div>';
    }
    col.innerHTML = html + '</div>';

    setPermissionButton(document.getElementById('btn-save'), create ? perm.create : perm.update);
    setPermissionButton(document.getElementById('btn-child-create'), perm.create);
    setPermissionButton(document.getElementById('btn-move'), perm.update);
    setPermissionButton(document.getElementById('btn-delete'), perm.delete);
    bindDetail();
    onDirty(AdminMenuTree.isDirty());
  }

  function bindDetail() {
    var form = document.getElementById('menu-form');
    form.querySelectorAll('input[name="menuTypeCd"]').forEach(function (r) {
      r.addEventListener('change', function () {
        form.querySelector('[data-page-only]').hidden = r.value !== 'PAGE';
      });
    });
    form.addEventListener('submit', function (e) { e.preventDefault(); save(form); });
    var child = document.getElementById('btn-child-create');
    if (child) { child.addEventListener('click', function () { startCreate(state.selected); }); }
    var move = document.getElementById('btn-move');
    if (move) { move.addEventListener('click', openMove); }
    var del = document.getElementById('btn-delete');
    if (del) { del.addEventListener('click', remove); }
  }

  function formBody(form) {
    var type = state.mode === 'create'
        ? (form.querySelector('input[name="menuTypeCd"]:checked') || {}).value
        : state.selected.menuTypeCd;
    var actions = Array.prototype.slice.call(form.querySelectorAll('input[name="actions"]:checked'))
        .map(function (c) { return c.value; });
    var useYn = (form.querySelector('input[name="useYn"]:checked') || {}).value || 'Y';
    var body = {
      menuNm: form.menuNm.value.trim(),
      menuUrl: type === 'PAGE' ? form.menuUrl.value.trim() || null : null,
      actions: type === 'PAGE' ? actions : [],
      icon: form.icon ? form.icon.value.trim() || null : null,
      useYn: useYn
    };
    if (state.mode === 'create') {
      body.parentMenuId = state.parent ? state.parent.menuId : null;
      body.menuCd = form.menuCd.value.trim();
      body.menuTypeCd = type;
    } else {
      body.modDt = state.selected.modDt;
    }
    return body;
  }

  async function save(form) {
    var area = document.getElementById('menu-message');
    area.innerHTML = '';
    if (AdminMenuTree.isDirty() && !confirm('저장하지 않은 순서 변경은 사라집니다. 계속하시겠습니까?')) { return; }
    var body = formBody(form);
    try {
      if (state.mode === 'create') {
        var created = await AdminApi.post('/menus', body);
        AdminJsp.showNotice('등록되었습니다');
        await loadTree();
        await select(created.menuId);
        return;
      }
      var id = state.selected.menuId;
      // 액션을 빼서 회수되는 역할이 있으면 먼저 확인한다 (BR-07)
      var preview = await AdminApi.put('/menus/' + id + '?dryRun=Y', body);
      if (preview.revokedRoles.length) {
        var names = preview.revokedRoles.map(function (r) { return r.roleNm + ' (' + r.actions.join(', ') + ')'; }).join(', ');
        if (!confirm(preview.revokedRoles.length + '개 역할에서 권한이 회수됩니다: ' + names + '\n저장하시겠습니까?')) { return; }
      }
      await AdminApi.put('/menus/' + id, body);
      AdminJsp.showNotice('수정되었습니다');
      await loadTree();
      await select(id);
    } catch (e) {
      area.appendChild(el('div', { 'class': 'alert alert-danger', role: 'alert' }, message(e)));
    }
  }

  async function remove() {
    if (!confirm('메뉴를 삭제하시겠습니까? 이 메뉴의 권한과 역할 매핑도 함께 지워집니다.')) { return; }
    try {
      await AdminApi.del('/menus/' + state.selected.menuId);
      state = { mode: null, selected: null, parent: null };
      AdminJsp.showNotice('삭제되었습니다');
      await loadTree();
      renderDetail();
      updateArrows();
    } catch (e) {
      showError(message(e));
    }
  }

  // ===================== 상위 메뉴 변경 =====================

  function openMove() {
    var select = document.getElementById('move-parent');
    select.innerHTML = '<option value="">(최상위)</option>';
    var exclude = state.selected.menuId;
    (function walk(nodes) {
      nodes.forEach(function (n) {
        if (n.menuId === exclude) { return; }
        if (n.menuTypeCd === 'FOLDER' && n.depth < 3) {
          var o = el('option', { value: String(n.menuId) }, (n.depth === 2 ? '└ ' : '') + n.menuNm);
          if (state.selected.parentMenuId === n.menuId) { o.selected = true; }
          select.appendChild(o);
        }
        walk(n.children);
      });
    })(tree);
    bootstrap.Modal.getOrCreateInstance(document.getElementById('move-modal')).show();
  }

  async function submitMove(e) {
    e.preventDefault();
    var value = document.getElementById('move-parent').value;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('move-modal')).hide();
    try {
      await AdminApi.patch('/menus/' + state.selected.menuId + '/parent',
          { parentMenuId: value ? Number(value) : null, modDt: state.selected.modDt });
      AdminJsp.showNotice('이동했습니다');
      var id = state.selected.menuId;
      await loadTree();
      await select(id);
    } catch (err) {
      showError(message(err));
    }
  }

  // ===================== 선택·순서 =====================

  async function select(menuId) {
    state = { mode: 'edit', selected: await AdminApi.get('/menus/' + menuId), parent: null };
    document.querySelectorAll('.menu-node-row').forEach(function (r) {
      r.classList.toggle('is-selected', r.parentNode.dataset.menuId === String(menuId));
    });
    renderDetail();
    updateArrows();
  }

  function startCreate(parent) {
    state = { mode: 'create', selected: null, parent: parent };
    renderDetail();
    updateArrows();
  }

  function updateArrows() {
    ['btn-up', 'btn-down'].forEach(function (id) {
      var b = document.getElementById(id);
      b.disabled = !perm.update || !state.selected;
      b.title = perm.update ? '' : NO_PERM;
    });
  }

  async function saveOrder() {
    if (!confirm('메뉴 순서를 저장하시겠습니까?')) { return; }
    try {
      await AdminApi.put('/menus/order', { orders: AdminMenuTree.collectOrders() });
      AdminJsp.showNotice('순서가 저장되었습니다');
      await loadTree();
    } catch (e) {
      showError(message(e));
    }
  }

  // ===================== 시작 =====================

  (async function () {
    var me = await AdminJsp.requireLogin();
    var granted = (me.permissions.MENU || []);
    perm = {
      create: me.superAdmin || granted.indexOf('CREATE') >= 0,
      update: me.superAdmin || granted.indexOf('UPDATE') >= 0,
      delete: me.superAdmin || granted.indexOf('DELETE') >= 0
    };
    setPermissionButton(document.getElementById('btn-root-create'), perm.create);
    document.getElementById('btn-root-create').addEventListener('click', function () { startCreate(null); });
    document.getElementById('btn-up').addEventListener('click', function () { AdminMenuTree.move(state.selected.menuId, -1); });
    document.getElementById('btn-down').addEventListener('click', function () { AdminMenuTree.move(state.selected.menuId, 1); });
    document.getElementById('btn-order-save').addEventListener('click', saveOrder);
    document.getElementById('btn-order-reset').addEventListener('click', AdminMenuTree.reset);
    document.getElementById('move-form').addEventListener('submit', submitMove);
    document.getElementById('menu-tree-root').addEventListener('click', function (e) {
      var link = e.target.closest('[data-select]');
      if (!link) { return; }
      e.preventDefault();
      void select(Number(link.dataset.select));
    });
    try {
      await loadTree();
    } catch (e) {
      showError(message(e));
    }
  })();
})();
