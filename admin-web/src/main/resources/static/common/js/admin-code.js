/*
 * ③ JSP SSR 코드관리 모달: 클릭한 버튼의 data-* 로 폼을 채우고 전송 URL(action)을 정한다.
 * (② JSP + API와 ① React는 같은 화면을 API 호출로 구현한다)
 */
(function () {
  'use strict';
  var base = '/ssr/codes/groups';
  var group = window.CODE_GROUP;

  // ----- 그룹코드 모달 -----
  var groupModal = document.getElementById('group-modal');
  if (groupModal) {
    groupModal.addEventListener('show.bs.modal', function (event) {
      var b = event.relatedTarget;
      var edit = b.dataset.mode === 'edit';
      var form = document.getElementById('group-form');
      document.getElementById('group-modal-title').textContent = edit ? '그룹 수정' : '그룹 등록';

      var groupCd = document.getElementById('group-groupCd');
      var display = document.getElementById('group-groupCd-display');
      form.groupNm.value = edit ? b.dataset.groupNm : '';
      form.description.value = edit ? (b.dataset.description || '') : '';
      setRadio(form.useYn, edit ? b.dataset.useYn : 'Y');

      if (edit) {
        // 수정: 그룹코드는 바꿀 수 없다 → 표시만, 전송 URL은 update
        form.action = base + '/' + b.dataset.groupCd + '/update';
        document.getElementById('group-modDt').value = b.dataset.modDt;
        groupCd.closest('.mb-3').hidden = true;
        display.hidden = false;
        display.textContent = '그룹코드: ' + b.dataset.groupCd;
        groupCd.required = false;
        // 시스템 코드는 사용 여부를 바꿀 수 없다
        document.getElementById('group-useYn-wrap').hidden = b.dataset.systemYn === 'Y';
      } else {
        form.action = base;
        document.getElementById('group-modDt').value = '';
        form.groupCd.value = '';
        groupCd.closest('.mb-3').hidden = false;
        display.hidden = true;
        groupCd.required = true;
        document.getElementById('group-useYn-wrap').hidden = false;
      }
    });
  }

  // ----- 상세코드 모달 -----
  var detailModal = document.getElementById('detail-modal');
  if (detailModal) {
    detailModal.addEventListener('show.bs.modal', function (event) {
      var b = event.relatedTarget;
      var edit = b.dataset.mode === 'edit';
      var form = document.getElementById('detail-form');
      document.getElementById('detail-modal-title').textContent = edit ? '코드 수정' : '코드 등록';

      var code = document.getElementById('detail-code');
      form.codeNm.value = edit ? b.dataset.codeNm : '';
      form.description.value = edit ? (b.dataset.description || '') : '';
      setRadio(form.useYn, edit ? b.dataset.useYn : 'Y');

      if (edit) {
        form.action = base + '/' + group + '/codes/' + b.dataset.code + '/update';
        document.getElementById('detail-modDt').value = b.dataset.modDt;
        form.sortOrd.value = b.dataset.sortOrd;
        code.closest('.mb-3').querySelector('label').textContent = '코드';
        code.value = b.dataset.code;
        code.readOnly = true;
        document.getElementById('detail-useYn-wrap').hidden = b.dataset.systemYn === 'Y';
      } else {
        form.action = base + '/' + group + '/codes';
        document.getElementById('detail-modDt').value = '';
        form.sortOrd.value = b.dataset.nextSort;
        code.value = '';
        code.readOnly = false;
        document.getElementById('detail-useYn-wrap').hidden = false;
      }
    });
  }

  function setRadio(radios, value) {
    Array.prototype.forEach.call(radios, function (r) { r.checked = r.value === value; });
  }
})();
