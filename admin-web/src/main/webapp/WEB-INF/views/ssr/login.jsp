<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- SCR-AUTH-01 로그인 (③ JSP SSR). 디자인(Tabler)은 화면 단계에서 입힌다. --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>로그인 - 관리자 서비스</title>
</head>
<body>
<h1>관리자 서비스</h1>
<form method="post" action="<c:url value='/ssr/login'/>">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <p><label>아이디 <input type="text" name="loginId" required autofocus></label></p>
    <p><label>비밀번호 <input type="password" name="password" required></label></p>
    <c:if test="${param.error != null}">
        <p role="alert" class="login-error"><c:out value="${sessionScope.SPRING_SECURITY_LAST_EXCEPTION.message}"/></p>
    </c:if>
    <c:if test="${param.logout != null}">
        <p role="status">로그아웃되었습니다.</p>
    </c:if>
    <button type="submit">로그인</button>
</form>
</body>
</html>
