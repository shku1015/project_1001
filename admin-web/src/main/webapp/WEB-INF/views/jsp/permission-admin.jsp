<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-PRM-02 관리자별 최종 권한 (② JSP + API). 관리자 선택(CMP-12)은 /api/v1/admins?keyword= 로 찾는다.
     마크업은 ③ ssr/permission-admin.jsp와 같다 --%>
<ui:layout title="권한" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-permission-admin-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="mb-3"><a class="btn btn-sm" id="btn-back" href="<c:url value='/jsp/permissions'/>">메뉴 기준 권한관리</a></div>
        <div class="card mb-3" id="admin-picker">
            <div class="card-header"><h3 class="card-title">관리자 선택</h3></div>
            <div class="card-body">
                <form class="d-flex gap-2" id="picker-form">
                    <input class="form-control" id="picker-keyword" name="keyword" placeholder="이름 또는 로그인 아이디" aria-label="이름 또는 로그인 아이디">
                    <button type="submit" class="btn btn-primary text-nowrap">검색</button>
                </form>
            </div>
            <div id="picker-result"></div>
        </div>
        <div id="effective-area"></div>
    </jsp:body>
</ui:layout>
