<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="value" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 휴대폰 번호 표시 (docs/05-ia-screens.md 4.1): 01012345678 → 010-1234-5678. 마스킹된 값(010-****-5678)과 빈 값(-)은 그대로 --%>
<c:choose>
    <c:when test="${empty value}">-</c:when>
    <c:when test="${value.length() == 11 and not value.contains('*')}">${value.substring(0, 3)}-${value.substring(3, 7)}-${value.substring(7)}</c:when>
    <c:when test="${value.length() == 10 and not value.contains('*')}">${value.substring(0, 3)}-${value.substring(3, 6)}-${value.substring(6)}</c:when>
    <c:otherwise><c:out value="${value}"/></c:otherwise>
</c:choose>
