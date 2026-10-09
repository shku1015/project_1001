<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ADM-03 관리자 등록·수정 (② JSP + API). adminId가 있으면 수정. 마크업은 ③ ssr/admin-form.jsp와 같다 --%>
<ui:layout title="관리자" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-admin-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="admin-page" data-page="form" data-admin-id="${adminId}" hidden></div>
        <div class="card" id="admin-form-card">
            <div class="card-header"><h3 class="card-title" id="admin-form-title">${adminId == null ? '관리자 등록' : '관리자 수정'}</h3></div>
            <form id="admin-form" novalidate>
                <div class="card-body">
                    <div class="mb-3">
                        <label class="form-label${adminId == null ? ' required' : ''}" for="admin-loginId">로그인 아이디</label>
                        <input class="form-control" id="admin-loginId" name="loginId" maxlength="50" required placeholder="영문 소문자·숫자 4~50자" ${adminId == null ? '' : 'readonly'}>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="admin-adminNm">이름</label>
                        <input class="form-control" id="admin-adminNm" name="adminNm" maxlength="50" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="admin-email">이메일</label>
                        <input class="form-control" type="email" id="admin-email" name="email" maxlength="100" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="admin-mobileNo">휴대폰 번호</label>
                        <input class="form-control" id="admin-mobileNo" name="mobileNo" maxlength="11" placeholder="숫자만 10~11자리">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="admin-deptNm">부서</label>
                        <input class="form-control" id="admin-deptNm" name="deptNm" maxlength="100">
                    </div>
                    <c:if test="${adminId == null}">
                        <div class="mb-3">
                            <label class="form-label required">역할</label>
                            <div id="admin-roles-choice"></div>
                            <small class="form-hint">내 권한 범위 안의 사용 중인 역할만 고를 수 있습니다. 등록 후 임시 비밀번호가 한 번 표시됩니다.</small>
                        </div>
                    </c:if>
                    <div id="admin-message"></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <a class="btn" id="btn-cancel" href="<c:url value='/jsp/admins'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
