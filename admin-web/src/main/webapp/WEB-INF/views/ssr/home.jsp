<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- SCR-HOME 홈 (③ JSP SSR). 레이아웃·메뉴·디자인은 화면 단계에서 입힌다. --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>홈 - 관리자 서비스</title>
</head>
<body>
<c:if test="${not empty notice}"><p role="status"><c:out value="${notice}"/></p></c:if>
<h1>홈</h1>
<p class="me-name"><c:out value="${me.adminNm()}"/> (<c:out value="${me.loginId()}"/>)</p>
<ul class="my-menus">
    <c:forEach var="menu" items="${me.menus()}">
        <li><c:out value="${menu.menuNm()}"/></li>
    </c:forEach>
</ul>
<form method="post" action="<c:url value='/ssr/logout'/>">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <button type="submit">로그아웃</button>
</form>
</body>
</html>
