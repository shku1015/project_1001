<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ADM-01 관리자 목록 (③ JSP SSR). ② jsp/admin-list.jsp(admin-admin-api.js), ① AdminListPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="path" value="/ssr/admins"/>
<ui:layout title="관리자" mode="ssr">
    <jsp:body>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">관리자 목록</h3>
                <div class="card-actions">
                    <c:choose>
                        <c:when test="${canCreate}"><a class="btn btn-primary btn-sm" id="btn-admin-create" href="<c:url value='/ssr/admins/new'/>">등록</a></c:when>
                        <c:otherwise><button type="button" class="btn btn-primary btn-sm" id="btn-admin-create" disabled title="${noPerm}">등록</button></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form method="get" action="<c:url value='${path}'/>" class="row g-2" id="admin-search">
                    <c:if test="${not empty param.size}"><input type="hidden" name="size" value="<c:out value='${param.size}'/>"></c:if>
                    <div class="col-md-2"><input class="form-control" id="search-loginId" name="loginId" placeholder="로그인 아이디" aria-label="로그인 아이디" value="<c:out value='${param.loginId}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-adminNm" name="adminNm" placeholder="이름" aria-label="이름" value="<c:out value='${param.adminNm}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-deptNm" name="deptNm" placeholder="부서" aria-label="부서" value="<c:out value='${param.deptNm}'/>"></div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-roleId" name="roleId" aria-label="역할">
                            <option value="">역할 전체</option>
                            <c:forEach var="r" items="${roleOptions}">
                                <option value="${r.roleId()}" ${param.roleId == r.roleId() ? 'selected' : ''}><c:out value="${r.roleNm()}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-statusCd" name="statusCd" aria-label="상태">
                            <option value="">상태 전체</option>
                            <option value="ACTIVE" ${param.statusCd == 'ACTIVE' ? 'selected' : ''}>사용</option>
                            <option value="LOCKED" ${param.statusCd == 'LOCKED' ? 'selected' : ''}>잠금</option>
                            <option value="DISABLED" ${param.statusCd == 'DISABLED' ? 'selected' : ''}>사용중지</option>
                        </select>
                    </div>
                    <div class="col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-primary">검색</button>
                        <a class="btn" id="btn-search-reset" href="<c:url value='${path}'/>">초기화</a>
                    </div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="admin-table">
                    <thead>
                    <tr>
                        <th>번호</th>
                        <ui:sortTh path="${path}" field="loginId" label="로그인 아이디" current="${sort}"/>
                        <ui:sortTh path="${path}" field="adminNm" label="이름" current="${sort}"/>
                        <th>부서</th><th>역할</th><th>상태</th>
                        <ui:sortTh path="${path}" field="lastLoginDt" label="마지막 로그인" current="${sort}"/>
                        <ui:sortTh path="${path}" field="regDt" label="등록일시" current="${sort}"/>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${result.items()}" varStatus="st">
                        <tr>
                            <td>${result.totalCount() - (result.page() - 1) * result.size() - st.index}</td>
                            <td><a href="<c:url value='/ssr/admins/${a.adminId()}'/>"><c:out value="${a.loginId()}"/></a></td>
                            <td><c:out value="${a.adminNm()}"/></td>
                            <td><c:out value="${empty a.deptNm() ? '-' : a.deptNm()}"/></td>
                            <td><c:choose>
                                <c:when test="${empty a.roles()}">-</c:when>
                                <c:otherwise><c:out value="${a.roles()[0].roleNm()}"/><c:if test="${a.roles().size() > 1}"> 외 ${a.roles().size() - 1}</c:if></c:otherwise>
                            </c:choose></td>
                            <td><c:out value="${a.statusNm()}"/></td>
                            <td>${a.lastLoginDt() == null ? '-' : a.lastLoginDt().format(dtMin)}</td>
                            <td>${a.regDt().format(dtMin)}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty result.items()}"><tr><td colspan="8" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
            <ui:pager path="${path}" page="${result.page()}" size="${result.size()}" totalPages="${result.totalPages()}" totalCount="${result.totalCount()}"/>
        </div>
    </jsp:body>
</ui:layout>
