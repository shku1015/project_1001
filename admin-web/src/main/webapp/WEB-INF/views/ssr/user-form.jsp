<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-USR-03 회원 등록·수정 (③ JSP SSR). user가 있으면 수정. 개인정보를 원문으로 다룬다 (CREATE·UPDATE + PRIVACY).
     소속 기업은 정상 기업 선택 상자(기업 회원만). ② jsp/user-form.jsp, ① UserFormPage.tsx와 같은 마크업 --%>
<c:set var="isEdit" value="${user != null}"/>
<c:set var="u" value="${user}"/>
<c:set var="userType" value="${isEdit ? u.userTypeCd() : (empty param.userTypeCd ? 'PERSONAL' : param.userTypeCd)}"/>
<c:set var="companyId" value="${not empty param.companyId ? param.companyId : (isEdit ? u.companyId() : '')}"/>
<ui:layout title="사용자관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script>
          (function () {
            // 기업 회원일 때만 소속 정보를 보인다
            document.querySelectorAll('input[name="userTypeCd"]').forEach(function (r) {
              r.addEventListener('change', function () {
                document.getElementById('corporate-fields').hidden = r.value !== 'CORPORATE';
              });
            });
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="card" id="user-form-card">
            <div class="card-header"><h3 class="card-title" id="user-form-title">${isEdit ? '회원 수정' : '회원 등록'}</h3></div>
            <c:set var="formAction" value="/ssr/users"/>
            <c:if test="${isEdit}"><c:set var="formAction" value="/ssr/users/${u.userId()}/update"/></c:if>
            <form method="post" action="<c:url value='${formAction}'/>" id="user-form" novalidate>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <c:if test="${isEdit}"><input type="hidden" name="modDt" value="${u.modDt()}"></c:if>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label${isEdit ? '' : ' required'}">회원 구분</label>
                            <c:choose>
                                <c:when test="${isEdit}"><div class="form-control-plaintext" id="user-type">${userType == 'CORPORATE' ? '기업' : '개인'}</div></c:when>
                                <c:otherwise>
                                    <div>
                                        <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="userTypeCd" value="PERSONAL" id="user-type-PERSONAL" ${userType == 'PERSONAL' ? 'checked' : ''}> 개인</label>
                                        <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="userTypeCd" value="CORPORATE" id="user-type-CORPORATE" ${userType == 'CORPORATE' ? 'checked' : ''}> 기업</label>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label${isEdit ? '' : ' required'}" for="user-loginId">로그인 아이디</label>
                            <c:choose>
                                <c:when test="${isEdit}"><input class="form-control" id="user-loginId" readonly value="<c:out value='${u.loginId()}'/>"></c:when>
                                <c:otherwise><input class="form-control" id="user-loginId" name="loginId" maxlength="50" required placeholder="영문 소문자·숫자 4~50자" value="<c:out value='${param.loginId}'/>"></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="user-userNm">이름</label>
                            <input class="form-control" id="user-userNm" name="userNm" maxlength="50" required
                                   value="<c:out value='${not empty param.userNm ? param.userNm : (isEdit ? u.userNm() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="user-email">이메일</label>
                            <input class="form-control" type="email" id="user-email" name="email" maxlength="100" required
                                   value="<c:out value='${not empty param.email ? param.email : (isEdit ? u.email() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="user-mobileNo">휴대폰 번호</label>
                            <input class="form-control" id="user-mobileNo" name="mobileNo" maxlength="11" inputmode="numeric" placeholder="숫자만 10~11자리"
                                   value="<c:out value='${not empty param.mobileNo ? param.mobileNo : (isEdit ? u.mobileNo() : "")}'/>">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="user-birthDate">생년월일</label>
                            <input class="form-control" type="date" id="user-birthDate" name="birthDate"
                                   value="<c:out value='${not empty param.birthDate ? param.birthDate : (isEdit ? u.birthDate() : "")}'/>">
                        </div>
                    </div>
                    <div class="row" id="corporate-fields" ${userType == 'CORPORATE' ? '' : 'hidden'}>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="user-companyId">소속 기업</label>
                            <select class="form-select" id="user-companyId" name="companyId">
                                <option value="">선택 (정상 기업)</option>
                                <c:set var="listed" value="false"/>
                                <c:forEach var="c" items="${companies}">
                                    <c:if test="${c.companyId() == companyId}"><c:set var="listed" value="true"/></c:if>
                                    <option value="${c.companyId()}" ${c.companyId() == companyId ? 'selected' : ''}><c:out value="${c.companyNm()}"/></option>
                                </c:forEach>
                                <c:if test="${isEdit and not listed and u.companyId() != null}"><option value="${u.companyId()}" selected><c:out value="${u.companyNm()}"/> (현재)</option></c:if>
                            </select>
                        </div>
                        <div class="col-md-3 mb-3">
                            <label class="form-label" for="user-deptNm">부서</label>
                            <input class="form-control" id="user-deptNm" name="deptNm" maxlength="100"
                                   value="<c:out value='${not empty param.deptNm ? param.deptNm : (isEdit ? u.deptNm() : "")}'/>">
                        </div>
                        <div class="col-md-3 mb-3">
                            <label class="form-label" for="user-positionNm">직위</label>
                            <input class="form-control" id="user-positionNm" name="positionNm" maxlength="50"
                                   value="<c:out value='${not empty param.positionNm ? param.positionNm : (isEdit ? u.positionNm() : "")}'/>">
                        </div>
                    </div>
                    <c:if test="${not isEdit}"><small class="form-hint d-block mb-2">비밀번호는 입력하지 않습니다. 저장하면 임시 비밀번호가 한 번 표시됩니다.</small></c:if>
                    <div id="user-message"><c:if test="${not empty formError}"><div class="alert alert-danger" role="alert"><c:out value="${formError}"/></div></c:if></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <c:set var="cancelUrl" value="/ssr/users"/>
                    <c:if test="${isEdit}"><c:set var="cancelUrl" value="/ssr/users/${u.userId()}"/></c:if>
                    <a class="btn" id="btn-cancel" href="<c:url value='${cancelUrl}'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
