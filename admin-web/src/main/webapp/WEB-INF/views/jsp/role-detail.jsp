<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ROL-02 역할 상세 (② JSP + API). 역할 정보·권한 표·관리자 목록은 admin-role-api.js가 채운다. 마크업은 ③ ssr/role-detail.jsp와 같다 --%>
<ui:layout title="역할" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-perm.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="role-page" data-page="detail" data-role-id="${roleId}" hidden></div>
        <div class="card mb-3" id="role-info"></div>

        <div class="card">
            <div class="card-header">
                <ul class="nav nav-tabs card-header-tabs" role="tablist">
                    <li class="nav-item" role="presentation"><a class="nav-link active" id="tab-permissions" data-bs-toggle="tab" href="#pane-permissions" role="tab">권한</a></li>
                    <li class="nav-item" role="presentation"><a class="nav-link" id="tab-admins" data-bs-toggle="tab" href="#pane-admins" role="tab">관리자</a></li>
                </ul>
            </div>
            <div class="tab-content">
                <div class="tab-pane active show" id="pane-permissions" role="tabpanel">
                    <div class="table-responsive">
                        <table class="table table-vcenter table-sm mb-0" id="perm-table">
                            <thead><tr><th>메뉴</th><th class="text-center">조회</th><th class="text-center">등록</th><th class="text-center">수정</th>
                                <th class="text-center">삭제</th><th class="text-center">엑셀</th><th class="text-center">개인정보</th></tr></thead>
                            <tbody id="perm-tbody"></tbody>
                        </table>
                    </div>
                    <div class="card-footer text-end" id="perm-footer" hidden>
                        <button type="button" class="btn btn-primary" id="btn-perm-save">권한 저장</button>
                    </div>
                </div>
                <div class="tab-pane" id="pane-admins" role="tabpanel">
                    <div class="table-responsive">
                        <table class="table table-vcenter mb-0" id="admin-table">
                            <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서</th><th>상태</th><th>부여일시</th></tr></thead>
                            <tbody id="admin-tbody"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal modal-blur fade" id="copy-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" id="copy-form" novalidate>
                    <div class="modal-header">
                        <h5 class="modal-title">역할 복사</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
                        <p class="text-secondary" id="copy-guide"></p>
                        <div class="mb-3">
                            <label class="form-label required" for="copy-roleCd">새 역할 코드</label>
                            <input class="form-control" id="copy-roleCd" name="roleCd" maxlength="50" required placeholder="영문 대문자·숫자·_">
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="copy-roleNm">새 역할명</label>
                            <input class="form-control" id="copy-roleNm" name="roleNm" maxlength="100" required>
                        </div>
                        <div id="copy-message"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                        <button type="submit" class="btn btn-primary" id="btn-copy-save">복사</button>
                    </div>
                </form>
            </div>
        </div>
    </jsp:body>
</ui:layout>
