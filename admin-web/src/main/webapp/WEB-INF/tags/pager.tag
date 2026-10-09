<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="path" required="true" description="목록 주소 (예: /ssr/admins)" %>
<%@ attribute name="page" required="true" type="java.lang.Integer" %>
<%@ attribute name="size" required="true" type="java.lang.Integer" %>
<%@ attribute name="totalPages" required="true" type="java.lang.Integer" %>
<%@ attribute name="totalCount" required="true" type="java.lang.Long" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- CMP-03 페이징 (docs/05-ia-screens.md 4절). « ‹ 번호(최대 10개) › » + 총 건수 + 페이지 크기.
     지금 검색 조건·정렬을 그대로 두고 page만 바꾼다. ①은 Pager.tsx, ②는 admin-pager.js가 같은 마크업을 만든다 --%>
<c:set var="last" value="${totalPages < 1 ? 1 : totalPages}"/>
<c:set var="start" value="${((page - 1) - (page - 1) % 10) + 1}"/>
<c:set var="end" value="${start + 9 > last ? last : start + 9}"/>
<div class="card-footer d-flex align-items-center flex-wrap gap-2" id="pager">
    <p class="m-0 text-secondary">총 <strong id="total-count"><fmt:formatNumber value="${totalCount}"/></strong>건</p>
    <ul class="pagination m-0 ms-auto">
        <c:forEach var="item" items="first,prev,nums,next,last">
            <c:choose>
                <c:when test="${item == 'nums'}">
                    <c:forEach var="n" begin="${start}" end="${end}">
                        <c:url var="href" value="${path}"><c:forEach var="p" items="${paramValues}"><c:if test="${p.key != 'page'}"><c:forEach var="v" items="${p.value}"><c:param name="${p.key}" value="${v}"/></c:forEach></c:if></c:forEach><c:param name="page" value="${n}"/></c:url>
                        <li class="page-item${n == page ? ' active' : ''}"><a class="page-link" href="${href}" data-page="${n}">${n}</a></li>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <c:set var="target" value="${item == 'first' ? 1 : item == 'prev' ? page - 1 : item == 'next' ? page + 1 : last}"/>
                    <c:set var="off" value="${(item == 'first' or item == 'prev') ? page <= 1 : page >= last}"/>
                    <c:url var="href" value="${path}"><c:forEach var="p" items="${paramValues}"><c:if test="${p.key != 'page'}"><c:forEach var="v" items="${p.value}"><c:param name="${p.key}" value="${v}"/></c:forEach></c:if></c:forEach><c:param name="page" value="${target}"/></c:url>
                    <li class="page-item${off ? ' disabled' : ''}">
                        <a class="page-link" href="${off ? '#' : href}" data-page="${target}"
                           aria-label="${item == 'first' ? '첫 페이지' : item == 'prev' ? '이전 페이지' : item == 'next' ? '다음 페이지' : '마지막 페이지'}">${item == 'first' ? '«' : item == 'prev' ? '‹' : item == 'next' ? '›' : '»'}</a>
                    </li>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </ul>
    <%-- 인라인 핸들러 안에서는 URL이 document.URL(문자열)로 해석되므로 window.URL을 쓴다 --%>
    <select class="form-select form-select-sm w-auto" id="page-size" aria-label="페이지 크기"
            onchange="var u = new window.URL(window.location.href); u.searchParams.set('size', this.value); u.searchParams.set('page', '1'); window.location.href = u;">
        <c:forEach var="s" items="10,20,50"><option value="${s}" ${s == size ? 'selected' : ''}>${s}건씩</option></c:forEach>
    </select>
</div>
