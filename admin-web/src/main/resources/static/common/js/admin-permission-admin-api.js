/*
 * ② JSP + API 관리자별 최종 권한 (SCR-PRM-02). 관리자 선택(CMP-12)은 /api/v1/admins?keyword= 로 10건을 찾고,
 * 고른 관리자의 최종 권한을 /api/v1/permissions/admins/{adminId} 로 보여 준다 (조회 전용).
 * 마크업은 ③ ssr/permission-admin.jsp와 같다. 고른 관리자는 주소의 ?adminId= 로 남겨 새로 고쳐도 유지한다.
 */
(function () {
  'use strict';

  var ACTIONS = ['READ', 'CREATE', 'UPDATE', 'DELETE', 'EXCEL', 'PRIVACY'];
  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };

  function message(e) {
    return e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
  }

  async function search(keyword) {
    var result = await AdminApi.get('/admins?size=10&keyword=' + encodeURIComponent(keyword));
    byId('picker-result').innerHTML = '<div class="table-responsive"><table class="table table-vcenter mb-0" id="picker-table">' +
      '<thead><tr><th>로그인 아이디</th><th>이름</th><th>부서</th><th>상태</th><th></th></tr></thead><tbody>' +
      (result.items.length ? result.items.map(function (a) {
        return '<tr><td>' + esc(a.loginId) + '</td><td>' + esc(a.adminNm) + '</td><td>' + esc(a.deptNm || '-') + '</td><td>' +
          esc(a.statusNm) + '</td><td class="text-end"><a class="btn btn-sm btn-pick" href="#" data-admin-id="' + a.adminId + '">선택</a></td></tr>';
      }).join('') : '<tr><td colspan="5" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr>') +
      '</tbody></table></div>';
  }

  async function show(adminId) {
    var e = await AdminApi.get('/permissions/admins/' + adminId);
    var html = '<div class="card" id="effective-card"><div class="card-header"><h3 class="card-title" id="effective-title">' +
      esc(e.admin.adminNm) + ' <span class="text-secondary ms-1">(' + esc(e.admin.loginId) + ')</span>의 최종 권한</h3></div>';
    if (e.superAdmin) {
      html += '<div class="card-body"><div class="alert alert-info mb-0" role="note">슈퍼관리자는 모든 권한을 가집니다.</div></div>';
    } else {
      html += '<div class="table-responsive"><table class="table table-vcenter table-sm mb-0" id="effective-table"><thead><tr><th>메뉴</th>' +
        '<th class="text-center">조회</th><th class="text-center">등록</th><th class="text-center">수정</th>' +
        '<th class="text-center">삭제</th><th class="text-center">엑셀</th><th class="text-center">개인정보</th></tr></thead><tbody>' +
        e.menus.map(function (m) {
          var folder = m.menuTypeCd === 'FOLDER';
          return '<tr data-menu-id="' + m.menuId + '"><td class="perm-depth-' + m.depth + (folder ? ' fw-bold' : '') + '">' + esc(m.menuNm) + '</td>' +
            ACTIONS.map(function (a) {
              var cell = '';
              if (m.actions.indexOf(a) >= 0) {
                cell = m.granted[a]
                  ? '<span class="perm-granted" title="' + esc(m.granted[a].join(', ')) + '">●</span>'
                  : '<span class="text-secondary">-</span>';
              }
              return '<td class="text-center" data-action="' + a + '">' + cell + '</td>';
            }).join('') + '</tr>';
        }).join('') + '</tbody></table></div>';
    }
    byId('effective-area').innerHTML = html + '</div>';
  }

  (async function () {
    await AdminJsp.requireLogin();
    byId('picker-form').addEventListener('submit', function (e) {
      e.preventDefault();
      var keyword = byId('picker-keyword').value.trim();
      if (keyword) { search(keyword).catch(function (err) { AdminJsp.showNotice(message(err), 'danger'); }); }
    });
    byId('picker-result').addEventListener('click', function (e) {
      var pick = e.target.closest('.btn-pick');
      if (!pick) { return; }
      e.preventDefault();
      history.replaceState(null, '', '?adminId=' + pick.dataset.adminId);
      show(pick.dataset.adminId).catch(function (err) { AdminJsp.showNotice(message(err), 'danger'); });
    });
    var adminId = new URLSearchParams(location.search).get('adminId');
    if (adminId) {
      show(adminId).catch(function (err) { AdminJsp.showNotice(message(err), 'danger'); });
    }
  })();
})();
