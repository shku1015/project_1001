<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-MY-01 내 정보 (③ JSP SSR). 입력 오류가 나면 입력한 값을 그대로 다시 보여 준다 --%>
<ui:layout title="내 정보" mode="ssr">
    <div class="card">
        <div class="card-body">
            <form method="post" action="<c:url value='/ssr/me'/>">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <input type="hidden" name="modDt" value="${me.modDt()}">
                <div class="mb-3">
                    <label class="form-label" for="loginId">로그인 아이디</label>
                    <input class="form-control" type="text" id="loginId" value="<c:out value='${me.loginId()}'/>" readonly>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="roles">역할</label>
                    <input class="form-control" type="text" id="roles" readonly
                           value="<c:forEach var='r' items='${me.roles()}' varStatus='s'><c:out value='${r.roleNm()}'/><c:if test='${!s.last}'>, </c:if></c:forEach>">
                </div>
                <div class="mb-3">
                    <label class="form-label required" for="adminNm">이름</label>
                    <input class="form-control" type="text" id="adminNm" name="adminNm" maxlength="50" required
                           value="<c:out value='${not empty param.adminNm ? param.adminNm : me.adminNm()}'/>">
                </div>
                <div class="mb-3">
                    <label class="form-label required" for="email">이메일</label>
                    <input class="form-control" type="email" id="email" name="email" maxlength="100" required
                           value="<c:out value='${not empty param.email ? param.email : me.email()}'/>">
                </div>
                <div class="mb-3">
                    <label class="form-label" for="mobileNo">휴대폰 번호</label>
                    <input class="form-control" type="text" id="mobileNo" name="mobileNo" maxlength="11" placeholder="숫자만"
                           value="<c:out value='${not empty param.mobileNo ? param.mobileNo : me.mobileNo()}'/>">
                </div>
                <div class="mb-3">
                    <label class="form-label" for="deptNm">부서</label>
                    <input class="form-control" type="text" id="deptNm" name="deptNm" maxlength="100"
                           value="<c:out value='${not empty param.deptNm ? param.deptNm : me.deptNm()}'/>">
                </div>
                <c:if test="${not empty formError}"><div class="alert alert-danger" role="alert"><c:out value="${formError}"/></div></c:if>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary">저장</button>
                    <a class="btn btn-outline-secondary" href="<c:url value='/ssr/password'/>">비밀번호 변경</a>
                </div>
            </form>
        </div>
    </div>
</ui:layout>
