<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ADM-02 관리자 상세 (② JSP + API). admin-admin-api.js가 채운다. 마크업은 ③ ssr/admin-detail.jsp와 같다 --%>
<ui:layout title="관리자" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-admin-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="admin-page" data-page="detail" data-admin-id="${adminId}" hidden></div>
        <div id="temp-password-area"></div>
        <div class="card mb-3" id="admin-info"></div>
        <div class="card mb-3" id="admin-roles"></div>
        <div class="card" id="login-history">
            <div class="card-header"><h3 class="card-title">최근 로그인 이력</h3></div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="history-table">
                    <thead><tr><th>일시</th><th>결과</th><th>방식</th><th>IP</th></tr></thead>
                    <tbody id="history-tbody"></tbody>
                </table>
            </div>
        </div>
    </jsp:body>
</ui:layout>
