<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="field" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="current" required="false" description="지금 정렬 (예: loginId,asc)" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- CMP-02 정렬 머리글. 누르면 오름차순, 다시 누르면 내림차순. 검색 조건은 그대로 두고 1페이지로 간다.
     ①은 SortTh(AdminListPage.tsx), ②는 admin-pager.js가 같은 마크업을 만든다 --%>
<c:set var="dir" value="${current == field.concat(',asc') ? 'asc' : current == field.concat(',desc') ? 'desc' : ''}"/>
<c:url var="href" value="${path}"><c:forEach var="p" items="${paramValues}"><c:if test="${p.key != 'page' and p.key != 'sort'}"><c:forEach var="v" items="${p.value}"><c:param name="${p.key}" value="${v}"/></c:forEach></c:if></c:forEach><c:param name="sort" value="${field},${dir == 'asc' ? 'desc' : 'asc'}"/></c:url>
<th aria-sort="${dir == 'asc' ? 'ascending' : dir == 'desc' ? 'descending' : 'none'}"><a class="sort-link text-reset" href="${href}" data-sort="${field}"><c:out value="${label}"/><span class="sort-mark">${dir == 'asc' ? ' ▲' : dir == 'desc' ? ' ▼' : ''}</span></a></th>
