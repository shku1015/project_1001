/*
 * ②③ 공통 입력 부품. ①은 DateRange.tsx, lib/postcode.ts가 같은 동작을 한다.
 * - CMP-05 기간 선택: .date-range 안의 [data-range] 버튼(오늘·1주일·1개월·3개월)이 시작일~종료일을 채운다.
 * - CMP-16 우편번호 검색: 카카오 우편번호 서비스. 스크립트는 버튼을 누를 때 한 번만 불러온다.
 */
(function (global) {
  'use strict';

  function ymd(d) {
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  }

  /** 빠른 선택 값 → [시작일, 종료일] (오늘 포함) */
  function quickRange(range) {
    var to = new Date();
    var from = new Date();
    if (range === '1w') { from.setDate(from.getDate() - 7); }
    if (range === '1m') { from.setMonth(from.getMonth() - 1); }
    if (range === '3m') { from.setMonth(from.getMonth() - 3); }
    return [ymd(from), ymd(to)];
  }

  function bindDateRanges(root) {
    (root || document).querySelectorAll('.date-range').forEach(function (box) {
      box.querySelectorAll('[data-range]').forEach(function (b) {
        b.addEventListener('click', function () {
          var r = quickRange(b.dataset.range);
          box.querySelector('[data-range-from]').value = r[0];
          box.querySelector('[data-range-to]').value = r[1];
        });
      });
    });
  }

  var POSTCODE_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
  var loading = null;

  function loadPostcode() {
    if (global.daum && global.daum.Postcode) { return Promise.resolve(); }
    if (!loading) {
      loading = new Promise(function (resolve, reject) {
        var s = document.createElement('script');
        s.src = POSTCODE_URL;
        s.onload = resolve;
        s.onerror = function () { loading = null; reject(new Error('우편번호 서비스를 불러올 수 없습니다.')); };
        document.head.appendChild(s);
      });
    }
    return loading;
  }

  /** 우편번호 검색을 열고, 고르면 우편번호·주소 칸을 채운 뒤 상세주소로 옮긴다 */
  function openPostcode(zipInput, addrInput, detailInput) {
    return loadPostcode().then(function () {
      new global.daum.Postcode({
        oncomplete: function (data) {
          zipInput.value = data.zonecode;
          addrInput.value = data.roadAddress || data.jibunAddress;
          if (detailInput) { detailInput.focus(); }
        }
      }).open();
    });
  }

  global.AdminFormKit = { quickRange: quickRange, bindDateRanges: bindDateRanges, openPostcode: openPostcode };
  document.addEventListener('DOMContentLoaded', function () { bindDateRanges(document); });
})(window);
