<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ADM-02 관리자 상세 (③ JSP SSR). 기본 정보, 역할, 최근 로그인 이력.
     ② jsp/admin-detail.jsp(admin-admin-api.js), ① AdminDetailPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="status" value="${admin.statusCd()}"/>
<ui:layout title="관리자" mode="ssr">
    <jsp:attribute name="scripts">
        <script>
          (function () {
            // 확인창 (CMP-06): data-confirm 문구로 묻고, 취소하면 보내지 않는다
            document.querySelectorAll('form[data-confirm]').forEach(function (f) {
              f.addEventListener('submit', function (e) {
                if (!confirm(f.dataset.confirm)) { e.preventDefault(); }
              });
            });
            var copy = document.getElementById('btn-temp-copy');
            if (copy) {
              copy.addEventListener('click', function () {
                navigator.clipboard.writeText(document.getElementById('temp-password').textContent);
                copy.textContent = '복사됨';
              });
              document.getElementById('btn-temp-close').addEventListener('click', function () {
                document.getElementById('temp-password-box').remove();
              });
            }
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <c:if test="${not empty tempPassword}">
            <div class="alert alert-warning" role="alert" id="temp-password-box">
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <span>임시 비밀번호:</span><code class="fs-3" id="temp-password"><c:out value="${tempPassword}"/></code>
                    <button type="button" class="btn btn-sm" id="btn-temp-copy">복사</button>
                    <button type="button" class="btn btn-sm ms-auto" id="btn-temp-close">닫기</button>
                </div>
                <div class="mt-1 small">이 창을 닫거나 화면을 옮기면 다시 볼 수 없습니다. 관리자에게 전달하세요.</div>
            </div>
        </c:if>

        <div class="card mb-3" id="admin-info">
            <div class="card-header">
                <h3 class="card-title" id="admin-title"><c:out value="${admin.adminNm()}"/>
                    <span class="text-secondary ms-1">(<c:out value="${admin.loginId()}"/>)</span>
                    <c:if test="${admin.self()}"><span class="badge bg-blue-lt ms-2">본인</span></c:if>
                </h3>
                <div class="card-actions btn-list">
                    <a class="btn btn-sm" id="btn-list" href="<c:url value='/ssr/admins'/>">목록</a>
                    <c:choose>
                        <c:when test="${canUpdate}"><a class="btn btn-sm" id="btn-edit" href="<c:url value='/ssr/admins/${admin.adminId()}/edit'/>">수정</a></c:when>
                        <c:otherwise><button type="button" class="btn btn-sm" id="btn-edit" disabled title="${noPerm}">수정</button></c:otherwise>
                    </c:choose>
                    <c:if test="${status == 'LOCKED'}">
                        <form method="post" action="<c:url value='/ssr/admins/${admin.adminId()}/unlock'/>" class="d-inline" data-confirm="잠금을 해제하시겠습니까?">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm" id="btn-unlock" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">잠금 해제</button>
                        </form>
                    </c:if>
                    <c:if test="${status != 'DISABLED'}">
                        <form method="post" action="<c:url value='/ssr/admins/${admin.adminId()}/password-reset'/>" class="d-inline" data-confirm="비밀번호를 초기화하시겠습니까? 현재 로그인도 종료됩니다.">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm" id="btn-password-reset" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">비밀번호 초기화</button>
                        </form>
                    </c:if>
                    <c:if test="${not admin.self() and status != 'DISABLED'}">
                        <form method="post" action="<c:url value='/ssr/admins/${admin.adminId()}/status'/>" class="d-inline" data-confirm="사용중지하시겠습니까? 현재 로그인도 종료됩니다.">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="statusCd" value="DISABLED"><input type="hidden" name="modDt" value="${admin.modDt()}">
                            <button type="submit" class="btn btn-sm btn-ghost-danger" id="btn-disable" ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">사용중지</button>
                        </form>
                    </c:if>
                    <c:if test="${status == 'DISABLED'}">
                        <form method="post" action="<c:url value='/ssr/admins/${admin.adminId()}/status'/>" class="d-inline" data-confirm="다시 사용하게 하시겠습니까?">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="statusCd" value="ACTIVE"><input type="hidden" name="modDt" value="${admin.modDt()}">
                            <button type="submit" class="btn btn-sm" id="btn-enable" ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">재사용</button>
                        </form>
                    </c:if>
                </div>
            </div>
            <div class="card-body">
                <dl class="row mb-0">
                    <dt class="col-sm-2">로그인 아이디</dt><dd class="col-sm-4" id="admin-loginId"><c:out value="${admin.loginId()}"/></dd>
                    <dt class="col-sm-2">상태</dt><dd class="col-sm-4" id="admin-status"><c:out value="${admin.statusNm()}"/></dd>
                    <dt class="col-sm-2">이름</dt><dd class="col-sm-4" id="admin-adminNm"><c:out value="${admin.adminNm()}"/></dd>
                    <dt class="col-sm-2">이메일</dt><dd class="col-sm-4" id="admin-email"><c:out value="${admin.email()}"/></dd>
                    <dt class="col-sm-2">휴대폰 번호</dt><dd class="col-sm-4" id="admin-mobileNo">${mobileText}</dd>
                    <dt class="col-sm-2">부서</dt><dd class="col-sm-4" id="admin-deptNm"><c:out value="${empty admin.deptNm() ? '-' : admin.deptNm()}"/></dd>
                    <dt class="col-sm-2">로그인 실패</dt><dd class="col-sm-4" id="admin-loginFailCnt">${admin.loginFailCnt()}회</dd>
                    <dt class="col-sm-2">임시 비밀번호</dt><dd class="col-sm-4" id="admin-pwdTempYn">${admin.pwdTempYn() == 'Y' ? '예' : '아니오'}</dd>
                    <dt class="col-sm-2">비밀번호 변경</dt><dd class="col-sm-4">${admin.pwdChangedDt() == null ? '-' : admin.pwdChangedDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">마지막 로그인</dt><dd class="col-sm-4">${admin.lastLoginDt() == null ? '-' : admin.lastLoginDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">등록</dt><dd class="col-sm-4"><c:out value="${empty admin.regNm() ? '-' : admin.regNm()}"/> · ${admin.regDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">수정</dt><dd class="col-sm-4"><c:out value="${empty admin.modNm() ? '-' : admin.modNm()}"/> · ${admin.modDt().format(dtSec)}</dd>
                </dl>
            </div>
        </div>

        <div class="card mb-3" id="admin-roles">
            <div class="card-header">
                <h3 class="card-title">역할</h3>
                <c:if test="${not admin.self()}">
                    <div class="card-actions">
                        <form method="post" action="<c:url value='/ssr/admins/${admin.adminId()}/roles'/>" class="d-flex gap-2" id="role-add-form">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <select class="form-select form-select-sm" id="role-add-select" name="roleId" aria-label="추가할 역할" ${canUpdate ? '' : 'disabled'}>
                                <c:forEach var="r" items="${addableRoles}"><option value="${r.roleId()}"><c:out value="${r.roleNm()}"/></option></c:forEach>
                            </select>
                            <button type="submit" class="btn btn-sm btn-primary text-nowrap" id="btn-role-add"
                                    ${canUpdate and not empty addableRoles ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">역할 추가</button>
                        </form>
                    </div>
                </c:if>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="role-table">
                    <thead><tr><th>역할명</th><th>부여자</th><th>부여일시</th><th></th></tr></thead>
                    <tbody>
                    <c:forEach var="r" items="${admin.roles()}">
                        <tr data-role-id="${r.roleId()}">
                            <td><c:out value="${r.roleNm()}"/><c:if test="${r.useYn() == 'N'}"><span class="badge bg-secondary-lt ms-1">사용 안 함</span></c:if></td>
                            <td><c:out value="${empty r.regNm() ? '-' : r.regNm()}"/></td>
                            <td>${r.regDt().format(dtMin)}</td>
                            <td class="text-end">
                                <c:if test="${not admin.self() and admin.roles().size() >= 2}">
                                    <form method="post" action="<c:url value='/ssr/admins/${admin.adminId()}/roles/${r.roleId()}/delete'/>" class="d-inline" data-confirm="역할을 회수하시겠습니까?">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                        <button type="submit" class="btn btn-sm btn-ghost-danger btn-role-revoke" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">회수</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="card" id="login-history">
            <div class="card-header"><h3 class="card-title">최근 로그인 이력</h3></div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="history-table">
                    <thead><tr><th>일시</th><th>결과</th><th>방식</th><th>IP</th></tr></thead>
                    <tbody>
                    <c:forEach var="h" items="${histories}">
                        <tr><td>${h.regDt().format(dtSec)}</td><td><c:out value="${h.resultNm()}"/></td>
                            <td>${h.authTypeCd() == 'SESSION' ? '세션' : '토큰'}</td><td><c:out value="${h.ipAddr()}"/></td></tr>
                    </c:forEach>
                    <c:if test="${empty histories}"><tr><td colspan="4" class="text-secondary text-center">로그인 이력이 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </jsp:body>
</ui:layout>
