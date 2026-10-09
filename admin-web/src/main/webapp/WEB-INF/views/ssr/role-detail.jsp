<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ROL-02 역할 상세 (③ JSP SSR). 위쪽 역할 정보, 아래쪽 [권한] [관리자] 탭.
     ② jsp/role-detail.jsp(admin-role-api.js), ① RoleDetailPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="isSystem" value="${role.systemYn() == 'Y'}"/>
<c:set var="permEditable" value="${role.editable() and canUpdate}"/>
<ui:layout title="역할" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-role-perm.js'/>"></script>
        <script>
          (function () {
            var table = document.getElementById('perm-table');
            AdminRolePerm.bind(table);
            var permForm = document.getElementById('perm-form');
            if (permForm) {
              permForm.addEventListener('submit', function (e) {
                if (!confirm('이 역할을 가진 관리자 ${role.adminCnt()}명에게 바로 반영됩니다. 저장하시겠습니까?')) { e.preventDefault(); return; }
                this.permissions.value = JSON.stringify(AdminRolePerm.collect(table));
              });
            }
            var deleteForm = document.getElementById('delete-form');
            if (deleteForm) {
              deleteForm.addEventListener('submit', function (e) {
                if (!confirm('역할을 삭제하시겠습니까?')) { e.preventDefault(); }
              });
            }
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="card mb-3" id="role-info">
            <div class="card-header">
                <h3 class="card-title" id="role-title">
                    <c:if test="${isSystem}">&#x1F512; </c:if><c:out value="${role.roleNm()}"/>
                    <c:if test="${role.mine()}"><span class="badge bg-blue-lt ms-2">내 역할</span></c:if>
                </h3>
                <div class="card-actions btn-list">
                    <a class="btn btn-sm" id="btn-list" href="<c:url value='/ssr/roles'/>">목록</a>
                    <c:if test="${role.editable()}">
                        <c:choose>
                            <c:when test="${canUpdate}"><a class="btn btn-sm" id="btn-edit" href="<c:url value='/ssr/roles/${role.roleId()}/edit'/>">수정</a></c:when>
                            <c:otherwise><button type="button" class="btn btn-sm" id="btn-edit" disabled title="${noPerm}">수정</button></c:otherwise>
                        </c:choose>
                    </c:if>
                    <button type="button" class="btn btn-sm" id="btn-copy" data-bs-toggle="modal" data-bs-target="#copy-modal"
                            ${canCreate ? '' : 'disabled'} title="${canCreate ? '' : noPerm}">복사</button>
                    <c:if test="${not isSystem and role.adminCnt() == 0}">
                        <form method="post" action="<c:url value='/ssr/roles/${role.roleId()}/delete'/>" id="delete-form" class="d-inline">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm btn-ghost-danger" id="btn-delete"
                                    ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">삭제</button>
                        </form>
                    </c:if>
                </div>
            </div>
            <div class="card-body">
                <c:if test="${not role.editable()}">
                    <div class="alert alert-info" role="note">${isSystem ? '시스템 역할' : '내가 가진 역할'}은 수정하거나 권한을 바꿀 수 없습니다.</div>
                </c:if>
                <dl class="row mb-0">
                    <dt class="col-sm-2">역할 코드</dt><dd class="col-sm-4" id="role-roleCd"><c:out value="${role.roleCd()}"/></dd>
                    <dt class="col-sm-2">사용 여부</dt><dd class="col-sm-4" id="role-useYn">${role.useYn() == 'Y' ? '사용' : '사용 안 함'}</dd>
                    <dt class="col-sm-2">설명</dt><dd class="col-sm-10" id="role-description"><c:out value="${empty role.description() ? '-' : role.description()}"/></dd>
                    <dt class="col-sm-2">관리자 수</dt><dd class="col-sm-4" id="role-adminCnt">${role.adminCnt()}</dd>
                    <dt class="col-sm-2">수정</dt>
                    <dd class="col-sm-4"><c:out value="${empty role.modNm() ? '-' : role.modNm()}"/> · ${fn:substring(fn:replace(role.modDt().toString(), 'T', ' '), 0, 16)}</dd>
                </dl>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <ul class="nav nav-tabs card-header-tabs" role="tablist">
                    <li class="nav-item" role="presentation"><a class="nav-link active" id="tab-permissions" data-bs-toggle="tab" href="#pane-permissions" role="tab">권한</a></li>
                    <li class="nav-item" role="presentation"><a class="nav-link" id="tab-admins" data-bs-toggle="tab" href="#pane-admins" role="tab">관리자 (${role.adminCnt()})</a></li>
                </ul>
            </div>
            <div class="tab-content">
                <div class="tab-pane active show" id="pane-permissions" role="tabpanel">
                    <div class="table-responsive">
                        <table class="table table-vcenter table-sm mb-0" id="perm-table">
                            <thead><tr><th>메뉴</th><th class="text-center">조회</th><th class="text-center">등록</th><th class="text-center">수정</th>
                                <th class="text-center">삭제</th><th class="text-center">엑셀</th><th class="text-center">개인정보</th></tr></thead>
                            <tbody>
                            <ui:rolePermRows nodes="${permTree}" actions="${allActions}" editable="${permEditable}"/>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${role.editable()}">
                        <div class="card-footer text-end">
                            <form method="post" action="<c:url value='/ssr/roles/${role.roleId()}/permissions'/>" id="perm-form">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <input type="hidden" name="permissions">
                                <input type="hidden" name="modDt" value="${role.modDt()}">
                                <button type="submit" class="btn btn-primary" id="btn-perm-save"
                                        ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">권한 저장</button>
                            </form>
                        </div>
                    </c:if>
                </div>
                <div class="tab-pane" id="pane-admins" role="tabpanel">
                    <div class="table-responsive">
                        <table class="table table-vcenter mb-0" id="admin-table">
                            <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서</th><th>상태</th><th>부여일시</th></tr></thead>
                            <tbody>
                            <c:forEach var="a" items="${admins}">
                                <tr>
                                    <td><a href="<c:url value='/ssr/admins/${a.adminId()}'/>"><c:out value="${a.loginId()}"/></a></td>
                                    <td><c:out value="${a.adminNm()}"/></td>
                                    <td><c:out value="${empty a.deptNm() ? '-' : a.deptNm()}"/></td>
                                    <td><c:out value="${a.statusNm()}"/></td>
                                    <td>${fn:substring(fn:replace(a.grantedDt().toString(), 'T', ' '), 0, 16)}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty admins}"><tr><td colspan="5" class="text-secondary text-center">이 역할을 가진 관리자가 없습니다</td></tr></c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal modal-blur fade" id="copy-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" method="post" action="<c:url value='/ssr/roles/${role.roleId()}/copy'/>" id="copy-form">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="modal-header">
                        <h5 class="modal-title">역할 복사</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
                        <p class="text-secondary"><c:out value="${role.roleNm()}"/>의 설명·권한을 그대로 가진 새 역할을 만듭니다.</p>
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
