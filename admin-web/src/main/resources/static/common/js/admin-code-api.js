/*
 * ② JSP + API 코드관리 (SCR-COD-01). 그룹·상세코드를 /api/v1/code-groups 로 다룬다.
 * 화면 구조·입력 칸 id는 ③ SSR·① React와 같다.
 * 모달은 data-bs-toggle 로 연다 (Tabler가 window.bootstrap을 노출하지 않으므로 프로그램 호출 대신 data-api 사용).
 */
(function () {
  'use strict';
  var BASE = '/code-groups';
  var NO_PERM = '권한이 없습니다';
  var perms = [];
  var superAdmin = false;
  var selected = null;   // 선택한 그룹 { groupCd, systemYn, ... }
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : v; return d.innerHTML; };
  var byId = function (id) { return document.getElementById(id); };

  (async function () {
    var me = await AdminJsp.requireLogin();
    perms = (me.permissions && me.permissions.CODE) || [];
    superAdmin = !!me.superAdmin;
    // 권한이 없는 버튼은 비활성으로 보여 준다 (docs/05-ia-screens.md 4.2)
    var create = byId('btn-group-create');
    create.disabled = !can('CREATE');
    create.title = can('CREATE') ? '' : NO_PERM;

    byId('group-search').addEventListener('submit', function (e) { e.preventDefault(); loadGroups(); });
    byId('group-form').addEventListener('submit', saveGroup);
    byId('detail-form').addEventListener('submit', saveDetail);
    byId('group-modal').addEventListener('show.bs.modal', fillGroupModal);
    byId('detail-modal').addEventListener('show.bs.modal', fillDetailModal);
    await loadGroups();
  })();

  function can(action) { return superAdmin || perms.indexOf(action) >= 0; }
  function closeModal(id) {
    var el = byId(id).querySelector('[data-bs-dismiss="modal"]');
    if (el) { el.click(); }
  }

  async function loadGroups() {
    var params = new URLSearchParams();
    if (byId('search-keyword').value.trim()) { params.set('keyword', byId('search-keyword').value.trim()); }
    if (byId('search-useYn').value) { params.set('useYn', byId('search-useYn').value); }
    var groups = await AdminApi.get(BASE + (params.toString() ? '?' + params : ''));
    var tbody = byId('group-tbody');
    tbody.innerHTML = '';
    if (!groups.length) {
      tbody.innerHTML = '<tr><td colspan="4" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr>';
      return;
    }
    groups.forEach(function (g) {
      var tr = document.createElement('tr');
      if (selected && selected.groupCd === g.groupCd) { tr.className = 'table-active'; }
      tr.innerHTML = '<td><a href="#" data-group="' + esc(g.groupCd) + '">' + (g.systemYn === 'Y' ? '🔒 ' : '') +
        esc(g.groupCd) + '</a></td><td>' + esc(g.groupNm) + '</td><td class="text-center">' + g.codeCnt +
        '</td><td class="text-center">' + (g.useYn === 'Y' ? '사용' : '사용 안 함') + '</td>';
      tr.querySelector('a').addEventListener('click', function (e) { e.preventDefault(); selectGroup(g.groupCd); });
      tbody.appendChild(tr);
    });
  }

  async function selectGroup(groupCd) {
    selected = await AdminApi.get(BASE + '/' + groupCd);
    var details = await AdminApi.get(BASE + '/' + groupCd + '/codes');
    renderDetailPanel(selected, details);
    Array.prototype.forEach.call(byId('group-tbody').querySelectorAll('tr'), function (tr) {
      var a = tr.querySelector('a[data-group]');
      tr.classList.toggle('table-active', a && a.dataset.group === groupCd);
    });
  }

  function renderDetailPanel(group, details) {
    var sys = group.systemYn === 'Y';
    var actions = '';
    actions += btn('그룹 수정', { 'bs-toggle': 'modal', 'bs-target': '#group-modal', mode: 'edit',
      'group-cd': group.groupCd, 'group-nm': group.groupNm, description: group.description || '',
      'use-yn': group.useYn, 'system-yn': group.systemYn, 'mod-dt': group.modDt }, '', 'UPDATE', 'btn-group-edit');
    if (!sys && !details.length) { actions += ' ' + btn('그룹 삭제', {}, 'btn-ghost-danger', 'DELETE', 'btn-group-delete'); }
    if (!sys) {
      actions += ' ' + btn('코드 등록', { 'bs-toggle': 'modal', 'bs-target': '#detail-modal', mode: 'create',
        'next-sort': nextSort(details) }, 'btn-primary', 'CREATE', 'btn-detail-create');
    }

    var rows = details.map(function (d) {
      var btns = '';
      btns += btn('수정', { 'bs-toggle': 'modal', 'bs-target': '#detail-modal', mode: 'edit', code: d.code,
        'code-nm': d.codeNm, 'sort-ord': d.sortOrd, description: d.description || '', 'use-yn': d.useYn,
        'system-yn': group.systemYn, 'mod-dt': d.modDt }, '', 'UPDATE');
      if (!sys) { btns += ' ' + btn('삭제', { del: d.code }, 'btn-ghost-danger', 'DELETE'); }
      return '<tr><td>' + esc(d.code) + '</td><td>' + esc(d.codeNm) + '</td><td class="text-center">' + d.sortOrd +
        '</td><td>' + esc(d.description) + '</td><td class="text-center">' + (d.useYn === 'Y' ? '사용' : '사용 안 함') +
        '</td><td class="text-end btn-list">' + btns + '</td></tr>';
    }).join('');
    if (!details.length) { rows = '<tr><td colspan="6" class="text-secondary text-center">상세코드가 없습니다</td></tr>'; }

    byId('detail-panel').innerHTML =
      '<div class="card"><div class="card-header"><h3 class="card-title">' + (sys ? '🔒 ' : '') + esc(group.groupNm) +
      ' <span class="text-secondary ms-1">(' + esc(group.groupCd) + ')</span></h3>' +
      '<div class="card-actions btn-list">' + actions + '</div></div>' +
      '<div class="table-responsive"><table class="table table-vcenter"><thead><tr><th>코드</th><th>코드명</th>' +
      '<th class="text-center">정렬</th><th>설명</th><th class="text-center">사용</th><th></th></tr></thead><tbody>' +
      rows + '</tbody></table></div></div>';

    if (byId('btn-group-delete')) { byId('btn-group-delete').addEventListener('click', deleteGroup); }
    details.forEach(function (d) {
      var x = byId('detail-panel').querySelector('[data-del="' + d.code + '"]');
      if (x) { x.addEventListener('click', function () { deleteDetail(d.code); }); }
    });
  }

  // ----- 모달 채우기 (data-bs-toggle 로 열릴 때) -----
  function fillGroupModal(event) {
    var b = event.relatedTarget;
    var edit = b.dataset.mode === 'edit';
    byId('group-modal-title').textContent = edit ? '그룹 수정' : '그룹 등록';
    byId('group-message').innerHTML = '';
    byId('group-form').dataset.mode = edit ? 'edit' : 'create';
    byId('group-form').dataset.groupCd = edit ? b.dataset.groupCd : '';
    byId('group-form').dataset.modDt = edit ? b.dataset.modDt : '';
    byId('group-groupNm').value = edit ? b.dataset.groupNm : '';
    byId('group-description').value = edit ? (b.dataset.description || '') : '';
    setRadio('group-useYn', edit ? b.dataset.useYn : 'Y');
    byId('group-groupCd-field').hidden = edit;
    byId('group-groupCd-display').hidden = !edit;
    if (edit) { byId('group-groupCd-display').textContent = '그룹코드: ' + b.dataset.groupCd; }
    else { byId('group-groupCd').value = ''; }
    byId('group-useYn-wrap').hidden = edit && b.dataset.systemYn === 'Y';
  }

  function fillDetailModal(event) {
    var b = event.relatedTarget;
    var edit = b.dataset.mode === 'edit';
    byId('detail-modal-title').textContent = edit ? '코드 수정' : '코드 등록';
    byId('detail-message').innerHTML = '';
    byId('detail-form').dataset.mode = edit ? 'edit' : 'create';
    byId('detail-form').dataset.code = edit ? b.dataset.code : '';
    byId('detail-form').dataset.modDt = edit ? b.dataset.modDt : '';
    byId('detail-codeNm').value = edit ? b.dataset.codeNm : '';
    byId('detail-description').value = edit ? (b.dataset.description || '') : '';
    byId('detail-sortOrd').value = edit ? b.dataset.sortOrd : b.dataset.nextSort;
    setRadio('detail-useYn', edit ? b.dataset.useYn : 'Y');
    byId('detail-code').value = edit ? b.dataset.code : '';
    byId('detail-code').readOnly = edit;
    byId('detail-useYn-wrap').hidden = edit && b.dataset.systemYn === 'Y';
  }

  // ----- 저장·삭제 -----
  async function saveGroup(e) {
    e.preventDefault();
    var edit = byId('group-form').dataset.mode === 'edit';
    var body = { groupNm: byId('group-groupNm').value.trim(),
      description: byId('group-description').value.trim() || null, useYn: radio('group-useYn') };
    try {
      if (edit) {
        body.modDt = byId('group-form').dataset.modDt;
        await AdminApi.put(BASE + '/' + byId('group-form').dataset.groupCd, body);
      } else {
        body.groupCd = byId('group-groupCd').value.trim();
        await AdminApi.post(BASE, body);
      }
      closeModal('group-modal');
      AdminJsp.showNotice(edit ? '수정되었습니다' : '등록되었습니다');
      await loadGroups();
      if (edit) { await selectGroup(byId('group-form').dataset.groupCd); }
    } catch (err) { showError('group-message', err); }
  }

  async function deleteGroup() {
    if (!confirm('그룹코드를 삭제하시겠습니까?')) { return; }
    try {
      await AdminApi.del(BASE + '/' + selected.groupCd);
      selected = null;
      byId('detail-panel').innerHTML = '<div class="card"><div class="card-body text-secondary">왼쪽에서 그룹코드를 선택하세요.</div></div>';
      AdminJsp.showNotice('삭제되었습니다');
      await loadGroups();
    } catch (err) { AdminJsp.showNotice(err.message, 'danger'); }
  }

  async function saveDetail(e) {
    e.preventDefault();
    var edit = byId('detail-form').dataset.mode === 'edit';
    var body = { codeNm: byId('detail-codeNm').value.trim(), sortOrd: Number(byId('detail-sortOrd').value),
      description: byId('detail-description').value.trim() || null, useYn: radio('detail-useYn') };
    try {
      if (edit) {
        body.modDt = byId('detail-form').dataset.modDt;
        await AdminApi.put(BASE + '/' + selected.groupCd + '/codes/' + byId('detail-form').dataset.code, body);
      } else {
        body.code = byId('detail-code').value.trim();
        await AdminApi.post(BASE + '/' + selected.groupCd + '/codes', body);
      }
      closeModal('detail-modal');
      AdminJsp.showNotice(edit ? '수정되었습니다' : '등록되었습니다');
      await selectGroup(selected.groupCd);
      await loadGroups();
    } catch (err) { showError('detail-message', err); }
  }

  async function deleteDetail(code) {
    if (!confirm('삭제하면 복구할 수 없습니다. 사용 안 함으로 바꾸는 것을 권장합니다. 삭제하시겠습니까?')) { return; }
    try {
      await AdminApi.del(BASE + '/' + selected.groupCd + '/codes/' + code);
      AdminJsp.showNotice('삭제되었습니다');
      await selectGroup(selected.groupCd);
      await loadGroups();
    } catch (err) { AdminJsp.showNotice(err.message, 'danger'); }
  }

  // ----- 유틸 -----
  /** action 권한이 없으면 비활성 버튼 (마우스를 올리면 "권한이 없습니다") */
  function btn(label, data, extraClass, action, id) {
    var attrs = 'type="button" class="btn btn-sm ' + (extraClass || '') + '"' + (id ? ' id="' + id + '"' : '');
    if (!can(action)) { attrs += ' disabled title="' + NO_PERM + '"'; }
    Object.keys(data).forEach(function (k) { attrs += ' data-' + k + '="' + esc(data[k]) + '"'; });
    return '<button ' + attrs + '>' + esc(label) + '</button>';
  }
  function radio(name) { return document.querySelector('input[name="' + name + '"]:checked').value; }
  function setRadio(name, value) {
    Array.prototype.forEach.call(document.getElementsByName(name), function (r) { r.checked = r.value === value; });
  }
  function nextSort(details) {
    return details && details.length ? Math.max.apply(null, details.map(function (d) { return d.sortOrd; })) + 1 : 1;
  }
  function showError(areaId, err) {
    var text = err.fieldErrors && err.fieldErrors.length ? err.fieldErrors[0].message : err.message;
    byId(areaId).innerHTML = '<div class="alert alert-danger" role="alert">' + esc(text) + '</div>';
  }
})();
