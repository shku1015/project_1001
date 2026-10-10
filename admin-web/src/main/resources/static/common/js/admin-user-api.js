/*
 * ② JSP + API 사용자관리 (SCR-USR-01~03). /api/v1/users 로 목록·엑셀·상세·원문 보기·등록·수정·상태 변경·삭제를 처리한다.
 * 화면 구조·입력 칸 id는 ③ SSR(ssr/user-*.jsp)·① React(User*Page.tsx)와 같다.
 * 개인정보 원문은 화면에만 보여 주고 저장하지 않는다. 임시 비밀번호는 상세 화면 위쪽에 한 번만 보여 준다.
 */
(function () {
  'use strict';

  var BASE = '/users';
  var PAGE = '/jsp/users';
  var NO_PERM = '권한이 없습니다';
  var TEMP_KEY = 'user.tempPassword';
  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };
  var perm = {};

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  function permAttrs(allowed) {
    return allowed ? '' : ' disabled title="' + NO_PERM + '"';
  }

  /** 휴대폰 번호: 숫자면 하이픈을 넣고, 가린 값(010-****-5678)은 그대로 */
  function mobile(v) {
    return v && v.indexOf('*') < 0 ? AdminJsp.formatMobile(v) : esc(v || '-');
  }

  // ===================== 목록 (SCR-USR-01) =====================

  var FILTERS = ['userTypeCd', 'loginId', 'userNm', 'email', 'mobileNo', 'companyNm', 'statusCd', 'joinPath', 'joinDtFrom', 'joinDtTo'];
  // filters: [검색]을 누른 때의 조건 (엑셀도 이 조건으로 받는다). 기업 상세의 [전체 보기]는 ?companyId= 로 들어온다
  var query = { page: 1, size: 20, sort: '', filters: {} };

  function readFilters() {
    var form = byId('user-search');
    var filters = {};
    FILTERS.forEach(function (name) {
      var v = form[name].value.trim();
      if (v) { filters[name] = v; }
    });
    if (query.filters.companyId) { filters.companyId = query.filters.companyId; }
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
    var companyId = new URLSearchParams(location.search).get('companyId');
    if (companyId) { query.filters.companyId = companyId; }
    var create = byId('btn-user-create');
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
    byId('user-search').addEventListener('submit', function (e) {
      e.preventDefault();
      query.filters = readFilters();
      query.page = 1;
      loadList();
    });
    byId('user-thead').addEventListener('click', function (e) {
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
    byId('user-thead').innerHTML = '<th>번호</th><th>구분</th>' + AdminPager.sortTh('loginId', '로그인 아이디', query.sort) +
      AdminPager.sortTh('userNm', '이름', query.sort) + '<th>이메일</th><th>휴대폰 번호</th><th>소속 기업</th><th>상태</th>' +
      AdminPager.sortTh('joinDt', '가입일시', query.sort);
    byId('user-tbody').innerHTML = result.items.length ? result.items.map(function (u, i) {
      return '<tr><td>' + (result.totalCount - (result.page - 1) * result.size - i) + '</td><td>' + esc(u.userTypeNm) + '</td>' +
        '<td><a href="' + PAGE + '/' + u.userId + '">' + esc(u.loginId) + '</a></td><td>' + esc(u.userNm) + '</td>' +
        '<td>' + esc(u.email) + '</td><td>' + mobile(u.mobileNo) + '</td><td>' + esc(u.companyNm || '-') + '</td>' +
        '<td>' + esc(u.statusNm) + '</td><td>' + AdminJsp.formatDateTime(u.joinDt) + '</td></tr>';
    }).join('') : '<tr><td colspan="9" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr>';
    AdminPager.render(byId('pager'), result,
      function (page) { query.page = page; loadList(); },
      function (size) { query.size = size; query.page = 1; loadList(); });
  }

  // ===================== 상세 (SCR-USR-02) =====================

  var user;

  async function initDetail(userId) {
    showTempPassword();
    user = await AdminApi.get(BASE + '/' + userId);
    var histories = await AdminApi.get(BASE + '/' + userId + '/status-histories');
    var reasons = await AdminApi.get('/common/codes/PRIVACY_REASON');
    byId('privacy-reasonCd').insertAdjacentHTML('beforeend', reasons.map(function (r) {
      return '<option value="' + esc(r.code) + '">' + esc(r.codeNm) + '</option>';
    }).join(''));
    renderInfo(null);
    byId('history-tbody').innerHTML = histories.length ? histories.map(function (h) {
      return '<tr><td>' + AdminJsp.formatDateTimeSec(h.regDt) + '</td><td>' + esc(h.beforeStatusNm) + ' → ' + esc(h.afterStatusNm) +
        '</td><td>' + esc(h.reason) + '</td><td>' + esc(h.regNm || '-') + '</td></tr>';
    }).join('') : '<tr><td colspan="4" class="text-secondary text-center">상태 변경 이력이 없습니다</td></tr>';

    byId('privacy-reasonCd').addEventListener('change', function () {
      byId('privacy-etc-wrap').hidden = this.value !== 'ETC';
    });
    byId('privacy-form').addEventListener('submit', viewPrivacy);
    byId('reason-modal').addEventListener('show.bs.modal', function (e) {
      var b = e.relatedTarget;
      var form = byId('reason-form');
      byId('reason-title').textContent = b.dataset.title;
      byId('reason-warning').textContent = b.dataset.warning || '';
      byId('reason-warning').hidden = !b.dataset.warning;
      form.mode.value = b.dataset.statusCd ? 'status' : 'delete';
      form.statusCd.value = b.dataset.statusCd || '';
      byId('reason-input').value = '';
      byId('reason-message').innerHTML = '';
    });
    byId('reason-form').addEventListener('submit', submitReason);
  }

  function showTempPassword() {
    var temp = sessionStorage.getItem(TEMP_KEY);
    if (!temp) { return; }
    sessionStorage.removeItem(TEMP_KEY);
    byId('temp-password-area').innerHTML = '<div class="alert alert-warning" role="alert" id="temp-password-box">' +
      '<div class="d-flex align-items-center gap-2 flex-wrap"><span>임시 비밀번호:</span><code class="fs-3" id="temp-password">' +
      esc(temp) + '</code><button type="button" class="btn btn-sm" id="btn-temp-copy">복사</button>' +
      '<button type="button" class="btn btn-sm ms-auto" id="btn-temp-close">닫기</button></div>' +
      '<div class="mt-1 small">이 창을 닫거나 화면을 옮기면 다시 볼 수 없습니다. 회원에게 전달하세요.</div></div>';
    byId('btn-temp-copy').addEventListener('click', function () {
      navigator.clipboard.writeText(temp);
      this.textContent = '복사됨';
    });
    byId('btn-temp-close').addEventListener('click', function () { byId('temp-password-box').remove(); });
  }

  /** privacy: 원문 보기 결과 (없으면 마스킹된 값을 보여 준다) */
  function renderInfo(privacy) {
    var u = user;
    var withdrawn = u.statusCd === 'WITHDRAWN';
    byId('withdrawn-area').innerHTML = withdrawn
      ? '<div class="alert alert-info" role="note" id="withdrawn-note">탈퇴한 회원입니다. 정보를 수정하거나 상태를 바꿀 수 없습니다.</div>' : '';
    var reason = function (id, cls, statusCd, title, label, warning) {
      return ' <button type="button" class="btn btn-sm' + cls + '" id="' + id + '" data-bs-toggle="modal" data-bs-target="#reason-modal"' +
        (statusCd ? ' data-status-cd="' + statusCd + '"' : '') + ' data-title="' + title + '"' +
        (warning ? ' data-warning="' + warning + '"' : '') + permAttrs(statusCd ? perm.update : perm.delete) + '>' + label + '</button>';
    };
    var actions = '<a class="btn btn-sm" id="btn-list" href="' + PAGE + '">목록</a>';
    if (u.maskedFields.length && !privacy) {
      actions += ' <button type="button" class="btn btn-sm" id="btn-privacy" data-bs-toggle="modal" data-bs-target="#privacy-modal"' +
        permAttrs(perm.privacy) + '>원문 보기</button>';
    }
    if (!withdrawn) {
      actions += perm.edit
        ? ' <a class="btn btn-sm" id="btn-edit" href="' + PAGE + '/' + u.userId + '/edit">수정</a>'
        : ' <button type="button" class="btn btn-sm" id="btn-edit"' + permAttrs(false) + '>수정</button>';
      if (u.statusCd === 'ACTIVE') { actions += reason('btn-suspend', '', 'SUSPENDED', '회원 정지', '정지'); }
      if (u.statusCd === 'SUSPENDED') { actions += reason('btn-resume', '', 'ACTIVE', '정지 해제', '정지 해제'); }
      if (u.statusCd === 'DORMANT') { actions += reason('btn-wake', '', 'ACTIVE', '휴면 해제', '휴면 해제'); }
      actions += reason('btn-withdraw', ' btn-ghost-danger', 'WITHDRAWN', '강제 탈퇴', '강제 탈퇴', '강제 탈퇴는 되돌릴 수 없습니다.');
      actions += ' <button type="button" class="btn btn-sm" id="btn-password-reset"' + permAttrs(perm.update) + '>비밀번호 초기화</button>';
    }
    actions += reason('btn-delete', ' btn-ghost-danger', null, '회원 삭제', '삭제');

    var p = privacy || u;
    var birth = privacy ? (privacy.birthDate || '-') : (u.birthDate || '-');
    var dl = function (rows) {
      return rows.map(function (d) {
        return '<dt class="col-sm-2">' + d[0] + '</dt><dd class="col-sm-' + (d[3] || 4) + '"' + (d[1] ? ' id="user-' + d[1] + '"' : '') + '>' + d[2] + '</dd>';
      }).join('');
    };
    var html = '<div class="card-header"><h3 class="card-title" id="user-title">' + esc(u.loginId) + '</h3>' +
      '<div class="card-actions btn-list">' + actions + '</div></div><div class="card-body">' +
      '<h4 class="mb-2">기본 정보</h4><dl class="row">' + dl([
        ['회원 구분', 'userType', esc(u.userTypeNm)], ['로그인 아이디', 'loginId', esc(u.loginId)],
        ['상태', 'status', esc(u.statusNm)], ['가입 경로', 'joinPath', u.joinPath === 'ADMIN' ? '관리자 등록' : '사용자 서비스'],
        ['가입일시', null, AdminJsp.formatDateTimeSec(u.joinDt)], ['최근 로그인', null, AdminJsp.formatDateTimeSec(u.lastLoginDt)],
        ['탈퇴일시', 'withdrawDt', AdminJsp.formatDateTimeSec(u.withdrawDt)], ['임시 비밀번호', 'pwdTempYn', u.pwdTempYn === 'Y' ? '예' : '아니오']
      ]) + '</dl>' +
      '<h4 class="mb-2">개인정보' + (privacy ? ' <span class="badge bg-red-lt ms-1" id="privacy-revealed">원문</span>' : '') + '</h4>' +
      '<dl class="row" id="user-privacy">' + dl([
        ['이름', 'userNm', esc(p.userNm)], ['이메일', 'email', esc(p.email)],
        ['휴대폰 번호', 'mobileNo', mobile(p.mobileNo)], ['생년월일', 'birthDate', esc(birth)]
      ]) + '</dl>';
    if (u.userTypeCd === 'CORPORATE') {
      html += '<h4 class="mb-2">소속 정보</h4><dl class="row">' + dl([
        ['소속 기업', 'company', '<a href="/jsp/companies/' + u.companyId + '">' + esc(u.companyNm) + '</a>'],
        ['부서 / 직위', 'dept', esc(u.deptNm || '-') + ' / ' + esc(u.positionNm || '-')]
      ]) + '</dl>';
    }
    html += '<h4 class="mb-2">관리 정보</h4><dl class="row mb-0">' + dl([
      ['등록', null, esc(u.regNm || '-') + ' · ' + AdminJsp.formatDateTimeSec(u.regDt)],
      ['수정', null, esc(u.modNm || '-') + ' · ' + AdminJsp.formatDateTimeSec(u.modDt)]
    ]) + '</dl></div>';
    byId('user-info').innerHTML = html;
    var reset = byId('btn-password-reset');
    if (reset) { reset.addEventListener('click', resetPassword); }
  }

  /** 개인정보 원문 보기창(CMP-08) */
  async function viewPrivacy(e) {
    e.preventDefault();
    var form = byId('privacy-form');
    byId('privacy-message').innerHTML = '';
    try {
      var privacy = await AdminApi.post(BASE + '/' + user.userId + '/privacy',
        { reasonCd: form.reasonCd.value, reasonEtc: form.reasonEtc.value.trim() || null });
      form.querySelector('[data-bs-dismiss="modal"]').click();
      renderInfo(privacy);
    } catch (err) {
      byId('privacy-message').innerHTML = '<div class="alert alert-danger mt-2" role="alert">' + esc(message(err)) + '</div>';
    }
  }

  /** 사유 입력창(CMP-07): 상태 변경 또는 삭제 */
  async function submitReason(e) {
    e.preventDefault();
    var form = byId('reason-form');
    var reason = byId('reason-input').value.trim();
    if (!reason) {
      byId('reason-message').innerHTML = '<div class="invalid-feedback d-block">사유를 입력하세요.</div>';
      return;
    }
    try {
      if (form.mode.value === 'delete') {
        await AdminApi.del(BASE + '/' + user.userId, { reason: reason });
        AdminJsp.flash('삭제되었습니다');
        location.href = PAGE;
        return;
      }
      await AdminApi.patch(BASE + '/' + user.userId + '/status', { statusCd: form.statusCd.value, reason: reason, modDt: user.modDt });
      AdminJsp.flash('상태가 변경되었습니다');
      location.reload();
    } catch (err) {
      byId('reason-message').innerHTML = '<div class="alert alert-danger mt-2" role="alert">' + esc(message(err)) + '</div>';
    }
  }

  async function resetPassword() {
    if (!confirm('비밀번호를 초기화하시겠습니까?')) { return; }
    try {
      var r = await AdminApi.post(BASE + '/' + user.userId + '/password-reset');
      sessionStorage.setItem(TEMP_KEY, r.tempPassword);
      AdminJsp.flash('비밀번호가 초기화되었습니다');
      location.reload();
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  }

  // ===================== 등록·수정 (SCR-USR-03) =====================

  async function initForm(userId) {
    var form = byId('user-form');
    var companies = await AdminApi.get(BASE + '/company-options');
    var select = byId('user-companyId');
    select.insertAdjacentHTML('beforeend', companies.map(function (c) {
      return '<option value="' + c.companyId + '">' + esc(c.companyNm) + '</option>';
    }).join(''));
    var original = null;
    if (userId) {
      original = await AdminApi.get(BASE + '/' + userId + '/form');
      byId('user-type').textContent = original.userTypeCd === 'CORPORATE' ? '기업' : '개인';
      byId('user-type').hidden = false;
      byId('user-type-choice').remove();
      form.loginId.value = original.loginId;
      ['userNm', 'email', 'mobileNo', 'birthDate', 'deptNm', 'positionNm'].forEach(function (f) { form[f].value = original[f] || ''; });
      if (original.companyId && !select.querySelector('option[value="' + original.companyId + '"]')) {
        select.insertAdjacentHTML('beforeend', '<option value="' + original.companyId + '">' + esc(original.companyNm) + ' (현재)</option>');
      }
      select.value = original.companyId || '';
      byId('corporate-fields').hidden = original.userTypeCd !== 'CORPORATE';
      byId('btn-cancel').href = PAGE + '/' + userId;
    } else {
      form.querySelectorAll('input[name="userTypeCd"]').forEach(function (r) {
        r.addEventListener('change', function () { byId('corporate-fields').hidden = r.value !== 'CORPORATE'; });
      });
    }
    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      byId('user-message').innerHTML = '';
      var body = {
        userNm: form.userNm.value.trim(), email: form.email.value.trim(), mobileNo: form.mobileNo.value.trim() || null,
        birthDate: form.birthDate.value || null, companyId: select.value ? Number(select.value) : null,
        deptNm: form.deptNm.value.trim() || null, positionNm: form.positionNm.value.trim() || null
      };
      try {
        if (!original) {
          body.userTypeCd = form.querySelector('input[name="userTypeCd"]:checked').value;
          body.loginId = form.loginId.value.trim();
          var created = await AdminApi.post(BASE, body);
          sessionStorage.setItem(TEMP_KEY, created.tempPassword);
          AdminJsp.flash('등록되었습니다');
          location.href = PAGE + '/' + created.userId;
          return;
        }
        body.modDt = original.modDt;
        await AdminApi.put(BASE + '/' + userId, body);
        AdminJsp.flash('수정되었습니다');
        location.href = PAGE + '/' + userId;
      } catch (err) {
        byId('user-message').innerHTML = '<div class="alert alert-danger" role="alert">' + esc(message(err)) + '</div>';
      }
    });
  }

  // ===================== 시작 =====================

  (async function () {
    var me = await AdminJsp.requireLogin();
    var granted = me.permissions.USER || [];
    var has = function (a) { return me.superAdmin || granted.indexOf(a) >= 0; };
    perm = {
      create: has('CREATE') && has('PRIVACY'), edit: has('UPDATE') && has('PRIVACY'), update: has('UPDATE'),
      delete: has('DELETE'), excel: has('EXCEL'), privacy: has('PRIVACY')
    };
    AdminJsp.showFlash();
    var page = byId('user-page');
    var userId = page.dataset.userId ? Number(page.dataset.userId) : null;
    try {
      if (page.dataset.page === 'list') { await initList(); }
      if (page.dataset.page === 'detail') { await initDetail(userId); }
      if (page.dataset.page === 'form') { await initForm(userId); }
    } catch (e) {
      AdminJsp.showNotice(message(e), 'danger');
    }
  })();
})();
