<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-PRM-02 관리자별 최종 권한 (③ JSP SSR). 위쪽 관리자 선택(CMP-12), 아래쪽 메뉴 × 액션 표 (조회 전용).
     ② jsp/permission-admin.jsp(admin-permission-admin-api.js), ① PermissionAdminPage.tsx와 같은 마크업 --%>
<ui:layout title="권한" mode="ssr">
    <jsp:body>
        <div class="mb-3"><a class="btn btn-sm" id="btn-back" href="<c:url value='/ssr/permissions'/>">메뉴 기준 권한관리</a></div>

        <div class="card mb-3" id="admin-picker">
            <div class="card-header"><h3 class="card-title">관리자 선택</h3></div>
            <div class="card-body">
                <form method="get" action="<c:url value='/ssr/permissions/admins'/>" class="d-flex gap-2" id="picker-form">
                    <input class="form-control" id="picker-keyword" name="keyword" placeholder="이름 또는 로그인 아이디" aria-label="이름 또는 로그인 아이디"
                           value="<c:out value='${param.keyword}'/>">
                    <button type="submit" class="btn btn-primary text-nowrap">검색</button>
                </form>
            </div>
            <c:if test="${candidates != null}">
                <div class="table-responsive">
                    <table class="table table-vcenter mb-0" id="picker-table">
                        <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서</th><th>상태</th><th></th></tr></thead>
                        <tbody>
                        <c:forEach var="a" items="${candidates.items()}">
                            <tr>
                                <td><c:out value="${a.loginId()}"/></td><td><c:out value="${a.adminNm()}"/></td>
                                <td><c:out value="${empty a.deptNm() ? '-' : a.deptNm()}"/></td><td><c:out value="${a.statusNm()}"/></td>
                                <td class="text-end"><a class="btn btn-sm btn-pick" href="<c:url value='/ssr/permissions/admins'><c:param name='keyword' value='${param.keyword}'/><c:param name='adminId' value='${a.adminId()}'/></c:url>">선택</a></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty candidates.items()}"><tr><td colspan="5" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </c:if>
        </div>

        <c:if test="${effective != null}">
            <div class="card" id="effective-card">
                <div class="card-header">
                    <h3 class="card-title" id="effective-title"><c:out value="${effective.admin().adminNm()}"/>
                        <span class="text-secondary ms-1">(<c:out value="${effective.admin().loginId()}"/>)</span>의 최종 권한</h3>
                </div>
                <c:choose>
                    <c:when test="${effective.superAdmin()}">
                        <div class="card-body"><div class="alert alert-info mb-0" role="note">슈퍼관리자는 모든 권한을 가집니다.</div></div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-vcenter table-sm mb-0" id="effective-table">
                                <thead><tr><th>메뉴</th><th class="text-center">조회</th><th class="text-center">등록</th><th class="text-center">수정</th>
                                    <th class="text-center">삭제</th><th class="text-center">엑셀</th><th class="text-center">개인정보</th></tr></thead>
                                <tbody>
                                <c:forEach var="m" items="${effective.menus()}">
                                    <c:set var="folder" value="${m.menuTypeCd() == 'FOLDER'}"/>
                                    <tr data-menu-id="${m.menuId()}">
                                        <td class="perm-depth-${m.depth()}${folder ? ' fw-bold' : ''}"><c:out value="${m.menuNm()}"/></td>
                                        <c:forEach var="a" items="${allActions}">
                                            <td class="text-center" data-action="${a}">
                                                <c:if test="${m.actions().contains(a)}">
                                                    <c:choose>
                                                        <c:when test="${m.granted().containsKey(a)}"><c:set var="givers" value=""/><c:forEach var="g" items="${m.granted().get(a)}" varStatus="gs"><c:set var="givers" value="${givers}${g}${gs.last ? '' : ', '}"/></c:forEach><span class="perm-granted" title="<c:out value='${givers}'/>">●</span></c:when>
                                                        <c:otherwise><span class="text-secondary">-</span></c:otherwise>
                                                    </c:choose>
                                                </c:if>
                                            </td>
                                        </c:forEach>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>
    </jsp:body>
</ui:layout>
