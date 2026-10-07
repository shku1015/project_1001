<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="nodes" required="true" type="java.util.List" %>
<%@ attribute name="actions" required="true" type="java.lang.Object[]" %>
<%@ attribute name="editable" required="true" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- 역할 권한 표의 행 (재귀). ②는 admin-role-api.js, ①은 RoleDetailPage.tsx가 같은 마크업을 만든다.
     폴더 행에는 체크박스가 없고, 메뉴에서 쓰지 않는 액션 칸은 비운다. 내가 줄 수 없는 칸은 비활성 (BR-05) --%>
<c:forEach var="n" items="${nodes}">
    <c:set var="folder" value="${n.menuTypeCd() == 'FOLDER'}"/>
    <tr class="${folder ? 'perm-folder' : 'perm-row'}${n.useYn() == 'N' ? ' text-secondary' : ''}" data-menu-id="${n.menuId()}">
        <td class="perm-depth-${n.depth()}${folder ? ' fw-bold' : ''}"><c:out value="${n.menuNm()}"/></td>
        <c:forEach var="a" items="${actions}">
            <td class="text-center">
                <c:if test="${n.actions().contains(a)}">
                    <c:set var="label" value="${a == 'READ' ? '조회' : a == 'CREATE' ? '등록' : a == 'UPDATE' ? '수정' : a == 'DELETE' ? '삭제' : a == 'EXCEL' ? '엑셀' : '개인정보'}"/>
                    <input type="checkbox" class="form-check-input m-0 perm-check" id="perm-${n.menuId()}-${a}" data-action="${a}"
                           aria-label="<c:out value='${n.menuNm()}'/> ${label}"
                           ${n.granted().contains(a) ? 'checked' : ''} ${editable and n.grantableActions().contains(a) ? '' : 'disabled'}>
                </c:if>
            </td>
        </c:forEach>
    </tr>
    <c:if test="${not empty n.children()}">
        <ui:rolePermRows nodes="${n.children()}" actions="${actions}" editable="${editable}"/>
    </c:if>
</c:forEach>
