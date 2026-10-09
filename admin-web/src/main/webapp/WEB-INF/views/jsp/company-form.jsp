<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COM-03 기업 등록·수정 (② JSP + API). companyId가 있으면 수정. 마크업은 ③ ssr/company-form.jsp와 같다 --%>
<ui:layout title="기업정보관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script src="<c:url value='/common/js/admin-company-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="company-page" data-page="form" data-company-id="${companyId}" hidden></div>
        <div class="card" id="company-form-card">
            <div class="card-header"><h3 class="card-title" id="company-form-title">${companyId == null ? '기업 등록' : '기업 수정'}</h3></div>
            <form id="company-form" novalidate>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="company-companyNm">기업명</label>
                            <input class="form-control" id="company-companyNm" name="companyNm" maxlength="100" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label${companyId == null ? ' required' : ''}" for="company-bizRegNo">사업자등록번호</label>
                            <input class="form-control" id="company-bizRegNo" name="bizRegNo" maxlength="10" inputmode="numeric" required placeholder="숫자 10자리" ${companyId == null ? '' : 'readonly'}>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="company-ceoNm">대표자명</label>
                            <input class="form-control" id="company-ceoNm" name="ceoNm" maxlength="50" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="company-telNo">대표 전화번호</label>
                            <input class="form-control" id="company-telNo" name="telNo" maxlength="11" inputmode="numeric" placeholder="숫자만 9~11자리">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="company-bizType">업태</label>
                            <input class="form-control" id="company-bizType" name="bizType" maxlength="100">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="company-bizItem">종목</label>
                            <input class="form-control" id="company-bizItem" name="bizItem" maxlength="100">
                        </div>
                        <div class="col-12 mb-3">
                            <label class="form-label" for="company-zipCd">주소</label>
                            <div class="input-group mb-2">
                                <input class="form-control" id="company-zipCd" name="zipCd" readonly placeholder="우편번호" aria-label="우편번호">
                                <button type="button" class="btn" id="btn-postcode">우편번호 검색</button>
                            </div>
                            <input class="form-control mb-2" id="company-addr" name="addr" readonly placeholder="주소" aria-label="주소">
                            <input class="form-control" id="company-addrDtl" name="addrDtl" maxlength="200" placeholder="상세주소" aria-label="상세주소">
                        </div>
                    </div>
                    <div id="company-message"></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <a class="btn" id="btn-cancel" href="<c:url value='/jsp/companies'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
