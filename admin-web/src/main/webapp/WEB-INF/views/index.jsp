<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>관리자 서비스</title>
</head>
<body>
<h1>관리자 서비스</h1>
<p>같은 화면 명세를 세 가지 방식으로 구현했습니다. 하나를 고르세요.</p>
<ul>
    <li><a href="<c:url value='/react/'/>">① React (토큰)</a></li>
    <li><a href="<c:url value='/jsp/login'/>">② JSP + API (토큰)</a></li>
    <li><a href="<c:url value='/ssr/login'/>">③ JSP SSR (세션)</a></li>
</ul>
</body>
</html>
