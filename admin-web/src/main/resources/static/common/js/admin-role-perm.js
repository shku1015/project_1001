/*
 * ②③ 역할 권한 표 (SCR-ROL-02, docs/04-features/08-role.md BR-03).
 * - READ가 아닌 칸을 체크하면 같은 행의 READ를 함께 체크하고, READ를 해제하면 같은 행 전체를 해제한다.
 * - 저장할 때는 비활성(내가 줄 수 없는) 칸도 현재 값 그대로 모은다 (API-ROL-09: 목록에 없는 메뉴는 회수).
 * ① React(RoleDetailPage.tsx)는 같은 규칙을 상태로 구현한다.
 *
 * 마크업: table#perm-table > tbody > tr.perm-row[data-menu-id] > td > input.perm-check[data-action]
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

  global.AdminRolePerm = { bind: bind, collect: collect };
})(window);
