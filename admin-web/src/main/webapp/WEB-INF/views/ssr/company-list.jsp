<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COM-01 기업 목록 (③ JSP SSR). ② jsp/company-list.jsp(admin-company-api.js), ① CompanyListPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="path" value="/ssr/companies"/>
<ui:layout title="기업정보관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">기업 목록</h3>
                <div class="card-actions btn-list">
                    <c:choose>
                        <c:when test="${canExcel}">
                            <c:url var="excelUrl" value="/ssr/companies/excel"><c:forEach var="p" items="${paramValues}"><c:if test="${p.key != 'page' and p.key != 'size'}"><c:forEach var="v" items="${p.value}"><c:param name="${p.key}" value="${v}"/></c:forEach></c:if></c:forEach></c:url>
                            <a class="btn btn-sm" id="btn-excel" href="${excelUrl}">엑셀</a>
                        </c:when>
                        <c:otherwise><button type="button" class="btn btn-sm" id="btn-excel" disabled title="${noPerm}">엑셀</button></c:otherwise>
                    </c:choose>
                    <c:choose>
                        <c:when test="${canCreate}"><a class="btn btn-primary btn-sm" id="btn-company-create" href="<c:url value='/ssr/companies/new'/>">등록</a></c:when>
                        <c:otherwise><button type="button" class="btn btn-primary btn-sm" id="btn-company-create" disabled title="${noPerm}">등록</button></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form method="get" action="<c:url value='${path}'/>" class="row g-2" id="company-search">
                    <c:if test="${not empty param.size}"><input type="hidden" name="size" value="<c:out value='${param.size}'/>"></c:if>
                    <div class="col-md-2"><input class="form-control" id="search-companyNm" name="companyNm" placeholder="기업명" aria-label="기업명" value="<c:out value='${param.companyNm}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-bizRegNo" name="bizRegNo" placeholder="사업자등록번호 (숫자)" aria-label="사업자등록번호" inputmode="numeric" value="<c:out value='${param.bizRegNo}'/>"></div>
                    <div class="col-md-2"><input class="form-control" id="search-ceoNm" name="ceoNm" placeholder="대표자명" aria-label="대표자명" value="<c:out value='${param.ceoNm}'/>"></div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-statusCd" name="statusCd" aria-label="상태">
                            <option value="">상태 전체</option>
                            <option value="ACTIVE" ${param.statusCd == 'ACTIVE' ? 'selected' : ''}>정상</option>
                            <option value="SUSPENDED" ${param.statusCd == 'SUSPENDED' ? 'selected' : ''}>정지</option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <div class="date-range">
                            <div class="input-group">
                                <input type="date" class="form-control" id="search-regDtFrom" name="regDtFrom" aria-label="등록일 시작" data-range-from value="<c:out value='${param.regDtFrom}'/>">
                                <span class="input-group-text">~</span>
                                <input type="date" class="form-control" id="search-regDtTo" name="regDtTo" aria-label="등록일 끝" data-range-to value="<c:out value='${param.regDtTo}'/>">
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
                <table class="table table-vcenter" id="company-table">
                    <thead>
                    <tr>
                        <th>번호</th>
                        <ui:sortTh path="${path}" field="companyNm" label="기업명" current="${sort}"/>
                        <th>사업자등록번호</th><th>대표자</th>
                        <ui:sortTh path="${path}" field="memberCnt" label="소속 회원 수" current="${sort}"/>
                        <th>상태</th>
                        <ui:sortTh path="${path}" field="regDt" label="등록일시" current="${sort}"/>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="co" items="${result.items()}" varStatus="st">
                        <tr>
                            <td>${result.totalCount() - (result.page() - 1) * result.size() - st.index}</td>
                            <td><a href="<c:url value='/ssr/companies/${co.companyId()}'/>"><c:out value="${co.companyNm()}"/></a></td>
                            <td>${co.bizRegNo().substring(0, 3)}-${co.bizRegNo().substring(3, 5)}-${co.bizRegNo().substring(5)}</td>
                            <td><c:out value="${co.ceoNm()}"/></td>
                            <td>${co.memberCnt()}</td>
                            <td><c:out value="${co.statusNm()}"/></td>
                            <td>${co.regDt().format(dtMin)}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty result.items()}"><tr><td colspan="7" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
            <ui:pager path="${path}" page="${result.page()}" size="${result.size()}" totalPages="${result.totalPages()}" totalCount="${result.totalCount()}"/>
        </div>
    </jsp:body>
</ui:layout>
