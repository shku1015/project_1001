<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-MSK-01 마스킹 설정 (③ JSP SSR). ② jsp/masking.jsp(admin-masking-api.js), ① MaskingPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<ui:layout title="마스킹 설정" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-masking.js'/>"></script>
        <script>
          document.getElementById('masking-form').addEventListener('submit', function (e) {
            var changes = AdminMasking.changes(document.getElementById('masking-table'));
            if (!changes.length) {
              e.preventDefault();
              document.getElementById('masking-message').innerHTML = '<div class="alert alert-info" role="note">' + AdminMasking.NO_CHANGE + '</div>';
              return;
            }
            if (!confirm(AdminMasking.CONFIRM)) { e.preventDefault(); return; }
            this.policies.value = JSON.stringify(changes);
          });
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="card" id="masking-card">
            <div class="card-header"><h3 class="card-title">개인정보 항목별 마스킹</h3></div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="masking-table">
                    <thead><tr><th>개인정보 항목</th><th>마스킹 예</th><th class="text-center">화면 마스킹</th><th class="text-center">엑셀 마스킹</th><th>마지막 수정</th></tr></thead>
                    <tbody>
                    <c:forEach var="p" items="${policies}">
                        <tr class="masking-row" data-field-cd="${p.fieldCd()}" data-mod-dt="${p.modDt().format(dtApi)}" data-initial="${p.screenMaskYn()},${p.excelMaskYn()}">
                            <td><c:out value="${p.fieldNm()}"/></td>
                            <td><code><c:out value="${p.example()}"/></code></td>
                            <td class="text-center"><label class="form-check form-switch d-inline-block m-0">
                                <input class="form-check-input" type="checkbox" id="screen-${p.fieldCd()}" aria-label="<c:out value='${p.fieldNm()}'/> 화면 마스킹"
                                       ${p.screenMaskYn() == 'Y' ? 'checked' : ''} ${canUpdate ? '' : 'disabled'}></label></td>
                            <td class="text-center"><label class="form-check form-switch d-inline-block m-0">
                                <input class="form-check-input" type="checkbox" id="excel-${p.fieldCd()}" aria-label="<c:out value='${p.fieldNm()}'/> 엑셀 마스킹"
                                       ${p.excelMaskYn() == 'Y' ? 'checked' : ''} ${canUpdate ? '' : 'disabled'}></label></td>
                            <td><c:out value="${empty p.modNm() ? '-' : p.modNm()}"/> · ${p.modDt().format(dtMin)}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="card-footer">
                <ul class="text-secondary mb-3" id="masking-note">
                    <li>화면 마스킹을 끄면 사용자관리 조회 권한만 있어도 원문이 보입니다.</li>
                    <li>엑셀 마스킹을 꺼도 개인정보열람 권한이 있는 관리자만 원문으로 내려받습니다.</li>
                </ul>
                <div id="masking-message"></div>
                <form method="post" action="<c:url value='/ssr/masking'/>" id="masking-form" class="text-end">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="policies">
                    <button type="submit" class="btn btn-primary" id="btn-save" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">저장</button>
                </form>
            </div>
        </div>
    </jsp:body>
</ui:layout>
