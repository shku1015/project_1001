<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="mode" required="true" description="ssr: 서버가 메뉴·내 정보를 그린다 / jsp: admin-jsp.js가 API로 채운다" %>
<%@ attribute name="scripts" fragment="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 공통 레이아웃 (docs/05-ia-screens.md 3절): 왼쪽 메뉴, 헤더(내 이름·메뉴), 경로, 화면 제목, 본문 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <ui:head title="${title}"/>
</head>
<body>
<div class="page">
    <aside class="navbar navbar-vertical navbar-expand-lg" data-bs-theme="dark">
        <div class="container-fluid">
            <h1 class="navbar-brand">
                <a href="<c:url value='${prefix}/'/>" class="text-white text-decoration-none">관리자 서비스</a>
            </h1>
            <div class="navbar-collapse">
                <ul class="navbar-nav pt-lg-3" id="side-menu" aria-label="메뉴">
                    <c:if test="${mode == 'ssr'}"><ui:menu items="${me.menus()}" prefix="${prefix}"/></c:if>
                </ul>
            </div>
        </div>
    </aside>

    <header class="navbar navbar-expand-md d-print-none">
        <div class="container-xl justify-content-end">
            <div class="nav-item dropdown" id="user-menu">
                <a href="#" class="nav-link" data-bs-toggle="dropdown" data-bs-display="static" aria-label="내 메뉴">
                    <span id="user-name"><c:if test="${mode == 'ssr'}"><c:out value="${me.adminNm()}"/></c:if></span>
                    <span class="text-secondary ms-1" id="user-role"><c:if test="${mode == 'ssr' and not empty me.roles()}">(<c:out value="${me.roles()[0].roleNm()}"/>)</c:if></span>
                </a>
                <div class="dropdown-menu dropdown-menu-end">
                    <a class="dropdown-item" href="<c:url value='${prefix}/me'/>">내 정보</a>
                    <a class="dropdown-item" href="<c:url value='${prefix}/password'/>">비밀번호 변경</a>
                    <c:choose>
                        <c:when test="${mode == 'ssr'}">
                            <form method="post" action="<c:url value='/ssr/logout'/>">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <button type="submit" class="dropdown-item">로그아웃</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <button type="button" class="dropdown-item" id="logout-button">로그아웃</button>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </header>

    <div class="page-wrapper">
        <div class="page-header">
            <div class="container-xl">
                <ol class="breadcrumb" aria-label="경로">
                    <li class="breadcrumb-item"><a href="<c:url value='${prefix}/'/>">홈</a></li>
                    <c:if test="${title != '홈'}"><li class="breadcrumb-item active"><c:out value="${title}"/></li></c:if>
                </ol>
                <h2 class="page-title"><c:out value="${title}"/></h2>
            </div>
        </div>
        <div class="page-body">
            <div class="container-xl">
                <div id="notice-area">
                    <c:if test="${not empty notice}">
                        <div class="alert alert-success" role="status"><c:out value="${notice}"/></div>
                    </c:if>
                    <c:if test="${not empty error}">
                        <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
                    </c:if>
                </div>
                <jsp:doBody/>
            </div>
        </div>
    </div>
</div>
<script src="<c:url value='/webjars/tabler__core/${tablerVersion}/dist/js/tabler.min.js'/>"></script>
<jsp:invoke fragment="scripts"/>
</body>
</html>
