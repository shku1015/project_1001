<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ROL-01 역할 목록 (③ JSP SSR). ② jsp/role-list.jsp, ① RoleListPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<ui:layout title="역할" mode="ssr">
    <jsp:body>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">역할 목록</h3>
                <div class="card-actions">
                    <c:choose>
                        <c:when test="${canCreate}"><a class="btn btn-primary btn-sm" id="btn-role-create" href="<c:url value='/ssr/roles/new'/>">등록</a></c:when>
                        <c:otherwise><button type="button" class="btn btn-primary btn-sm" id="btn-role-create" disabled title="${noPerm}">등록</button></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form method="get" action="<c:url value='/ssr/roles'/>" class="row g-2" id="role-search">
                    <div class="col">
                        <input type="text" class="form-control" id="search-keyword" name="keyword" placeholder="역할 코드 / 역할명"
                               value="<c:out value='${keyword}'/>">
                    </div>
                    <div class="col-auto">
                        <select class="form-select" id="search-useYn" name="useYn">
                            <option value="">전체</option>
                            <option value="Y" ${searchUseYn == 'Y' ? 'selected' : ''}>사용</option>
                            <option value="N" ${searchUseYn == 'N' ? 'selected' : ''}>사용 안 함</option>
                        </select>
                    </div>
                    <div class="col-auto"><button type="submit" class="btn">검색</button></div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="role-table">
                    <thead><tr><th>역할 코드</th><th>역할명</th><th>설명</th><th class="text-center">관리자 수</th><th class="text-center">사용</th></tr></thead>
                    <tbody>
                    <c:forEach var="r" items="${roles}">
                        <tr>
                            <td><a href="<c:url value='/ssr/roles/${r.roleId()}'/>"><c:if test="${r.systemYn() == 'Y'}">&#x1F512; </c:if><c:out value="${r.roleCd()}"/></a></td>
                            <td><c:out value="${r.roleNm()}"/></td>
                            <td><c:out value="${r.description()}"/></td>
                            <td class="text-center">${r.adminCnt()}</td>
                            <td class="text-center">${r.useYn() == 'Y' ? '사용' : '사용 안 함'}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty roles}"><tr><td colspan="5" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </jsp:body>
</ui:layout>
