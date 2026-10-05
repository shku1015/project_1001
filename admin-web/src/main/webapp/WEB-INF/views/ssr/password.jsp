<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- SCR-AUTH-02 비밀번호 변경 (③ JSP SSR). 디자인(Tabler)은 화면 단계에서 입힌다. --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>비밀번호 변경 - 관리자 서비스</title>
</head>
<body>
<h1>비밀번호 변경</h1>
<c:if test="${me.pwdChangeRequired()}">
    <p role="status">임시 비밀번호로 로그인했습니다. 비밀번호를 바꿔야 다른 화면을 쓸 수 있습니다.</p>
</c:if>
<form method="post" action="<c:url value='/ssr/password'/>">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <p><label>현재 비밀번호 <input type="password" name="currentPassword" required></label></p>
    <p><label>새 비밀번호 <input type="password" name="newPassword" required maxlength="20"></label></p>
    <p><label>새 비밀번호 확인 <input type="password" name="newPasswordConfirm" required maxlength="20"></label></p>
    <c:if test="${not empty fieldError}"><p role="alert" class="field-error"><c:out value="${fieldError}"/></p></c:if>
    <button type="submit">변경</button>
</form>
<c:if test="${me.pwdChangeRequired()}">
    <form method="post" action="<c:url value='/ssr/logout'/>">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
        <button type="submit">로그아웃</button>
    </form>
</c:if>
</body>
</html>
