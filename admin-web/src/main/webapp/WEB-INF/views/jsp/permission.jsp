<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-PRM-01 권한관리 (② JSP + API). 메뉴 트리·역할 × 액션 표는 admin-permission-api.js가 /api/v1/permissions 로 채운다.
     마크업은 ③ ssr/permission.jsp와 같다 --%>
<ui:layout title="권한" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-perm.js'/>"></script>
        <script src="<c:url value='/common/js/admin-permission-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="row row-cards">
            <div class="col-lg-4">
                <div class="card" id="perm-menu-card">
                    <div class="card-header"><h3 class="card-title">메뉴</h3></div>
                    <div class="card-body" id="perm-menu-tree"></div>
                </div>
            </div>
            <div class="col-lg-8" id="grant-col">
                <div class="card"><div class="card-body text-secondary">왼쪽에서 화면 메뉴를 선택하세요.</div></div>
            </div>
        </div>
    </jsp:body>
</ui:layout>
