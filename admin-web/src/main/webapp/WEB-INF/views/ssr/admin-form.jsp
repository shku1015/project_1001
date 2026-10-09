<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ADM-03 관리자 등록·수정 (③ JSP SSR). admin이 있으면 수정. 역할은 등록 때만 고른다 (수정은 상세 화면에서).
     ② jsp/admin-form.jsp, ① AdminFormPage.tsx와 같은 마크업 --%>
<c:set var="isEdit" value="${admin != null}"/>
<ui:layout title="관리자" mode="ssr">
    <jsp:body>
        <div class="card" id="admin-form-card">
            <div class="card-header"><h3 class="card-title" id="admin-form-title">${isEdit ? '관리자 수정' : '관리자 등록'}</h3></div>
            <c:set var="formAction" value="/ssr/admins"/>
            <c:if test="${isEdit}"><c:set var="formAction" value="/ssr/admins/${admin.adminId()}/update"/></c:if>
            <form method="post" action="<c:url value='${formAction}'/>" id="admin-form" novalidate>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <c:if test="${isEdit}"><input type="hidden" name="modDt" value="${admin.modDt()}"></c:if>
                <div class="card-body">
                    <div class="mb-3">
                        <label class="form-label${isEdit ? '' : ' required'}" for="admin-loginId">로그인 아이디</label>
                        <c:choose>
                            <c:when test="${isEdit}"><input class="form-control" id="admin-loginId" readonly value="<c:out value='${admin.loginId()}'/>"></c:when>
                            <c:otherwise><input class="form-control" id="admin-loginId" name="loginId" maxlength="50" required placeholder="영문 소문자·숫자 4~50자"
                                                value="<c:out value='${param.loginId}'/>"></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="admin-adminNm">이름</label>
                        <input class="form-control" id="admin-adminNm" name="adminNm" maxlength="50" required
                               value="<c:out value='${not empty param.adminNm ? param.adminNm : (isEdit ? admin.adminNm() : "")}'/>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="admin-email">이메일</label>
                        <input class="form-control" type="email" id="admin-email" name="email" maxlength="100" required
                               value="<c:out value='${not empty param.email ? param.email : (isEdit ? admin.email() : "")}'/>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="admin-mobileNo">휴대폰 번호</label>
                        <input class="form-control" id="admin-mobileNo" name="mobileNo" maxlength="11" placeholder="숫자만 10~11자리"
                               value="<c:out value='${not empty param.mobileNo ? param.mobileNo : (isEdit ? admin.mobileNo() : "")}'/>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="admin-deptNm">부서</label>
                        <input class="form-control" id="admin-deptNm" name="deptNm" maxlength="100"
                               value="<c:out value='${not empty param.deptNm ? param.deptNm : (isEdit ? admin.deptNm() : "")}'/>">
                    </div>
                    <c:if test="${not isEdit}">
                        <div class="mb-3">
                            <label class="form-label required">역할</label>
                            <div id="admin-roles-choice">
                                <c:forEach var="r" items="${roleChoices}">
                                    <c:set var="picked" value="false"/>
                                    <c:forEach var="v" items="${paramValues.roleIds}"><c:if test="${v == r.roleId()}"><c:set var="picked" value="true"/></c:if></c:forEach>
                                    <label class="form-check form-check-inline"><input class="form-check-input" type="checkbox" name="roleIds" value="${r.roleId()}"
                                           id="role-${r.roleId()}" ${picked ? 'checked' : ''}> <c:out value="${r.roleNm()}"/></label>
                                </c:forEach>
                            </div>
                            <small class="form-hint">내 권한 범위 안의 사용 중인 역할만 고를 수 있습니다. 등록 후 임시 비밀번호가 한 번 표시됩니다.</small>
                        </div>
                    </c:if>
                    <div id="admin-message"><c:if test="${not empty formError}"><div class="alert alert-danger" role="alert"><c:out value="${formError}"/></div></c:if></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <c:set var="cancelUrl" value="/ssr/admins"/>
                    <c:if test="${isEdit}"><c:set var="cancelUrl" value="/ssr/admins/${admin.adminId()}"/></c:if>
                    <a class="btn" id="btn-cancel" href="<c:url value='${cancelUrl}'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
