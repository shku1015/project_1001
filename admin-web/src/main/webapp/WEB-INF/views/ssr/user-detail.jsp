<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-USR-02 회원 상세 (③ JSP SSR). 개인정보는 마스킹 설정대로, [원문 보기](CMP-08)를 하면 이 화면에서만 원문을 보여 준다.
     상태 변경·삭제는 사유 입력창(CMP-07). ② jsp/user-detail.jsp(admin-user-api.js), ① UserDetailPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="u" value="${user}"/>
<c:set var="status" value="${u.statusCd()}"/>
<c:set var="withdrawn" value="${status == 'WITHDRAWN'}"/>
<c:set var="corporate" value="${u.userTypeCd() == 'CORPORATE'}"/>
<ui:layout title="사용자관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script>
          (function () {
            // 사유 입력창: 누른 버튼의 data-*로 제목·경고·보낼 곳을 정한다 (상태 변경 또는 삭제)
            var modal = document.getElementById('reason-modal');
            modal.addEventListener('show.bs.modal', function (e) {
              var b = e.relatedTarget;
              var form = document.getElementById('reason-form');
              document.getElementById('reason-title').textContent = b.dataset.title;
              var warning = document.getElementById('reason-warning');
              warning.textContent = b.dataset.warning || '';
              warning.hidden = !b.dataset.warning;
              form.action = b.dataset.action;
              form.statusCd.value = b.dataset.statusCd || '';
              form.statusCd.disabled = !b.dataset.statusCd;
              form.modDt.disabled = !b.dataset.statusCd;
              document.getElementById('reason-input').value = '';
            });
            // 원문 보기 사유: 기타면 직접 입력
            var reasonCd = document.getElementById('privacy-reasonCd');
            reasonCd.addEventListener('change', function () {
              document.getElementById('privacy-etc-wrap').hidden = reasonCd.value !== 'ETC';
            });
            var reset = document.getElementById('password-reset-form');
            if (reset) {
              reset.addEventListener('submit', function (e) {
                if (!confirm('비밀번호를 초기화하시겠습니까?')) { e.preventDefault(); }
              });
            }
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
                <div class="mt-1 small">이 창을 닫거나 화면을 옮기면 다시 볼 수 없습니다. 회원에게 전달하세요.</div>
            </div>
        </c:if>
        <c:if test="${withdrawn}"><div class="alert alert-info" role="note" id="withdrawn-note">탈퇴한 회원입니다. 정보를 수정하거나 상태를 바꿀 수 없습니다.</div></c:if>

        <div class="card mb-3" id="user-info">
            <div class="card-header">
                <h3 class="card-title" id="user-title"><c:out value="${u.loginId()}"/></h3>
                <div class="card-actions btn-list">
                    <a class="btn btn-sm" id="btn-list" href="<c:url value='/ssr/users'/>">목록</a>
                    <c:if test="${not empty u.maskedFields() and privacy == null}">
                        <button type="button" class="btn btn-sm" id="btn-privacy" data-bs-toggle="modal" data-bs-target="#privacy-modal"
                                ${canPrivacy ? '' : 'disabled'} title="${canPrivacy ? '' : noPerm}">원문 보기</button>
                    </c:if>
                    <c:if test="${not withdrawn}">
                        <c:choose>
                            <c:when test="${canEdit}"><a class="btn btn-sm" id="btn-edit" href="<c:url value='/ssr/users/${u.userId()}/edit'/>">수정</a></c:when>
                            <c:otherwise><button type="button" class="btn btn-sm" id="btn-edit" disabled title="${noPerm}">수정</button></c:otherwise>
                        </c:choose>
                        <c:set var="statusAction"><c:url value='/ssr/users/${u.userId()}/status'/></c:set>
                        <c:if test="${status == 'ACTIVE'}">
                            <button type="button" class="btn btn-sm" id="btn-suspend" data-bs-toggle="modal" data-bs-target="#reason-modal" data-action="${statusAction}"
                                    data-status-cd="SUSPENDED" data-title="회원 정지" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">정지</button>
                        </c:if>
                        <c:if test="${status == 'SUSPENDED'}">
                            <button type="button" class="btn btn-sm" id="btn-resume" data-bs-toggle="modal" data-bs-target="#reason-modal" data-action="${statusAction}"
                                    data-status-cd="ACTIVE" data-title="정지 해제" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">정지 해제</button>
                        </c:if>
                        <c:if test="${status == 'DORMANT'}">
                            <button type="button" class="btn btn-sm" id="btn-wake" data-bs-toggle="modal" data-bs-target="#reason-modal" data-action="${statusAction}"
                                    data-status-cd="ACTIVE" data-title="휴면 해제" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">휴면 해제</button>
                        </c:if>
                        <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-withdraw" data-bs-toggle="modal" data-bs-target="#reason-modal" data-action="${statusAction}"
                                data-status-cd="WITHDRAWN" data-title="강제 탈퇴" data-warning="강제 탈퇴는 되돌릴 수 없습니다."
                                ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">강제 탈퇴</button>
                        <form method="post" action="<c:url value='/ssr/users/${u.userId()}/password-reset'/>" class="d-inline" id="password-reset-form">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm" id="btn-password-reset" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">비밀번호 초기화</button>
                        </form>
                    </c:if>
                    <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-delete" data-bs-toggle="modal" data-bs-target="#reason-modal"
                            data-action="<c:url value='/ssr/users/${u.userId()}/delete'/>" data-title="회원 삭제"
                            ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">삭제</button>
                </div>
            </div>
            <div class="card-body">
                <h4 class="mb-2">기본 정보</h4>
                <dl class="row">
                    <dt class="col-sm-2">회원 구분</dt><dd class="col-sm-4" id="user-userType"><c:out value="${u.userTypeNm()}"/></dd>
                    <dt class="col-sm-2">로그인 아이디</dt><dd class="col-sm-4" id="user-loginId"><c:out value="${u.loginId()}"/></dd>
                    <dt class="col-sm-2">상태</dt><dd class="col-sm-4" id="user-status"><c:out value="${u.statusNm()}"/></dd>
                    <dt class="col-sm-2">가입 경로</dt><dd class="col-sm-4" id="user-joinPath">${u.joinPath() == 'ADMIN' ? '관리자 등록' : '사용자 서비스'}</dd>
                    <dt class="col-sm-2">가입일시</dt><dd class="col-sm-4">${u.joinDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">최근 로그인</dt><dd class="col-sm-4">${u.lastLoginDt() == null ? '-' : u.lastLoginDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">탈퇴일시</dt><dd class="col-sm-4" id="user-withdrawDt">${u.withdrawDt() == null ? '-' : u.withdrawDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">임시 비밀번호</dt><dd class="col-sm-4" id="user-pwdTempYn">${u.pwdTempYn() == 'Y' ? '예' : '아니오'}</dd>
                </dl>
                <h4 class="mb-2">개인정보 <c:if test="${privacy != null}"><span class="badge bg-red-lt ms-1" id="privacy-revealed">원문</span></c:if></h4>
                <dl class="row" id="user-privacy">
                    <dt class="col-sm-2">이름</dt><dd class="col-sm-4" id="user-userNm"><c:out value="${privacy != null ? privacy.userNm() : u.userNm()}"/></dd>
                    <dt class="col-sm-2">이메일</dt><dd class="col-sm-4" id="user-email"><c:out value="${privacy != null ? privacy.email() : u.email()}"/></dd>
                    <dt class="col-sm-2">휴대폰 번호</dt><dd class="col-sm-4" id="user-mobileNo"><ui:mobile value="${privacy != null ? privacy.mobileNo() : u.mobileNo()}"/></dd>
                    <dt class="col-sm-2">생년월일</dt><dd class="col-sm-4" id="user-birthDate"><c:out value="${privacy != null ? (privacy.birthDate() == null ? '-' : privacy.birthDate()) : (empty u.birthDate() ? '-' : u.birthDate())}"/></dd>
                </dl>
                <c:if test="${corporate}">
                    <h4 class="mb-2">소속 정보</h4>
                    <dl class="row">
                        <dt class="col-sm-2">소속 기업</dt><dd class="col-sm-4" id="user-company"><a href="<c:url value='/ssr/companies/${u.companyId()}'/>"><c:out value="${u.companyNm()}"/></a></dd>
                        <dt class="col-sm-2">부서 / 직위</dt><dd class="col-sm-4" id="user-dept"><c:out value="${empty u.deptNm() ? '-' : u.deptNm()}"/> / <c:out value="${empty u.positionNm() ? '-' : u.positionNm()}"/></dd>
                    </dl>
                </c:if>
                <h4 class="mb-2">관리 정보</h4>
                <dl class="row mb-0">
                    <dt class="col-sm-2">등록</dt><dd class="col-sm-4"><c:out value="${empty u.regNm() ? '-' : u.regNm()}"/> · ${u.regDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">수정</dt><dd class="col-sm-4"><c:out value="${empty u.modNm() ? '-' : u.modNm()}"/> · ${u.modDt().format(dtSec)}</dd>
                </dl>
            </div>
        </div>

        <div class="card" id="status-history">
            <div class="card-header"><h3 class="card-title">상태 변경 이력</h3></div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="history-table">
                    <thead><tr><th>변경일시</th><th>변경 전 → 후</th><th>사유</th><th>처리자</th></tr></thead>
                    <tbody>
                    <c:forEach var="h" items="${histories}">
                        <tr><td>${h.regDt().format(dtSec)}</td><td><c:out value="${h.beforeStatusNm()}"/> → <c:out value="${h.afterStatusNm()}"/></td>
                            <td><c:out value="${h.reason()}"/></td><td><c:out value="${empty h.regNm() ? '-' : h.regNm()}"/></td></tr>
                    </c:forEach>
                    <c:if test="${empty histories}"><tr><td colspan="4" class="text-secondary text-center">상태 변경 이력이 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="modal modal-blur fade" id="privacy-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" method="post" action="<c:url value='/ssr/users/${u.userId()}/privacy'/>" id="privacy-form">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="modal-header">
                        <h5 class="modal-title" id="privacy-title">개인정보 원문 보기</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
                        <p class="text-secondary">원문 보기는 감사로그에 남습니다.</p>
                        <label class="form-label required" for="privacy-reasonCd">열람 사유</label>
                        <select class="form-select" id="privacy-reasonCd" name="reasonCd" required>
                            <option value="">선택</option>
                            <c:forEach var="r" items="${privacyReasons}"><option value="${r.code()}"><c:out value="${r.codeNm()}"/></option></c:forEach>
                        </select>
                        <div class="mt-2" id="privacy-etc-wrap" hidden>
                            <label class="form-label required" for="privacy-reasonEtc">기타 사유</label>
                            <input class="form-control" id="privacy-reasonEtc" name="reasonEtc" maxlength="200">
                        </div>
                        <div id="privacy-message"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                        <button type="submit" class="btn btn-primary" id="btn-privacy-save">원문 보기</button>
                    </div>
                </form>
            </div>
        </div>

        <div class="modal modal-blur fade" id="reason-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" method="post" id="reason-form">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="statusCd"><input type="hidden" name="modDt" value="${u.modDt()}">
                    <div class="modal-header">
                        <h5 class="modal-title" id="reason-title">사유 입력</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
                        <div class="alert alert-warning" role="alert" id="reason-warning" hidden></div>
                        <label class="form-label required" for="reason-input">사유</label>
                        <textarea class="form-control" id="reason-input" name="reason" rows="3" maxlength="500" required></textarea>
                        <div id="reason-message"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                        <button type="submit" class="btn btn-primary" id="btn-reason-save">확인</button>
                    </div>
                </form>
            </div>
        </div>
    </jsp:body>
</ui:layout>
