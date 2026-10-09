/*
 * ② 목록 공통: CMP-03 페이징, CMP-02 정렬 머리글. 마크업은 ③ pager.tag·sortTh.tag, ① Pager.tsx와 같다.
 */
(function (global) {
  'use strict';

  function esc(v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; }

  /**
   * @param {Element} container 페이징을 넣을 자리 (#pager로 바뀐다)
   * @param {{page:number,size:number,totalCount:number,totalPages:number}} meta 목록 응답
   * @param {function(number)} onPage 페이지 번호를 고르면
   * @param {function(number)} onSize 페이지 크기를 고르면
   */
  function render(container, meta, onPage, onSize) {
    var last = Math.max(meta.totalPages, 1);
    var start = Math.floor((meta.page - 1) / 10) * 10 + 1;
    var end = Math.min(start + 9, last);
    function item(target, label, aria, off, active) {
      return '<li class="page-item' + (off ? ' disabled' : '') + (active ? ' active' : '') + '"><a class="page-link" href="#" data-page="' +
        target + '"' + (aria ? ' aria-label="' + aria + '"' : '') + '>' + label + '</a></li>';
    }
    var html = item(1, '«', '첫 페이지', meta.page <= 1) + item(meta.page - 1, '‹', '이전 페이지', meta.page <= 1);
    for (var n = start; n <= end; n++) { html += item(n, n, null, false, n === meta.page); }
    html += item(meta.page + 1, '›', '다음 페이지', meta.page >= last) + item(last, '»', '마지막 페이지', meta.page >= last);

    container.outerHTML = '<div class="card-footer d-flex align-items-center flex-wrap gap-2" id="pager">' +
      '<p class="m-0 text-secondary">총 <strong id="total-count">' + Number(meta.totalCount).toLocaleString('ko-KR') + '</strong>건</p>' +
      '<ul class="pagination m-0 ms-auto">' + html + '</ul>' +
      '<select class="form-select form-select-sm w-auto" id="page-size" aria-label="페이지 크기">' +
      [10, 20, 50].map(function (s) { return '<option value="' + s + '"' + (s === meta.size ? ' selected' : '') + '>' + s + '건씩</option>'; }).join('') +
      '</select></div>';
    var pager = document.getElementById('pager');
    pager.querySelectorAll('.page-link').forEach(function (a) {
      a.addEventListener('click', function (e) {
        e.preventDefault();
        if (!a.parentNode.classList.contains('disabled')) { onPage(Number(a.dataset.page)); }
      });
    });
    pager.querySelector('#page-size').addEventListener('change', function () { onSize(Number(this.value)); });
  }

  /** 정렬 머리글 <th>. current: 지금 정렬 ("loginId,asc") */
  function sortTh(field, label, current) {
    var dir = current === field + ',asc' ? 'asc' : current === field + ',desc' ? 'desc' : '';
    return '<th aria-sort="' + (dir === 'asc' ? 'ascending' : dir === 'desc' ? 'descending' : 'none') + '">' +
      '<a class="sort-link text-reset" href="#" data-sort="' + field + '">' + esc(label) +
      '<span class="sort-mark">' + (dir === 'asc' ? ' ▲' : dir === 'desc' ? ' ▼' : '') + '</span></a></th>';
  }

  /** 정렬 머리글을 누르면 다음 정렬 값 (없음·내림 → 오름, 오름 → 내림) */
  function nextSort(field, current) {
    return current === field + ',asc' ? field + ',desc' : field + ',asc';
  }

  global.AdminPager = { render: render, sortTh: sortTh, nextSort: nextSort };
})(window);
