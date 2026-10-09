<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ADM-01 관리자 목록 (② JSP + API). admin-admin-api.js가 /api/v1/admins 로 채운다. 마크업은 ③ ssr/admin-list.jsp와 같다 --%>
<ui:layout title="관리자" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-admin-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="admin-page" data-page="list" hidden></div>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">관리자 목록</h3>
                <div class="card-actions">
                    <a class="btn btn-primary btn-sm disabled" id="btn-admin-create" href="<c:url value='/jsp/admins/new'/>" aria-disabled="true">등록</a>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form class="row g-2" id="admin-search">
                    <div class="col-md-2"><input class="form-control" id="search-loginId" name="loginId" placeholder="로그인 아이디" aria-label="로그인 아이디"></div>
                    <div class="col-md-2"><input class="form-control" id="search-adminNm" name="adminNm" placeholder="이름" aria-label="이름"></div>
                    <div class="col-md-2"><input class="form-control" id="search-deptNm" name="deptNm" placeholder="부서" aria-label="부서"></div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-roleId" name="roleId" aria-label="역할"><option value="">역할 전체</option></select>
                    </div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-statusCd" name="statusCd" aria-label="상태">
                            <option value="">상태 전체</option>
                            <option value="ACTIVE">사용</option>
                            <option value="LOCKED">잠금</option>
                            <option value="DISABLED">사용중지</option>
                        </select>
                    </div>
                    <div class="col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-primary">검색</button>
                        <a class="btn" id="btn-search-reset" href="<c:url value='/jsp/admins'/>">초기화</a>
                    </div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="admin-table">
                    <thead><tr id="admin-thead"></tr></thead>
                    <tbody id="admin-tbody"></tbody>
                </table>
            </div>
            <div id="pager"></div>
        </div>
    </jsp:body>
</ui:layout>
