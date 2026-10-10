<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-USR-01 회원 목록 (② JSP + API). admin-user-api.js가 /api/v1/users 로 채운다. 마크업은 ③ ssr/user-list.jsp와 같다 --%>
<ui:layout title="사용자관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script src="<c:url value='/common/js/admin-user-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="user-page" data-page="list" hidden></div>
        <div class="card">
            <div class="card-header">
                <h3 class="card-title">회원 목록</h3>
                <div class="card-actions btn-list">
                    <button type="button" class="btn btn-sm" id="btn-excel" disabled>엑셀</button>
                    <a class="btn btn-primary btn-sm disabled" id="btn-user-create" href="<c:url value='/jsp/users/new'/>" aria-disabled="true">등록</a>
                </div>
            </div>
            <div class="card-body border-bottom">
                <form class="row g-2" id="user-search">
                    <div class="col-md-2">
                        <select class="form-select" id="search-userTypeCd" name="userTypeCd" aria-label="회원 구분">
                            <option value="">구분 전체</option>
                            <option value="PERSONAL">개인</option>
                            <option value="CORPORATE">기업</option>
                        </select>
                    </div>
                    <div class="col-md-2"><input class="form-control" id="search-loginId" name="loginId" placeholder="로그인 아이디" aria-label="로그인 아이디"></div>
                    <div class="col-md-2"><input class="form-control" id="search-userNm" name="userNm" placeholder="이름" aria-label="이름"></div>
                    <div class="col-md-2"><input class="form-control" id="search-email" name="email" placeholder="이메일" aria-label="이메일"></div>
                    <div class="col-md-2"><input class="form-control" id="search-mobileNo" name="mobileNo" placeholder="휴대폰 번호 (숫자)" aria-label="휴대폰 번호" inputmode="numeric"></div>
                    <div class="col-md-2"><input class="form-control" id="search-companyNm" name="companyNm" placeholder="소속 기업" aria-label="소속 기업"></div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-statusCd" name="statusCd" aria-label="상태">
                            <option value="">상태 전체</option>
                            <option value="ACTIVE">정상</option>
                            <option value="DORMANT">휴면</option>
                            <option value="SUSPENDED">정지</option>
                            <option value="WITHDRAWN">탈퇴</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <select class="form-select" id="search-joinPath" name="joinPath" aria-label="가입 경로">
                            <option value="">가입 경로 전체</option>
                            <option value="USER_SERVICE">사용자 서비스</option>
                            <option value="ADMIN">관리자 등록</option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <div class="date-range">
                            <div class="input-group">
                                <input type="date" class="form-control" id="search-joinDtFrom" name="joinDtFrom" aria-label="가입일 시작" data-range-from>
                                <span class="input-group-text">~</span>
                                <input type="date" class="form-control" id="search-joinDtTo" name="joinDtTo" aria-label="가입일 끝" data-range-to>
                            </div>
                            <div class="btn-list mt-1">
                                <button type="button" class="btn btn-sm date-quick" data-range="today">오늘</button>
                                <button type="button" class="btn btn-sm date-quick" data-range="1w">1주일</button>
                                <button type="button" class="btn btn-sm date-quick" data-range="1m">1개월</button>
                                <button type="button" class="btn btn-sm date-quick" data-range="3m">3개월</button>
                            </div>
                        </div>
                    </div>
                    <div class="col-12 d-flex gap-2 justify-content-end">
                        <button type="submit" class="btn btn-primary">검색</button>
                        <a class="btn" id="btn-search-reset" href="<c:url value='/jsp/users'/>">초기화</a>
                    </div>
                </form>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter" id="user-table">
                    <thead><tr id="user-thead"></tr></thead>
                    <tbody id="user-tbody"></tbody>
                </table>
            </div>
            <div id="pager"></div>
        </div>
    </jsp:body>
</ui:layout>
