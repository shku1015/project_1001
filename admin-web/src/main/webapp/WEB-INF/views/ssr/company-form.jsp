<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COM-03 기업 등록·수정 (③ JSP SSR). company가 있으면 수정. 우편번호·주소는 우편번호 검색(CMP-16)으로만 채운다.
     ② jsp/company-form.jsp, ① CompanyFormPage.tsx와 같은 마크업 --%>
<c:set var="isEdit" value="${company != null}"/>
<c:set var="co" value="${company}"/>
<ui:layout title="기업정보관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script>
          (function () {
            var form = document.getElementById('company-form');
            document.getElementById('btn-postcode').addEventListener('click', function () {
              AdminFormKit.openPostcode(form.zipCd, form.addr, form.addrDtl).catch(function (e) { alert(e.message); });
            });
            // 입력 중에 취소하면 확인한다
            var changed = false;
            form.addEventListener('input', function () { changed = true; });
            document.getElementById('btn-cancel').addEventListener('click', function (e) {
              if (changed && !confirm('입력한 내용이 사라집니다. 취소하시겠습니까?')) { e.preventDefault(); }
            });
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="card" id="company-form-card">
            <div class="card-header"><h3 class="card-title" id="company-form-title">${isEdit ? '기업 수정' : '기업 등록'}</h3></div>
            <c:set var="formAction" value="/ssr/companies"/>
            <c:if test="${isEdit}"><c:set var="formAction" value="/ssr/companies/${co.companyId()}/update"/></c:if>
            <form method="post" action="<c:url value='${formAction}'/>" id="company-form" novalidate>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <c:if test="${isEdit}"><input type="hidden" name="modDt" value="${co.modDt()}"></c:if>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="company-companyNm">기업명</label>
                            <input class="form-control" id="company-companyNm" name="companyNm" maxlength="100" required
                                   value="<c:out value='${not empty param.companyNm ? param.companyNm : (isEdit ? co.companyNm() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label${isEdit ? '' : ' required'}" for="company-bizRegNo">사업자등록번호</label>
                            <c:choose>
                                <c:when test="${isEdit}"><input class="form-control" id="company-bizRegNo" readonly value="${co.bizRegNo()}"></c:when>
                                <c:otherwise><input class="form-control" id="company-bizRegNo" name="bizRegNo" maxlength="10" inputmode="numeric" required placeholder="숫자 10자리"
                                                    value="<c:out value='${param.bizRegNo}'/>"></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="company-ceoNm">대표자명</label>
                            <input class="form-control" id="company-ceoNm" name="ceoNm" maxlength="50" required
                                   value="<c:out value='${not empty param.ceoNm ? param.ceoNm : (isEdit ? co.ceoNm() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="company-telNo">대표 전화번호</label>
                            <input class="form-control" id="company-telNo" name="telNo" maxlength="11" inputmode="numeric" placeholder="숫자만 9~11자리"
                                   value="<c:out value='${not empty param.telNo ? param.telNo : (isEdit ? co.telNo() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="company-bizType">업태</label>
                            <input class="form-control" id="company-bizType" name="bizType" maxlength="100"
                                   value="<c:out value='${not empty param.bizType ? param.bizType : (isEdit ? co.bizType() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="company-bizItem">종목</label>
                            <input class="form-control" id="company-bizItem" name="bizItem" maxlength="100"
                                   value="<c:out value='${not empty param.bizItem ? param.bizItem : (isEdit ? co.bizItem() : "")}'/>">
                        </div>
                        <div class="col-12 mb-3">
                            <label class="form-label" for="company-zipCd">주소</label>
                            <div class="input-group mb-2">
                                <input class="form-control" id="company-zipCd" name="zipCd" readonly placeholder="우편번호" aria-label="우편번호"
                                       value="<c:out value='${not empty param.zipCd ? param.zipCd : (isEdit ? co.zipCd() : "")}'/>">
                                <button type="button" class="btn" id="btn-postcode">우편번호 검색</button>
                            </div>
                            <input class="form-control mb-2" id="company-addr" name="addr" readonly placeholder="주소" aria-label="주소"
                                   value="<c:out value='${not empty param.addr ? param.addr : (isEdit ? co.addr() : "")}'/>">
                            <input class="form-control" id="company-addrDtl" name="addrDtl" maxlength="200" placeholder="상세주소" aria-label="상세주소"
                                   value="<c:out value='${not empty param.addrDtl ? param.addrDtl : (isEdit ? co.addrDtl() : "")}'/>">
                        </div>
                    </div>
                    <div id="company-message"><c:if test="${not empty formError}"><div class="alert alert-danger" role="alert"><c:out value="${formError}"/></div></c:if></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <c:set var="cancelUrl" value="/ssr/companies"/>
                    <c:if test="${isEdit}"><c:set var="cancelUrl" value="/ssr/companies/${co.companyId()}"/></c:if>
                    <a class="btn" id="btn-cancel" href="<c:url value='${cancelUrl}'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
