/*
 * ②③ 권한 체크 표: 역할 상세의 권한 탭(SCR-ROL-02, 08-role BR-03)과 권한관리의 역할 × 액션 표(SCR-PRM-01, 07-permission BR-02).
 * - READ가 아닌 칸을 체크하면 같은 행의 READ를 함께 체크하고, READ를 해제하면 같은 행 전체를 해제한다.
 * - 저장할 때는 비활성(내가 줄 수 없는) 칸도 현재 값 그대로 모은다 (API-ROL-09: 목록에 없는 메뉴는 회수).
 * ① React(RoleDetailPage.tsx, PermissionPage.tsx)는 같은 규칙을 상태로 구현한다.
 *
 * 마크업: 역할 상세 table#perm-table > tbody > tr.perm-row[data-menu-id] > td > input.perm-check[data-action]
 *        권한관리 table#grant-table > tbody > tr.grant-row[data-role-id] > td > input.perm-check[data-action]
 */
(function (global) {
  'use strict';

  function bind(table) {
    table.addEventListener('change', function (e) {
      var cb = e.target.closest('.perm-check');
      if (!cb) { return; }
      var row = cb.closest('tr');
      if (cb.dataset.action !== 'READ' && cb.checked) {
        var read = row.querySelector('.perm-check[data-action="READ"]');
        if (read) { read.checked = true; }
      } else if (cb.dataset.action === 'READ' && !cb.checked) {
        row.querySelectorAll('.perm-check').forEach(function (other) {
          if (!other.disabled) { other.checked = false; }
        });
      }
    });
  }

  /** 권한 설정 요청의 permissions (api/openapi.yaml RolePermissionSaveRequest) */
  function collect(table) {
    var result = [];
    table.querySelectorAll('tr.perm-row').forEach(function (row) {
      var actions = Array.prototype.slice.call(row.querySelectorAll('.perm-check:checked'))
          .map(function (cb) { return cb.dataset.action; });
      if (actions.length) {
        result.push({ menuId: Number(row.dataset.menuId), actions: actions });
      }
    });
    return result;
  }

  function rowActions(row) {
    return Array.prototype.slice.call(row.querySelectorAll('.perm-check:checked'))
        .map(function (cb) { return cb.dataset.action; });
  }

  /** 권한관리: 지금 체크 상태를 처음 값으로 기억한다 */
  function snapshot(table) {
    table.querySelectorAll('tr.grant-row').forEach(function (row) {
      row.dataset.initial = rowActions(row).join(',');
    });
  }

  /** 권한관리: 바뀐 역할만 최종 액션 목록으로 (api/openapi.yaml MenuRoleGrantSaveRequest) */
  function changes(table) {
    var result = [];
    table.querySelectorAll('tr.grant-row').forEach(function (row) {
      var actions = rowActions(row);
      if (actions.join(',') !== (row.dataset.initial || '')) {
        result.push({ roleId: Number(row.dataset.roleId), actions: actions });
      }
    });
    return result;
  }

  global.AdminRolePerm = { bind: bind, collect: collect, snapshot: snapshot, changes: changes };
})(window);
