<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-AUTH-02 비밀번호 변경 (③ JSP SSR).
     임시 비밀번호면 헤더·메뉴 없이 보여 주고(로그아웃만 가능), 아니면 일반 레이아웃 안에 보여 준다 --%>
<c:choose>
    <c:when test="${me.pwdChangeRequired()}">
<!DOCTYPE html>
<html lang="ko">
<head>
    <ui:head title="비밀번호 변경"/>
</head>
<body>
<div class="page page-center login-page">
    <div class="container container-tight py-4">
        <h2 class="page-title mb-3">비밀번호 변경</h2>
        <%@ include file="password-form.jspf" %>
        <form method="post" action="<c:url value='/ssr/logout'/>" class="mt-3 text-center">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button type="submit" class="btn btn-link">로그아웃</button>
        </form>
    </div>
</div>
</body>
</html>
    </c:when>
    <c:otherwise>
        <ui:layout title="비밀번호 변경" mode="ssr">
            <%@ include file="password-form.jspf" %>
        </ui:layout>
    </c:otherwise>
</c:choose>
