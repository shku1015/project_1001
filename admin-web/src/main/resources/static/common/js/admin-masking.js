/*
 * ②③ 마스킹 설정 (SCR-MSK-01). 표에서 바뀐 항목만 모은다 (API-MSK-02 MaskingPolicySaveRequest).
 * ①은 MaskingPage.tsx가 같은 동작을 상태로 한다.
 *
 * 마크업: table#masking-table > tbody > tr.masking-row[data-field-cd][data-mod-dt][data-initial="Y,Y"]
 *         > input#screen-{FIELD}, input#excel-{FIELD} (스위치)
 */
(function (global) {
  'use strict';

  function changes(table) {
    var result = [];
    table.querySelectorAll('tr.masking-row').forEach(function (row) {
      var field = row.dataset.fieldCd;
      var screen = document.getElementById('screen-' + field).checked ? 'Y' : 'N';
      var excel = document.getElementById('excel-' + field).checked ? 'Y' : 'N';
      if (screen + ',' + excel !== row.dataset.initial) {
        result.push({ fieldCd: field, screenMaskYn: screen, excelMaskYn: excel, modDt: row.dataset.modDt });
      }
    });
    return result;
  }

  global.AdminMasking = {
    changes: changes,
    CONFIRM: '마스킹을 끄면 개인정보가 노출됩니다. 저장하시겠습니까?',
    NO_CHANGE: '바뀐 내용이 없습니다.'
  };
})(window);
