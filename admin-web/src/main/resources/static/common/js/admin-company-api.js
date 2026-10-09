/*
 * ② JSP + API 기업정보관리 (SCR-COM-01~03). /api/v1/companies 로 목록·엑셀·상세·등록·수정·상태 변경·삭제를 처리한다.
 * 화면 구조·입력 칸 id는 ③ SSR(ssr/company-*.jsp)·① React(Company*Page.tsx)와 같다.
 */
(function () {
  'use strict';

  var BASE = '/companies';
  var PAGE = '/jsp/companies';
  var NO_PERM = '권한이 없습니다';
  var FIELDS = ['companyNm', 'ceoNm', 'bizType', 'bizItem', 'telNo', 'zipCd', 'addr', 'addrDtl'];
  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };
  var perm = {};

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  function permAttrs(allowed) {
    return allowed ? '' : ' disabled title="' + NO_PERM + '"';
  }

  /** 1234567890 → 123-45-67890 */
  function bizRegNo(v) {
    return v && v.length === 10 ? v.substring(0, 3) + '-' + v.substring(3, 5) + '-' + v.substring(5) : esc(v);
  }

  // ===================== 목록 (SCR-COM-01) =====================

  // filters: [검색]을 누른 때의 조건 (엑셀도 이 조건으로 받는다)
  var query = { page: 1, size: 20, sort: '', filters: {} };

  function readFilters() {
    var form = byId('company-search');
    var filters = {};
    ['companyNm', 'bizRegNo', 'ceoNm', 'statusCd', 'regDtFrom', 'regDtTo'].forEach(function (name) {
      var v = form[name].value.trim();
      if (v) { filters[name] = v; }
    });
    return filters;
  }

  function searchParams(withPage) {
    var params = new URLSearchParams(query.filters);
    if (query.sort) { params.set('sort', query.sort); }
    if (withPage) {
      params.set('page', query.page);
      params.set('size', query.size);
    }
    return params;
  }

  async function initList() {
    var create = byId('btn-company-create');
    if (perm.create) {
      create.classList.remove('disabled');
      create.removeAttribute('aria-disabled');
    } else {
      create.title = NO_PERM;
    }
    var excel = byId('btn-excel');
    excel.disabled = !perm.excel;
    excel.title = perm.excel ? '' : NO_PERM;
    excel.addEventListener('click', function () {
      AdminApi.download(BASE + '/excel?' + searchParams(false)).catch(function (e) { AdminJsp.showNotice(message(e), 'danger'); });
    });
    byId('company-search').addEventListener('submit', function (e) {
      e.preventDefault();
      query.filters = readFilters();
      query.page = 1;
      loadList();
    });
    byId('company-thead').addEventListener('click', function (e) {
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
    var result = await AdminApi.get(BASE + '?' + searchParams(true));
    byId('company-thead').innerHTML = '<th>번호</th>' + AdminPager.sortTh('companyNm', '기업명', query.sort) +
      '<th>사업자등록번호</th><th>대표자</th>' + AdminPager.sortTh('memberCnt', '소속 회원 수', query.sort) +
      '<th>상태</th>' + AdminPager.sortTh('regDt', '등록일시', query.sort);
    byId('company-tbody').innerHTML = result.items.length ? result.items.map(function (c, i) {
      return '<tr><td>' + (result.totalCount - (result.page - 1) * result.size - i) + '</td>' +
        '<td><a href="' + PAGE + '/' + c.companyId + '">' + esc(c.companyNm) + '</a></td>' +
        '<td>' + bizRegNo(c.bizRegNo) + '</td><td>' + esc(c.ceoNm) + '</td><td>' + c.memberCnt + '</td>' +
        '<td>' + esc(c.statusNm) + '</td><td>' + AdminJsp.formatDateTime(c.regDt) + '</td></tr>';
    }).join('') : '<tr><td colspan="7" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr>';
    AdminPager.render(byId('pager'), result,
      function (page) { query.page = page; loadList(); },
      function (size) { query.size = size; query.page = 1; loadList(); });
  }

  // ===================== 상세 (SCR-COM-02) =====================

  var company;

  async function initDetail(companyId) {
    company = await AdminApi.get(BASE + '/' + companyId);
    var users = await AdminApi.get(BASE + '/' + companyId + '/users?size=10');
    renderInfo();
    renderUsers(users);
    byId('reason-modal').addEventListener('show.bs.modal', function (e) {
      var b = e.relatedTarget;
      byId('reason-title').textContent = b.dataset.title;
      byId('reason-form').statusCd.value = b.dataset.statusCd;
      byId('reason-input').value = '';
      byId('reason-message').innerHTML = '';
    });
    byId('reason-form').addEventListener('submit', changeStatus);
  }

  function renderInfo() {
    var c = company;
    var actions = '<a class="btn btn-sm" id="btn-list" href="' + PAGE + '">목록</a>';
    actions += perm.update
      ? ' <a class="btn btn-sm" id="btn-edit" href="' + PAGE + '/' + c.companyId + '/edit">수정</a>'
      : ' <button type="button" class="btn btn-sm" id="btn-edit"' + permAttrs(false) + '>수정</button>';
    actions += c.statusCd === 'ACTIVE'
      ? ' <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-suspend" data-bs-toggle="modal" data-bs-target="#reason-modal" data-status-cd="SUSPENDED" data-title="기업 정지"' + permAttrs(perm.update) + '>정지</button>'
      : ' <button type="button" class="btn btn-sm" id="btn-resume" data-bs-toggle="modal" data-bs-target="#reason-modal" data-status-cd="ACTIVE" data-title="정지 해제"' + permAttrs(perm.update) + '>정지 해제</button>';
    if (c.memberCnt === 0) {
      actions += ' <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-delete"' + permAttrs(perm.delete) + '>삭제</button>';
    }
    var addr = c.addr ? '(' + esc(c.zipCd) + ') ' + esc(c.addr) + ' ' + esc(c.addrDtl || '') : '-';
    var dl = [
      ['기업명', 'companyNm', esc(c.companyNm)], ['사업자등록번호', 'bizRegNo', bizRegNo(c.bizRegNo)],
      ['대표자명', 'ceoNm', esc(c.ceoNm)], ['상태', 'status', esc(c.statusNm)],
      ['업태', 'bizType', esc(c.bizType || '-')], ['종목', 'bizItem', esc(c.bizItem || '-')],
      ['대표 전화번호', 'telNo', esc(c.telNo || '-')], ['소속 회원 수', 'memberCnt', c.memberCnt]
    ].map(function (d) {
      return '<dt class="col-sm-2">' + d[0] + '</dt><dd class="col-sm-4" id="company-' + d[1] + '">' + d[2] + '</dd>';
    }).join('') +
      '<dt class="col-sm-2">주소</dt><dd class="col-sm-10" id="company-addr">' + addr + '</dd>' +
      '<dt class="col-sm-2">등록</dt><dd class="col-sm-4">' + esc(c.regNm || '-') + ' · ' + AdminJsp.formatDateTimeSec(c.regDt) + '</dd>' +
      '<dt class="col-sm-2">수정</dt><dd class="col-sm-4">' + esc(c.modNm || '-') + ' · ' + AdminJsp.formatDateTimeSec(c.modDt) + '</dd>';
    byId('company-info').innerHTML = '<div class="card-header"><h3 class="card-title" id="company-title">' + esc(c.companyNm) + '</h3>' +
      '<div class="card-actions btn-list">' + actions + '</div></div><div class="card-body">' +
      (c.memberCnt > 0 ? '<div class="alert alert-info" role="note" id="delete-note">소속 회원이 있어 삭제할 수 없습니다.</div>' : '') +
      '<dl class="row mb-0">' + dl + '</dl></div>';
    var del = byId('btn-delete');
    if (del) { del.addEventListener('click', remove); }
  }

  function renderUsers(users) {
    var rows = users.items.length ? users.items.map(function (u) {
      return '<tr><td><a href="/jsp/users/' + u.userId + '">' + esc(u.loginId) + '</a></td><td>' + esc(u.userNm) + '</td>' +
        '<td>' + esc(u.deptNm || '-') + ' / ' + esc(u.positionNm || '-') + '</td><td>' + esc(u.statusNm) + '</td>' +
        '<td>' + AdminJsp.formatDateTime(u.joinDt) + '</td></tr>';
    }).join('') : '<tr><td colspan="5" class="text-secondary text-center">소속 회원이 없습니다</td></tr>';
    byId('company-users').innerHTML = '<div class="card-header"><h3 class="card-title">소속 회원 <span class="text-secondary" id="users-total">(' +
      users.totalCount + '명)</span></h3><div class="card-actions"><a class="btn btn-sm" id="btn-users-all" href="/jsp/users?companyId=' +
      company.companyId + '">전체 보기</a></div></div><div class="table-responsive"><table class="table table-vcenter mb-0" id="users-table">' +
      '<thead><tr><th>로그인 아이디</th><th>이름</th><th>부서 / 직위</th><th>상태</th><th>가입일시</th></tr></thead><tbody>' + rows + '</tbody></table></div>';
  }

  /** 사유 입력창(CMP-07)에서 확인 */
  async function changeStatus(e) {
    e.preventDefault();
    var form = byId('reason-form');
    var reason = byId('reason-input').value.trim();
    if (!reason) {
      byId('reason-message').innerHTML = '<div class="invalid-feedback d-block">사유를 입력하세요.</div>';
      return;
    }
    try {
      await AdminApi.patch(BASE + '/' + company.companyId + '/status',
        { statusCd: form.statusCd.value, reason: reason, modDt: company.modDt });
      AdminJsp.flash(form.statusCd.value === 'SUSPENDED' ? '정지되었습니다' : '정지 해제되었습니다');
      location.reload();
    } catch (err) {
      byId('reason-message').innerHTML = '<div class="alert alert-danger mt-2" role="alert">' + esc(message(err)) + '</div>';
    }
  }

  async function remove() {
    if (!confirm('기업을 삭제하시겠습니까?')) { return; }
    try {
      await AdminApi.del(BASE + '/' + company.companyId);
      AdminJsp.flash('삭제되었습니다');
      location.href = PAGE;
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  }

  // ===================== 등록·수정 (SCR-COM-03) =====================

  async function initForm(companyId) {
    var form = byId('company-form');
    var original = null;
    if (companyId) {
      original = await AdminApi.get(BASE + '/' + companyId);
      form.bizRegNo.value = original.bizRegNo;
      FIELDS.forEach(function (f) { form[f].value = original[f] || ''; });
      byId('btn-cancel').href = PAGE + '/' + companyId;
    }
    byId('btn-postcode').addEventListener('click', function () {
      AdminFormKit.openPostcode(form.zipCd, form.addr, form.addrDtl).catch(function (e) { AdminJsp.showNotice(e.message, 'danger'); });
    });
    var changed = false;
    form.addEventListener('input', function () { changed = true; });
    byId('btn-cancel').addEventListener('click', function (e) {
      if (changed && !confirm('입력한 내용이 사라집니다. 취소하시겠습니까?')) { e.preventDefault(); }
    });
    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      byId('company-message').innerHTML = '';
      var body = {};
      FIELDS.forEach(function (f) { body[f] = form[f].value.trim() || null; });
      try {
        if (!original) {
          body.bizRegNo = form.bizRegNo.value.trim();
          var created = await AdminApi.post(BASE, body);
          AdminJsp.flash('등록되었습니다');
          location.href = PAGE + '/' + created.companyId;
          return;
        }
        body.modDt = original.modDt;
        await AdminApi.put(BASE + '/' + companyId, body);
        AdminJsp.flash('수정되었습니다');
        location.href = PAGE + '/' + companyId;
      } catch (err) {
        byId('company-message').innerHTML = '<div class="alert alert-danger" role="alert">' + esc(message(err)) + '</div>';
      }
    });
  }

  // ===================== 시작 =====================

  (async function () {
    var me = await AdminJsp.requireLogin();
    var granted = me.permissions.COMPANY || [];
    var has = function (a) { return me.superAdmin || granted.indexOf(a) >= 0; };
    perm = { create: has('CREATE'), update: has('UPDATE'), delete: has('DELETE'), excel: has('EXCEL') };
    AdminJsp.showFlash();
    var page = byId('company-page');
    var companyId = page.dataset.companyId ? Number(page.dataset.companyId) : null;
    try {
      if (page.dataset.page === 'list') { await initList(); }
      if (page.dataset.page === 'detail') { await initDetail(companyId); }
      if (page.dataset.page === 'form') { await initForm(companyId); }
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  })();
})();
