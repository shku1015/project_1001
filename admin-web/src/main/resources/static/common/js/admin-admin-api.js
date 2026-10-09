/*
 * ② JSP + API 관리자관리 (SCR-ADM-01~03). /api/v1/admins 로 목록·상세·등록·수정·상태 변경을 처리한다.
 * 화면 구조·입력 칸 id는 ③ SSR(ssr/admin-*.jsp)·① React(Admin*Page.tsx)와 같다.
 * 등록·비밀번호 초기화 때 받은 임시 비밀번호는 상세 화면 위쪽에 한 번만 보여 준다 (BR-02, CMP-09).
 */
(function () {
  'use strict';

  var BASE = '/admins';
  var PAGE = '/jsp/admins';
  var NO_PERM = '권한이 없습니다';
  var TEMP_KEY = 'admin.tempPassword';
  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };
  var perm = {};

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  function permAttrs(allowed) {
    return allowed ? '' : ' disabled title="' + NO_PERM + '"';
  }

  // ===================== 목록 (SCR-ADM-01) =====================

  var query = { page: 1, size: 20, sort: '' };

  async function initList() {
    var create = byId('btn-admin-create');
    if (perm.create) {
      create.classList.remove('disabled');
      create.removeAttribute('aria-disabled');
    } else {
      create.title = NO_PERM;
    }
    var roles = await AdminApi.get(BASE + '/role-options');
    byId('search-roleId').insertAdjacentHTML('beforeend', roles.map(function (r) {
      return '<option value="' + r.roleId + '">' + esc(r.roleNm) + '</option>';
    }).join(''));
    byId('admin-search').addEventListener('submit', function (e) { e.preventDefault(); query.page = 1; loadList(); });
    byId('admin-thead').addEventListener('click', function (e) {
      var link = e.target.closest('.sort-link');
      if (!link) { return; }
      e.preventDefault();
      query.sort = AdminPager.nextSort(link.dataset.sort, query.sort);
      query.page = 1;
      loadList();
    });
    await loadList();
  }

  async function loadList() {
    var form = byId('admin-search');
    var params = new URLSearchParams();
    ['loginId', 'adminNm', 'deptNm', 'roleId', 'statusCd'].forEach(function (name) {
      var v = form[name].value.trim();
      if (v) { params.set(name, v); }
    });
    params.set('page', query.page);
    params.set('size', query.size);
    if (query.sort) { params.set('sort', query.sort); }
    var result = await AdminApi.get(BASE + '?' + params);

    byId('admin-thead').innerHTML = '<th>번호</th>' + AdminPager.sortTh('loginId', '로그인 아이디', query.sort) +
      AdminPager.sortTh('adminNm', '이름', query.sort) + '<th>부서</th><th>역할</th><th>상태</th>' +
      AdminPager.sortTh('lastLoginDt', '마지막 로그인', query.sort) + AdminPager.sortTh('regDt', '등록일시', query.sort);
    byId('admin-tbody').innerHTML = result.items.length ? result.items.map(function (a, i) {
      var no = result.totalCount - (result.page - 1) * result.size - i;
      var roles = a.roles.length ? esc(a.roles[0].roleNm) + (a.roles.length > 1 ? ' 외 ' + (a.roles.length - 1) : '') : '-';
      return '<tr><td>' + no + '</td><td><a href="' + PAGE + '/' + a.adminId + '">' + esc(a.loginId) + '</a></td>' +
        '<td>' + esc(a.adminNm) + '</td><td>' + esc(a.deptNm || '-') + '</td><td>' + roles + '</td>' +
        '<td>' + esc(a.statusNm) + '</td><td>' + AdminJsp.formatDateTime(a.lastLoginDt) + '</td>' +
        '<td>' + AdminJsp.formatDateTime(a.regDt) + '</td></tr>';
    }).join('') : '<tr><td colspan="8" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr>';
    AdminPager.render(byId('pager'), result,
      function (page) { query.page = page; loadList(); },
      function (size) { query.size = size; query.page = 1; loadList(); });
  }

  // ===================== 상세 (SCR-ADM-02) =====================

  var admin;

  async function initDetail(adminId) {
    showTempPassword();
    admin = await AdminApi.get(BASE + '/' + adminId);
    var options = await AdminApi.get(BASE + '/role-options');
    var histories = await AdminApi.get(BASE + '/' + adminId + '/login-histories');
    renderInfo();
    renderRoles(options);
    byId('history-tbody').innerHTML = histories.length ? histories.map(function (h) {
      return '<tr><td>' + AdminJsp.formatDateTimeSec(h.regDt) + '</td><td>' + esc(h.resultNm) + '</td><td>' +
        (h.authTypeCd === 'SESSION' ? '세션' : '토큰') + '</td><td>' + esc(h.ipAddr) + '</td></tr>';
    }).join('') : '<tr><td colspan="4" class="text-secondary text-center">로그인 이력이 없습니다</td></tr>';
  }

  /** 등록·비밀번호 초기화 직후 한 번만 보여 준다. 보여 준 뒤 바로 지운다 */
  function showTempPassword() {
    var temp = sessionStorage.getItem(TEMP_KEY);
    if (!temp) { return; }
    sessionStorage.removeItem(TEMP_KEY);
    byId('temp-password-area').innerHTML = '<div class="alert alert-warning" role="alert" id="temp-password-box">' +
      '<div class="d-flex align-items-center gap-2 flex-wrap"><span>임시 비밀번호:</span><code class="fs-3" id="temp-password">' +
      esc(temp) + '</code><button type="button" class="btn btn-sm" id="btn-temp-copy">복사</button>' +
      '<button type="button" class="btn btn-sm ms-auto" id="btn-temp-close">닫기</button></div>' +
      '<div class="mt-1 small">이 창을 닫거나 화면을 옮기면 다시 볼 수 없습니다. 관리자에게 전달하세요.</div></div>';
    byId('btn-temp-copy').addEventListener('click', function () {
      navigator.clipboard.writeText(temp);
      this.textContent = '복사됨';
    });
    byId('btn-temp-close').addEventListener('click', function () { byId('temp-password-box').remove(); });
  }

  function renderInfo() {
    var a = admin;
    var actions = '<a class="btn btn-sm" id="btn-list" href="' + PAGE + '">목록</a>';
    actions += perm.update
      ? ' <a class="btn btn-sm" id="btn-edit" href="' + PAGE + '/' + a.adminId + '/edit">수정</a>'
      : ' <button type="button" class="btn btn-sm" id="btn-edit"' + permAttrs(false) + '>수정</button>';
    if (a.statusCd === 'LOCKED') {
      actions += ' <button type="button" class="btn btn-sm" id="btn-unlock"' + permAttrs(perm.update) + '>잠금 해제</button>';
    }
    if (a.statusCd !== 'DISABLED') {
      actions += ' <button type="button" class="btn btn-sm" id="btn-password-reset"' + permAttrs(perm.update) + '>비밀번호 초기화</button>';
    }
    if (!a.self && a.statusCd !== 'DISABLED') {
      actions += ' <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-disable"' + permAttrs(perm.delete) + '>사용중지</button>';
    }
    if (a.statusCd === 'DISABLED') {
      actions += ' <button type="button" class="btn btn-sm" id="btn-enable"' + permAttrs(perm.delete) + '>재사용</button>';
    }
    var dl = [
      ['로그인 아이디', 'loginId', esc(a.loginId)], ['상태', 'status', esc(a.statusNm)],
      ['이름', 'adminNm', esc(a.adminNm)], ['이메일', 'email', esc(a.email)],
      ['휴대폰 번호', 'mobileNo', AdminJsp.formatMobile(a.mobileNo)], ['부서', 'deptNm', esc(a.deptNm || '-')],
      ['로그인 실패', 'loginFailCnt', a.loginFailCnt + '회'], ['임시 비밀번호', 'pwdTempYn', a.pwdTempYn === 'Y' ? '예' : '아니오'],
      ['비밀번호 변경', null, AdminJsp.formatDateTimeSec(a.pwdChangedDt)], ['마지막 로그인', null, AdminJsp.formatDateTimeSec(a.lastLoginDt)],
      ['등록', null, esc(a.regNm || '-') + ' · ' + AdminJsp.formatDateTimeSec(a.regDt)],
      ['수정', null, esc(a.modNm || '-') + ' · ' + AdminJsp.formatDateTimeSec(a.modDt)]
    ].map(function (d) {
      return '<dt class="col-sm-2">' + d[0] + '</dt><dd class="col-sm-4"' + (d[1] ? ' id="admin-' + d[1] + '"' : '') + '>' + d[2] + '</dd>';
    }).join('');
    byId('admin-info').innerHTML = '<div class="card-header"><h3 class="card-title" id="admin-title">' + esc(a.adminNm) +
      ' <span class="text-secondary ms-1">(' + esc(a.loginId) + ')</span>' +
      (a.self ? '<span class="badge bg-blue-lt ms-2">본인</span>' : '') + '</h3>' +
      '<div class="card-actions btn-list">' + actions + '</div></div>' +
      '<div class="card-body"><dl class="row mb-0">' + dl + '</dl></div>';

    bind('btn-unlock', '잠금을 해제하시겠습니까?', function () { return AdminApi.post(BASE + '/' + a.adminId + '/unlock'); }, '잠금이 해제되었습니다');
    bind('btn-password-reset', '비밀번호를 초기화하시겠습니까? 현재 로그인도 종료됩니다.', async function () {
      var r = await AdminApi.post(BASE + '/' + a.adminId + '/password-reset');
      sessionStorage.setItem(TEMP_KEY, r.tempPassword);
    }, '비밀번호가 초기화되었습니다');
    bind('btn-disable', '사용중지하시겠습니까? 현재 로그인도 종료됩니다.', function () {
      return AdminApi.patch(BASE + '/' + a.adminId + '/status', { statusCd: 'DISABLED', modDt: a.modDt });
    }, '사용중지되었습니다');
    bind('btn-enable', '다시 사용하게 하시겠습니까?', function () {
      return AdminApi.patch(BASE + '/' + a.adminId + '/status', { statusCd: 'ACTIVE', modDt: a.modDt });
    }, '재사용 처리되었습니다');
  }

  function renderRoles(options) {
    var a = admin;
    var granted = a.roles.map(function (r) { return r.roleId; });
    var addable = options.filter(function (o) { return o.assignable && granted.indexOf(o.roleId) < 0; });
    var header = '<div class="card-header"><h3 class="card-title">역할</h3>';
    if (!a.self) {
      header += '<div class="card-actions"><form class="d-flex gap-2" id="role-add-form">' +
        '<select class="form-select form-select-sm" id="role-add-select" name="roleId" aria-label="추가할 역할"' + (perm.update ? '' : ' disabled') + '>' +
        addable.map(function (o) { return '<option value="' + o.roleId + '">' + esc(o.roleNm) + '</option>'; }).join('') + '</select>' +
        '<button type="submit" class="btn btn-sm btn-primary text-nowrap" id="btn-role-add"' +
        (perm.update && addable.length ? '' : ' disabled') + (perm.update ? '' : ' title="' + NO_PERM + '"') + '>역할 추가</button></form></div>';
    }
    header += '</div>';
    var rows = a.roles.map(function (r) {
      var revoke = !a.self && a.roles.length >= 2
        ? '<button type="button" class="btn btn-sm btn-ghost-danger btn-role-revoke" data-role-id="' + r.roleId + '"' + permAttrs(perm.update) + '>회수</button>' : '';
      return '<tr data-role-id="' + r.roleId + '"><td>' + esc(r.roleNm) +
        (r.useYn === 'N' ? '<span class="badge bg-secondary-lt ms-1">사용 안 함</span>' : '') + '</td><td>' + esc(r.regNm || '-') +
        '</td><td>' + AdminJsp.formatDateTime(r.regDt) + '</td><td class="text-end">' + revoke + '</td></tr>';
    }).join('');
    byId('admin-roles').innerHTML = header + '<div class="table-responsive"><table class="table table-vcenter mb-0" id="role-table">' +
      '<thead><tr><th>역할명</th><th>부여자</th><th>부여일시</th><th></th></tr></thead><tbody>' + rows + '</tbody></table></div>';

    var form = byId('role-add-form');
    if (form) {
      form.addEventListener('submit', function (e) {
        e.preventDefault();
        act(function () { return AdminApi.post(BASE + '/' + a.adminId + '/roles', { roleId: Number(form.roleId.value) }); }, '역할이 부여되었습니다');
      });
    }
    document.querySelectorAll('.btn-role-revoke').forEach(function (b) {
      b.addEventListener('click', function () {
        if (!confirm('역할을 회수하시겠습니까?')) { return; }
        act(function () { return AdminApi.del(BASE + '/' + a.adminId + '/roles/' + b.dataset.roleId); }, '역할이 회수되었습니다');
      });
    });
  }

  function bind(id, question, call, notice) {
    var button = byId(id);
    if (!button) { return; }
    button.addEventListener('click', function () {
      if (confirm(question)) { act(call, notice); }
    });
  }

  /** 작업 뒤 상세를 다시 읽어 알림을 보여 준다. 보호 규칙(R3, R5)에 걸리면 서버가 거부한 이유를 보여 준다 */
  async function act(call, notice) {
    try {
      await call();
      AdminJsp.flash(notice);
      location.reload();
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  }

  // ===================== 등록·수정 (SCR-ADM-03) =====================

  async function initForm(adminId) {
    var form = byId('admin-form');
    var original = null;
    if (adminId) {
      original = await AdminApi.get(BASE + '/' + adminId);
      form.loginId.value = original.loginId;
      form.adminNm.value = original.adminNm;
      form.email.value = original.email;
      form.mobileNo.value = original.mobileNo || '';
      form.deptNm.value = original.deptNm || '';
      byId('btn-cancel').href = PAGE + '/' + adminId;
    } else {
      var options = await AdminApi.get(BASE + '/role-options');
      byId('admin-roles-choice').innerHTML = options.filter(function (o) { return o.assignable; }).map(function (o) {
        return '<label class="form-check form-check-inline"><input class="form-check-input" type="checkbox" name="roleIds" value="' +
          o.roleId + '" id="role-' + o.roleId + '"> ' + esc(o.roleNm) + '</label>';
      }).join('');
    }
    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      byId('admin-message').innerHTML = '';
      var body = {
        adminNm: form.adminNm.value.trim(), email: form.email.value.trim(),
        mobileNo: form.mobileNo.value.trim() || null, deptNm: form.deptNm.value.trim() || null
      };
      try {
        if (!original) {
          body.loginId = form.loginId.value.trim();
          body.roleIds = Array.prototype.slice.call(form.querySelectorAll('input[name="roleIds"]:checked'))
            .map(function (c) { return Number(c.value); });
          var created = await AdminApi.post(BASE, body);
          sessionStorage.setItem(TEMP_KEY, created.tempPassword);
          AdminJsp.flash('등록되었습니다');
          location.href = PAGE + '/' + created.adminId;
          return;
        }
        body.modDt = original.modDt;
        await AdminApi.put(BASE + '/' + adminId, body);
        AdminJsp.flash('수정되었습니다');
        location.href = PAGE + '/' + adminId;
      } catch (err) {
        byId('admin-message').innerHTML = '<div class="alert alert-danger" role="alert">' + esc(message(err)) + '</div>';
      }
    });
  }

  // ===================== 시작 =====================

  (async function () {
    var me = await AdminJsp.requireLogin();
    var granted = me.permissions.ADMIN || [];
    perm = {
      create: me.superAdmin || granted.indexOf('CREATE') >= 0,
      update: me.superAdmin || granted.indexOf('UPDATE') >= 0,
      delete: me.superAdmin || granted.indexOf('DELETE') >= 0
    };
    AdminJsp.showFlash();
    var page = byId('admin-page');
    var adminId = page.dataset.adminId ? Number(page.dataset.adminId) : null;
    try {
      if (page.dataset.page === 'list') { await initList(); }
      if (page.dataset.page === 'detail') { await initDetail(adminId); }
      if (page.dataset.page === 'form') { await initForm(adminId); }
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  })();
})();
