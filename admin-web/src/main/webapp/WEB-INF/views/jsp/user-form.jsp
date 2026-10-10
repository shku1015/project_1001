<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-USR-03 회원 등록·수정 (② JSP + API). userId가 있으면 수정. 마크업은 ③ ssr/user-form.jsp와 같다 --%>
<ui:layout title="사용자관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script src="<c:url value='/common/js/admin-user-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="user-page" data-page="form" data-user-id="${userId}" hidden></div>
        <div class="card" id="user-form-card">
            <div class="card-header"><h3 class="card-title" id="user-form-title">${userId == null ? '회원 등록' : '회원 수정'}</h3></div>
            <form id="user-form" novalidate>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label${userId == null ? ' required' : ''}">회원 구분</label>
                            <div class="form-control-plaintext" id="user-type" hidden></div>
                            <div id="user-type-choice">
                                <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="userTypeCd" value="PERSONAL" id="user-type-PERSONAL" checked> 개인</label>
                                <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="userTypeCd" value="CORPORATE" id="user-type-CORPORATE"> 기업</label>
                            </div>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label${userId == null ? ' required' : ''}" for="user-loginId">로그인 아이디</label>
                            <input class="form-control" id="user-loginId" name="loginId" maxlength="50" required placeholder="영문 소문자·숫자 4~50자" ${userId == null ? '' : 'readonly'}>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="user-userNm">이름</label>
                            <input class="form-control" id="user-userNm" name="userNm" maxlength="50" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="user-email">이메일</label>
                            <input class="form-control" type="email" id="user-email" name="email" maxlength="100" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="user-mobileNo">휴대폰 번호</label>
                            <input class="form-control" id="user-mobileNo" name="mobileNo" maxlength="11" inputmode="numeric" placeholder="숫자만 10~11자리">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label" for="user-birthDate">생년월일</label>
                            <input class="form-control" type="date" id="user-birthDate" name="birthDate">
                        </div>
                    </div>
                    <div class="row" id="corporate-fields" hidden>
                        <div class="col-md-6 mb-3">
                            <label class="form-label required" for="user-companyId">소속 기업</label>
                            <select class="form-select" id="user-companyId" name="companyId">
                                <option value="">선택 (정상 기업)</option>
                            </select>
                        </div>
                        <div class="col-md-3 mb-3">
                            <label class="form-label" for="user-deptNm">부서</label>
                            <input class="form-control" id="user-deptNm" name="deptNm" maxlength="100">
                        </div>
                        <div class="col-md-3 mb-3">
                            <label class="form-label" for="user-positionNm">직위</label>
                            <input class="form-control" id="user-positionNm" name="positionNm" maxlength="50">
                        </div>
                    </div>
                    <c:if test="${userId == null}"><small class="form-hint d-block mb-2">비밀번호는 입력하지 않습니다. 저장하면 임시 비밀번호가 한 번 표시됩니다.</small></c:if>
                    <div id="user-message"></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <a class="btn" id="btn-cancel" href="<c:url value='/jsp/users'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
