/*
 * ② JSP + API 마스킹 설정 (SCR-MSK-01). /api/v1/masking-policies 로 읽고 바뀐 항목만 저장한다.
 * 마크업은 ③ ssr/masking.jsp와 같다. 바뀐 항목 모으기는 admin-masking.js를 함께 쓴다.
 */
(function () {
  'use strict';

  var byId = function (id) { return document.getElementById(id); };
  var esc = function (v) { var d = document.createElement('div'); d.textContent = v == null ? '' : String(v); return d.innerHTML; };
  var canUpdate = false;

  async function load() {
    var policies = await AdminApi.get('/masking-policies');
    var off = canUpdate ? '' : ' disabled';
    byId('masking-tbody').innerHTML = policies.map(function (p) {
      var sw = function (kind, yn, label) {
        return '<td class="text-center"><label class="form-check form-switch d-inline-block m-0"><input class="form-check-input" type="checkbox" id="' +
          kind + '-' + p.fieldCd + '" aria-label="' + esc(p.fieldNm) + ' ' + label + '"' + (yn === 'Y' ? ' checked' : '') + off + '></label></td>';
      };
      return '<tr class="masking-row" data-field-cd="' + p.fieldCd + '" data-mod-dt="' + p.modDt + '" data-initial="' +
        p.screenMaskYn + ',' + p.excelMaskYn + '"><td>' + esc(p.fieldNm) + '</td><td><code>' + esc(p.example) + '</code></td>' +
        sw('screen', p.screenMaskYn, '화면 마스킹') + sw('excel', p.excelMaskYn, '엑셀 마스킹') +
        '<td>' + esc(p.modNm || '-') + ' · ' + AdminJsp.formatDateTime(p.modDt) + '</td></tr>';
    }).join('');
  }

  async function save() {
    byId('masking-message').innerHTML = '';
    var changes = AdminMasking.changes(byId('masking-table'));
    if (!changes.length) {
      byId('masking-message').innerHTML = '<div class="alert alert-info" role="note">' + AdminMasking.NO_CHANGE + '</div>';
      return;
    }
    if (!confirm(AdminMasking.CONFIRM)) { return; }
    try {
      await AdminApi.put('/masking-policies', { policies: changes });
      AdminJsp.showNotice('저장되었습니다');
      await load();
    } catch (e) {
      AdminJsp.showNotice(e.message, 'danger');
    }
  }

  (async function () {
    var me = await AdminJsp.requireLogin();
    canUpdate = me.superAdmin || (me.permissions.MASKING || []).indexOf('UPDATE') >= 0;
    var button = byId('btn-save');
    button.disabled = !canUpdate;
    button.title = canUpdate ? '' : '권한이 없습니다';
    button.addEventListener('click', save);
    try {
      await load();
    } catch (e) {
      AdminJsp.showNotice(e.message, 'danger');
    }
  })();
})();
