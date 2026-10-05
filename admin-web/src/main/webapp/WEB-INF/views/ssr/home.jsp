<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-HOME 홈 (③ JSP SSR). 일시는 yyyy-MM-dd HH:mm (docs/05-ia-screens.md 4.1) --%>
<ui:layout title="홈" mode="ssr">
    <div class="row row-cards">
        <div class="col-md-6">
            <div class="card">
                <div class="card-header"><h3 class="card-title">내 정보</h3></div>
                <div class="card-body">
                    <dl class="row mb-0">
                        <dt class="col-5">이름</dt>
                        <dd class="col-7" id="home-name"><c:out value="${me.adminNm()}"/></dd>
                        <dt class="col-5">역할</dt>
                        <dd class="col-7" id="home-roles"><c:forEach var="r" items="${me.roles()}" varStatus="s"><c:out value="${r.roleNm()}"/><c:if test="${!s.last}">, </c:if></c:forEach><c:if test="${empty me.roles()}">-</c:if></dd>
                        <dt class="col-5">마지막 로그인</dt>
                        <dd class="col-7" id="home-last-login">
                            <c:choose>
                                <c:when test="${me.lastLoginDt() != null}">${fn:substring(fn:replace(me.lastLoginDt().toString(), 'T', ' '), 0, 16)} (<c:out value="${me.lastLoginIp()}"/>)</c:when>
                                <c:otherwise>이전 로그인 기록이 없습니다</c:otherwise>
                            </c:choose>
                        </dd>
                        <dt class="col-5">비밀번호 변경일</dt>
                        <dd class="col-7" id="home-pwd-changed"><c:choose><c:when test="${me.pwdChangedDt() != null}">${fn:substring(me.pwdChangedDt().toString(), 0, 10)}</c:when><c:otherwise>-</c:otherwise></c:choose></dd>
                    </dl>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card">
                <div class="card-header"><h3 class="card-title">바로가기</h3></div>
                <div class="card-body shortcut-list d-flex flex-wrap gap-2" id="home-shortcuts">
                    <c:forEach var="m" items="${shortcuts}">
                        <a class="btn btn-outline-primary" href="<c:url value='${prefix}${m.menuUrl()}'/>"><c:out value="${m.menuNm()}"/></a>
                    </c:forEach>
                    <c:if test="${empty shortcuts}">
                        <p class="text-secondary mb-0">사용 가능한 메뉴가 없습니다. 관리자에게 권한을 요청하세요</p>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</ui:layout>
