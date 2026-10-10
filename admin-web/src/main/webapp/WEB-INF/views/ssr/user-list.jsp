<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-USR-01 회원 목록 (③ JSP SSR). 개인정보는 화면 마스킹 설정대로 가려서 보여 준다.
     ② jsp/user-list.jsp(admin-user-api.js), ① UserListPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="path" value="/ssr/users"/>
<ui:layout title="사용자관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">회원 목록</h3>
                <div class="card-actions btn-list">
                    <c:choose>
                        <c:when test="${canExcel}">
                            <c:url var="excelUrl" value="/ssr/users/excel"><c:forEach var="p" items="${paramValues}"><c:if test="${p.key != 'page' and p.key != 'size'}"><c:forEach var="v" items="${p.value}"><c:param name="${p.key}" value="${v}"/></c:forEach></c:if></c:forEach></c:url>
                            <a class="btn btn-sm" id="btn-excel" href="${excelUrl}">엑셀</a>
                        </c:when>
                        <c:otherwise><button type="button" class="btn btn-sm" id="btn-excel" disabled title="${noPerm}">엑셀</button></c:otherwise>
                    </c:choose>
                    <c:choose>
                        <c:when test="${canCreate}"><a class="btn btn-primary btn-sm" id="btn-user-create" href="<c:url value='/ssr/users/new'/>">등록</a></c:when>
                        <c:otherwise><button type="button" class="btn btn-primary btn-sm" id="btn-user-create" disabled title="${noPerm}">등록</button></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form method="get" action="<c:url value='${path}'/>" class="row g-2" id="user-search">
                    <c:if test="${not empty param.size}"><input type="hidden" name="size" value="<c:out value='${param.size}'/>"></c:if>
                    <div class="col-md-2">
                        <select class="form-select" id="search-userTypeCd" name="userTypeCd" aria-label="회원 구분">
                            <option value="">구분 전체</option>
                            <option value="PERSONAL" ${param.userTypeCd == 'PERSONAL' ? 'selected' : ''}>개인</option>
                            <option value="CORPORATE" ${param.userTypeCd == 'CORPORATE' ? 'selected' : ''}>기업</option>
                        </select>
                    </div>
                    <div class="col-md-2"><input class="form-control" id="search-loginId" name="loginId" placeholder="로그인 아이디" aria-label="로그인 아이디" value="<c:out value='${param.loginId}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-userNm" name="userNm" placeholder="이름" aria-label="이름" value="<c:out value='${param.userNm}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-email" name="email" placeholder="이메일" aria-label="이메일" value="<c:out value='${param.email}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-mobileNo" name="mobileNo" placeholder="휴대폰 번호 (숫자)" aria-label="휴대폰 번호" inputmode="numeric" value="<c:out value='${param.mobileNo}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-companyNm" name="companyNm" placeholder="소속 기업" aria-label="소속 기업" value="<c:out value='${param.companyNm}'/>"></div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-statusCd" name="statusCd" aria-label="상태">
                            <option value="">상태 전체</option>
                            <option value="ACTIVE" ${param.statusCd == 'ACTIVE' ? 'selected' : ''}>정상</option>
                            <option value="DORMANT" ${param.statusCd == 'DORMANT' ? 'selected' : ''}>휴면</option>
                            <option value="SUSPENDED" ${param.statusCd == 'SUSPENDED' ? 'selected' : ''}>정지</option>
                            <option value="WITHDRAWN" ${param.statusCd == 'WITHDRAWN' ? 'selected' : ''}>탈퇴</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-joinPath" name="joinPath" aria-label="가입 경로">
                            <option value="">가입 경로 전체</option>
                            <option value="USER_SERVICE" ${param.joinPath == 'USER_SERVICE' ? 'selected' : ''}>사용자 서비스</option>
                            <option value="ADMIN" ${param.joinPath == 'ADMIN' ? 'selected' : ''}>관리자 등록</option>
                        </select>
                    </div>
                    <c:if test="${not empty param.companyId}"><input type="hidden" name="companyId" value="<c:out value='${param.companyId}'/>"></c:if>
                    <div class="col-md-4">
                        <div class="date-range">
                            <div class="input-group">
                                <input type="date" class="form-control" id="search-joinDtFrom" name="joinDtFrom" aria-label="가입일 시작" data-range-from value="<c:out value='${param.joinDtFrom}'/>">
                                <span class="input-group-text">~</span>
                                <input type="date" class="form-control" id="search-joinDtTo" name="joinDtTo" aria-label="가입일 끝" data-range-to value="<c:out value='${param.joinDtTo}'/>">
                            </div>
                            <div class="btn-list mt-1">
                                <button type="button" class="btn btn-sm date-quick" data-range="today">오늘</button>
                                <button type="button" class="btn btn-sm date-quick" data-range="1w">1주일</button>
                                <button type="button" class="btn btn-sm date-quick" data-range="1m">1개월</button>
                                <button type="button" class="btn btn-sm date-quick" data-range="3m">3개월</button>
                            </div>
                        </div>
                    </div>
                    <div class="col-12 d-flex gap-2 justify-content-end">
                        <button type="submit" class="btn btn-primary">검색</button>
                        <a class="btn" id="btn-search-reset" href="<c:url value='${path}'/>">초기화</a>
                    </div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="user-table">
                    <thead>
                    <tr>
                        <th>번호</th><th>구분</th>
                        <ui:sortTh path="${path}" field="loginId" label="로그인 아이디" current="${sort}"/>
                        <ui:sortTh path="${path}" field="userNm" label="이름" current="${sort}"/>
                        <th>이메일</th><th>휴대폰 번호</th><th>소속 기업</th><th>상태</th>
                        <ui:sortTh path="${path}" field="joinDt" label="가입일시" current="${sort}"/>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="u" items="${result.items()}" varStatus="st">
                        <tr>
                            <td>${result.totalCount() - (result.page() - 1) * result.size() - st.index}</td>
                            <td><c:out value="${u.userTypeNm()}"/></td>
                            <td><a href="<c:url value='/ssr/users/${u.userId()}'/>"><c:out value="${u.loginId()}"/></a></td>
                            <td><c:out value="${u.userNm()}"/></td>
                            <td><c:out value="${u.email()}"/></td>
                            <td><ui:mobile value="${u.mobileNo()}"/></td>
                            <td><c:out value="${empty u.companyNm() ? '-' : u.companyNm()}"/></td>
                            <td><c:out value="${u.statusNm()}"/></td>
                            <td>${u.joinDt().format(dtMin)}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty result.items()}"><tr><td colspan="9" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
            <ui:pager path="${path}" page="${result.page()}" size="${result.size()}" totalPages="${result.totalPages()}" totalCount="${result.totalCount()}"/>
        </div>
    </jsp:body>
</ui:layout>
