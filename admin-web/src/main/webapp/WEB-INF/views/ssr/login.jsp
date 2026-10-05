<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-AUTH-01 로그인 (③ JSP SSR). 화면 구조는 ①② 로그인과 같다 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <ui:head title="로그인"/>
</head>
<body>
<div class="page page-center login-page">
    <div class="container container-tight py-4">
        <div class="text-center mb-4"><h1>관리자 서비스</h1></div>
        <div class="card card-md">
            <div class="card-body">
                <h2 class="h2 text-center mb-4">로그인</h2>
                <form method="post" action="<c:url value='/ssr/login'/>">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="mb-3">
                        <label class="form-label" for="loginId">아이디</label>
                        <input class="form-control" type="text" id="loginId" name="loginId" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="password">비밀번호</label>
                        <input class="form-control" type="password" id="password" name="password" required>
                    </div>
                    <c:if test="${param.error != null}">
                        <div class="alert alert-danger" role="alert"><c:out value="${sessionScope.SPRING_SECURITY_LAST_EXCEPTION.message}"/></div>
                    </c:if>
                    <c:if test="${param.logout != null}">
                        <div class="alert alert-info" role="status">로그아웃되었습니다.</div>
                    </c:if>
                    <button type="submit" class="btn btn-primary w-100">로그인</button>
                </form>
            </div>
        </div>
    </div>
</div>
</body>
</html>
