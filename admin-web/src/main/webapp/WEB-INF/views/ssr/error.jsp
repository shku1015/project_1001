<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ERR-403/404/500 (③ JSP SSR) --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <ui:head title="오류"/>
</head>
<body>
<div class="page page-center login-page">
    <div class="container container-tight py-4 text-center">
        <div class="empty">
            <div class="empty-header"><c:out value="${status}"/></div>
            <p class="empty-title"><c:out value="${message}"/></p>
            <div class="empty-action"><a href="<c:url value='/ssr/'/>" class="btn btn-primary">홈으로</a></div>
        </div>
    </div>
</div>
</body>
</html>
