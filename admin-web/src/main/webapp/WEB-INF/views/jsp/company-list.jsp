<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COM-01 기업 목록 (② JSP + API). admin-company-api.js가 /api/v1/companies 로 채운다. 마크업은 ③ ssr/company-list.jsp와 같다 --%>
<ui:layout title="기업정보관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script src="<c:url value='/common/js/admin-company-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="company-page" data-page="list" hidden></div>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">기업 목록</h3>
                <div class="card-actions btn-list">
                    <button type="button" class="btn btn-sm" id="btn-excel" disabled>엑셀</button>
                    <a class="btn btn-primary btn-sm disabled" id="btn-company-create" href="<c:url value='/jsp/companies/new'/>" aria-disabled="true">등록</a>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form class="row g-2" id="company-search">
                    <div class="col-md-2"><input class="form-control" id="search-companyNm" name="companyNm" placeholder="기업명" aria-label="기업명"></div>
                    <div class="col-md-2"><input class="form-control" id="search-bizRegNo" name="bizRegNo" placeholder="사업자등록번호 (숫자)" aria-label="사업자등록번호" inputmode="numeric"></div>
                    <div class="col-md-2"><input class="form-control" id="search-ceoNm" name="ceoNm" placeholder="대표자명" aria-label="대표자명"></div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-statusCd" name="statusCd" aria-label="상태">
                            <option value="">상태 전체</option>
                            <option value="ACTIVE">정상</option>
                            <option value="SUSPENDED">정지</option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <div class="date-range">
                            <div class="input-group">
                                <input type="date" class="form-control" id="search-regDtFrom" name="regDtFrom" aria-label="등록일 시작" data-range-from>
                                <span class="input-group-text">~</span>
                                <input type="date" class="form-control" id="search-regDtTo" name="regDtTo" aria-label="등록일 끝" data-range-to>
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
                        <a class="btn" id="btn-search-reset" href="<c:url value='/jsp/companies'/>">초기화</a>
                    </div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="company-table">
                    <thead><tr id="company-thead"></tr></thead>
                    <tbody id="company-tbody"></tbody>
                </table>
            </div>
            <div id="pager"></div>
        </div>
    </jsp:body>
</ui:layout>
