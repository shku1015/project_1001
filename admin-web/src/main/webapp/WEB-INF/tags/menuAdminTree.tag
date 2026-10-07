<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="items" required="true" type="java.util.List" %>
<%@ attribute name="parentId" required="true" %>
<%@ attribute name="selectedId" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 메뉴관리 트리 (재귀). ②는 admin-menu.js, ①은 MenuPage.tsx가 같은 마크업을 만든다 --%>
<ul class="menu-tree" data-parent-id="${parentId}">
    <c:forEach var="m" items="${items}">
        <li class="menu-node" data-menu-id="${m.menuId()}">
            <div class="menu-node-row${m.menuId() == selectedId ? ' is-selected' : ''}${m.useYn() == 'N' ? ' is-unused' : ''}">
                <span class="drag-handle" title="끌어서 순서 변경" aria-hidden="true">⠿</span>
                <a class="menu-node-link" href="<c:url value='/ssr/menus?menu=${m.menuId()}'/>"><c:out value="${m.menuNm()}"/></a>
                <c:if test="${m.systemYn() == 'Y'}"><span class="badge bg-secondary-lt" title="시스템 메뉴">&#x1F512;</span></c:if>
                <c:if test="${m.boardAutoYn() == 'Y'}"><span class="badge bg-azure-lt">자동</span></c:if>
                <span class="badge bg-yellow-lt changed-badge" hidden>변경됨</span>
            </div>
            <c:if test="${not empty m.children()}">
                <ui:menuAdminTree items="${m.children()}" parentId="${m.menuId()}" selectedId="${selectedId}"/>
            </c:if>
        </li>
    </c:forEach>
</ul>
