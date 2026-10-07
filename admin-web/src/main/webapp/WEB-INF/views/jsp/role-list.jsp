<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ROL-01 역할 목록 (② JSP + API). admin-role-api.js가 /api/v1/roles 로 채운다. 마크업은 ③ ssr/role-list.jsp와 같다 --%>
<ui:layout title="역할" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-perm.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="role-page" data-page="list" hidden></div>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">역할 목록</h3>
                <div class="card-actions">
                    <a class="btn btn-primary btn-sm disabled" id="btn-role-create" href="<c:url value='/jsp/roles/new'/>" aria-disabled="true">등록</a>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form class="row g-2" id="role-search">
                    <div class="col"><input type="text" class="form-control" id="search-keyword" name="keyword" placeholder="역할 코드 / 역할명"></div>
                    <div class="col-auto">
                        <select class="form-select" id="search-useYn" name="useYn">
                            <option value="">전체</option>
                            <option value="Y">사용</option>
                            <option value="N">사용 안 함</option>
                        </select>
                    </div>
                    <div class="col-auto"><button type="submit" class="btn">검색</button></div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="role-table">
                    <thead><tr><th>역할 코드</th><th>역할명</th><th>설명</th><th class="text-center">관리자 수</th><th class="text-center">사용</th></tr></thead>
                    <tbody id="role-tbody"></tbody>
                </table>
            </div>
        </div>
    </jsp:body>
</ui:layout>
