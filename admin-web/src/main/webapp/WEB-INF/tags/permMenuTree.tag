<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="items" required="true" type="java.util.List" %>
<%@ attribute name="selectedId" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 권한관리 메뉴 트리 (재귀). 화면 메뉴만 고를 수 있고 폴더는 이름만 보인다.
     ②는 admin-permission-api.js, ①은 PermissionPage.tsx가 같은 마크업을 만든다 --%>
<ul class="menu-tree">
    <c:forEach var="m" items="${items}">
        <li class="menu-node" data-menu-id="${m.menuId()}">
            <div class="menu-node-row${m.menuId() == selectedId ? ' is-selected' : ''}${m.useYn() == 'N' ? ' is-unused' : ''}">
                <c:choose>
                    <c:when test="${m.menuTypeCd() == 'PAGE'}"><a class="menu-node-link" href="<c:url value='/ssr/permissions?menu=${m.menuId()}'/>"><c:out value="${m.menuNm()}"/></a></c:when>
                    <c:otherwise><span class="menu-node-link fw-bold"><c:out value="${m.menuNm()}"/></span></c:otherwise>
                </c:choose>
            </div>
            <c:if test="${not empty m.children()}">
                <ui:permMenuTree items="${m.children()}" selectedId="${selectedId}"/>
            </c:if>
        </li>
    </c:forEach>
</ul>
