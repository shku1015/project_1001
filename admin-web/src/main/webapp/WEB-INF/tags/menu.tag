<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="items" required="true" type="java.util.List" %>
<%@ attribute name="prefix" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 왼쪽 메뉴 트리 (재귀). ② JSP + API는 admin-jsp.js가 같은 마크업을 만든다 --%>
<c:forEach var="m" items="${items}">
    <li class="nav-item">
        <c:choose>
            <c:when test="${m.menuTypeCd() == 'PAGE'}">
                <a class="nav-link" href="<c:url value='${prefix}${m.menuUrl()}'/>" data-menu-cd="${m.menuCd()}">
                    <span class="nav-link-title"><c:out value="${m.menuNm()}"/></span>
                </a>
            </c:when>
            <c:otherwise>
                <span class="nav-link menu-folder"><span class="nav-link-title"><c:out value="${m.menuNm()}"/></span></span>
                <ul class="navbar-nav menu-children">
                    <ui:menu items="${m.children()}" prefix="${prefix}"/>
                </ul>
            </c:otherwise>
        </c:choose>
    </li>
</c:forEach>
