<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- SCR-ERR-403/404/500 (③ JSP SSR) --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>오류 - 관리자 서비스</title>
</head>
<body>
<h1><c:out value="${status}"/></h1>
<p><c:out value="${message}"/></p>
<a href="<c:url value='/ssr/'/>">홈으로</a>
</body>
</html>
