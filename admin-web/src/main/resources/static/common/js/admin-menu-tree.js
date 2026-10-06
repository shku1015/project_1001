/*
 * ②③ 메뉴관리 트리 조작 (docs/05-screens/03-menu.md, ADR-0015).
 * - 드래그(SortableJS)와 ▲▼로 같은 상위 메뉴 안에서만 순서를 바꾼다. 다른 폴더로는 옮기지 않는다.
 * - 바꿔도 바로 저장하지 않는다. 바뀐 메뉴에 "변경됨"을 붙이고 [순서 저장] [되돌리기] 막대를 보여 준다.
 * - 저장할 때는 바뀐 상위 메뉴마다 하위 메뉴 전부를 새 순서대로 보낸다 (API-MNU-07).
 * ① React(MenuPage.tsx)는 같은 동작을 상태로 구현한다.
 *
 * 마크업: ul.menu-tree[data-parent-id] > li.menu-node[data-menu-id] > .menu-node-row (.drag-handle, .changed-badge)
 */
(function (global) {
  'use strict';

  var root;
  var original = {};    // 상위 메뉴 ID('' = 최상위) → 처음 하위 메뉴 ID 순서
  var editable = false;
  var onChange = function () {};

  function lists() {
    return Array.prototype.slice.call(root.querySelectorAll('ul.menu-tree'));
  }

  function childIds(ul) {
    return Array.prototype.slice.call(ul.children)
        .filter(function (li) { return li.classList.contains('menu-node'); })
        .map(function (li) { return li.dataset.menuId; });
  }

  function snapshot() {
    original = {};
    lists().forEach(function (ul) { original[ul.dataset.parentId] = childIds(ul); });
  }

  /** 바뀐 상위 메뉴 목록 */
  function changedParents() {
    return lists().filter(function (ul) {
      return childIds(ul).join(',') !== (original[ul.dataset.parentId] || []).join(',');
    });
  }

  function refresh() {
    lists().forEach(function (ul) {
      var before = original[ul.dataset.parentId] || [];
      Array.prototype.slice.call(ul.children).forEach(function (li, index) {
        var badge = li.querySelector(':scope > .menu-node-row .changed-badge');
        if (badge) {
          badge.hidden = before[index] === li.dataset.menuId;
        }
      });
    });
    var dirty = isDirty();
    var bar = document.getElementById('order-bar');
    if (bar) {
      bar.hidden = !dirty;
    }
    onChange(dirty);
  }

  function isDirty() {
    return changedParents().length > 0;
  }

  /** 선택한 메뉴를 같은 상위 메뉴 안에서 한 칸 옮긴다 (-1: 위로, +1: 아래로) */
  function move(menuId, step) {
    var li = root.querySelector('li.menu-node[data-menu-id="' + menuId + '"]');
    if (!li || !editable) {
      return;
    }
    if (step < 0 && li.previousElementSibling) {
      li.parentNode.insertBefore(li, li.previousElementSibling);
    } else if (step > 0 && li.nextElementSibling) {
      li.parentNode.insertBefore(li.nextElementSibling, li);
    }
    refresh();
  }

  /** 마지막으로 저장한 순서로 되돌린다 */
  function reset() {
    lists().forEach(function (ul) {
      (original[ul.dataset.parentId] || []).forEach(function (id) {
        var li = ul.querySelector(':scope > li.menu-node[data-menu-id="' + id + '"]');
        if (li) {
          ul.appendChild(li);
        }
      });
    });
    refresh();
  }

  /** 순서 저장 요청 본문 (api/openapi.yaml MenuOrderRequest) */
  function collectOrders() {
    return changedParents().map(function (ul) {
      return {
        parentMenuId: ul.dataset.parentId ? Number(ul.dataset.parentId) : null,
        menuIds: childIds(ul).map(Number)
      };
    });
  }

  /**
   * @param {{root: Element, editable: boolean, onChange?: function(boolean)}} options
   */
  function init(options) {
    root = options.root;
    editable = options.editable;
    onChange = options.onChange || onChange;
    snapshot();
    if (editable && global.Sortable) {
      lists().forEach(function (ul) {
        global.Sortable.create(ul, {
          group: 'menu-parent-' + (ul.dataset.parentId || 'root'),    // 같은 상위 메뉴 안에서만
          handle: '.drag-handle',
          draggable: 'li.menu-node',
          animation: 150,
          onEnd: refresh
        });
      });
    }
    refresh();
  }

  global.AdminMenuTree = {
    init: init,
    move: move,
    reset: reset,
    isDirty: isDirty,
    collectOrders: collectOrders,
    snapshot: function () { snapshot(); refresh(); }
  };
})(window);
