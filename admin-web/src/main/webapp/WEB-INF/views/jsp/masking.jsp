<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-MSK-01 마스킹 설정 (② JSP + API). admin-masking-api.js가 /api/v1/masking-policies 로 채운다. 마크업은 ③ ssr/masking.jsp와 같다 --%>
<ui:layout title="마스킹 설정" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-masking.js'/>"></script>
        <script src="<c:url value='/common/js/admin-masking-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="card" id="masking-card">
            <div class="card-header"><h3 class="card-title">개인정보 항목별 마스킹</h3></div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="masking-table">
                    <thead><tr><th>개인정보 항목</th><th>마스킹 예</th><th class="text-center">화면 마스킹</th><th class="text-center">엑셀 마스킹</th><th>마지막 수정</th></tr></thead>
                    <tbody id="masking-tbody"></tbody>
                </table>
            </div>
            <div class="card-footer">
                <ul class="text-secondary mb-3" id="masking-note">
                    <li>화면 마스킹을 끄면 사용자관리 조회 권한만 있어도 원문이 보입니다.</li>
                    <li>엑셀 마스킹을 꺼도 개인정보열람 권한이 있는 관리자만 원문으로 내려받습니다.</li>
                </ul>
                <div id="masking-message"></div>
                <div class="text-end"><button type="button" class="btn btn-primary" id="btn-save" disabled>저장</button></div>
            </div>
        </div>
    </jsp:body>
</ui:layout>
