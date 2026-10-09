<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COM-02 기업 상세 (③ JSP SSR). 기업 정보, 소속 회원(최근 10명), 정지·정지 해제는 사유 입력창(CMP-07).
     ② jsp/company-detail.jsp(admin-company-api.js), ① CompanyDetailPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="co" value="${company}"/>
<ui:layout title="기업정보관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script>
          (function () {
            // 사유 입력창: 누른 버튼의 data-status-cd·data-title로 채운다
            var modal = document.getElementById('reason-modal');
            if (modal) {
              modal.addEventListener('show.bs.modal', function (e) {
                var b = e.relatedTarget;
                document.getElementById('reason-title').textContent = b.dataset.title;
                document.getElementById('reason-form').statusCd.value = b.dataset.statusCd;
                document.getElementById('reason-input').value = '';
              });
            }
            var del = document.getElementById('delete-form');
            if (del) {
              del.addEventListener('submit', function (e) {
                if (!confirm('기업을 삭제하시겠습니까?')) { e.preventDefault(); }
              });
            }
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="card mb-3" id="company-info">
            <div class="card-header">
                <h3 class="card-title" id="company-title"><c:out value="${co.companyNm()}"/></h3>
                <div class="card-actions btn-list">
                    <a class="btn btn-sm" id="btn-list" href="<c:url value='/ssr/companies'/>">목록</a>
                    <c:choose>
                        <c:when test="${canUpdate}"><a class="btn btn-sm" id="btn-edit" href="<c:url value='/ssr/companies/${co.companyId()}/edit'/>">수정</a></c:when>
                        <c:otherwise><button type="button" class="btn btn-sm" id="btn-edit" disabled title="${noPerm}">수정</button></c:otherwise>
                    </c:choose>
                    <c:choose>
                        <c:when test="${co.statusCd() == 'ACTIVE'}">
                            <button type="button" class="btn btn-sm btn-ghost-danger" id="btn-suspend" data-bs-toggle="modal" data-bs-target="#reason-modal"
                                    data-status-cd="SUSPENDED" data-title="기업 정지" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">정지</button>
                        </c:when>
                        <c:otherwise>
                            <button type="button" class="btn btn-sm" id="btn-resume" data-bs-toggle="modal" data-bs-target="#reason-modal"
                                    data-status-cd="ACTIVE" data-title="정지 해제" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">정지 해제</button>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${co.memberCnt() == 0}">
                        <form method="post" action="<c:url value='/ssr/companies/${co.companyId()}/delete'/>" class="d-inline" id="delete-form">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-sm btn-ghost-danger" id="btn-delete" ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">삭제</button>
                        </form>
                    </c:if>
                </div>
            </div>
            <div class="card-body">
                <c:if test="${co.memberCnt() > 0}"><div class="alert alert-info" role="note" id="delete-note">소속 회원이 있어 삭제할 수 없습니다.</div></c:if>
                <dl class="row mb-0">
                    <dt class="col-sm-2">기업명</dt><dd class="col-sm-4" id="company-companyNm"><c:out value="${co.companyNm()}"/></dd>
                    <dt class="col-sm-2">사업자등록번호</dt><dd class="col-sm-4" id="company-bizRegNo">${co.bizRegNo().substring(0, 3)}-${co.bizRegNo().substring(3, 5)}-${co.bizRegNo().substring(5)}</dd>
                    <dt class="col-sm-2">대표자명</dt><dd class="col-sm-4" id="company-ceoNm"><c:out value="${co.ceoNm()}"/></dd>
                    <dt class="col-sm-2">상태</dt><dd class="col-sm-4" id="company-status"><c:out value="${co.statusNm()}"/></dd>
                    <dt class="col-sm-2">업태</dt><dd class="col-sm-4" id="company-bizType"><c:out value="${empty co.bizType() ? '-' : co.bizType()}"/></dd>
                    <dt class="col-sm-2">종목</dt><dd class="col-sm-4" id="company-bizItem"><c:out value="${empty co.bizItem() ? '-' : co.bizItem()}"/></dd>
                    <dt class="col-sm-2">대표 전화번호</dt><dd class="col-sm-4" id="company-telNo"><c:out value="${empty co.telNo() ? '-' : co.telNo()}"/></dd>
                    <dt class="col-sm-2">소속 회원 수</dt><dd class="col-sm-4" id="company-memberCnt">${co.memberCnt()}</dd>
                    <dt class="col-sm-2">주소</dt>
                    <dd class="col-sm-10" id="company-addr"><c:choose><c:when test="${empty co.addr()}">-</c:when><c:otherwise>(<c:out value="${co.zipCd()}"/>) <c:out value="${co.addr()}"/> <c:out value="${co.addrDtl()}"/></c:otherwise></c:choose></dd>
                    <dt class="col-sm-2">등록</dt><dd class="col-sm-4"><c:out value="${empty co.regNm() ? '-' : co.regNm()}"/> · ${co.regDt().format(dtSec)}</dd>
                    <dt class="col-sm-2">수정</dt><dd class="col-sm-4"><c:out value="${empty co.modNm() ? '-' : co.modNm()}"/> · ${co.modDt().format(dtSec)}</dd>
                </dl>
            </div>
        </div>

        <div class="card" id="company-users">
            <div class="card-header">
                <h3 class="card-title">소속 회원 <span class="text-secondary" id="users-total">(${users.totalCount()}명)</span></h3>
                <div class="card-actions"><a class="btn btn-sm" id="btn-users-all" href="<c:url value='/ssr/users?companyId=${co.companyId()}'/>">전체 보기</a></div>
            </div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="users-table">
                    <thead><tr><th>로그인 아이디</th><th>이름</th><th>부서 / 직위</th><th>상태</th><th>가입일시</th></tr></thead>
                    <tbody>
                    <c:forEach var="u" items="${users.items()}">
                        <tr>
                            <td><a href="<c:url value='/ssr/users/${u.userId()}'/>"><c:out value="${u.loginId()}"/></a></td>
                            <td><c:out value="${u.userNm()}"/></td>
                            <td><c:out value="${empty u.deptNm() ? '-' : u.deptNm()}"/> / <c:out value="${empty u.positionNm() ? '-' : u.positionNm()}"/></td>
                            <td><c:out value="${u.statusNm()}"/></td>
                            <td>${u.joinDt().format(dtMin)}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty users.items()}"><tr><td colspan="5" class="text-secondary text-center">소속 회원이 없습니다</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="modal modal-blur fade" id="reason-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" method="post" action="<c:url value='/ssr/companies/${co.companyId()}/status'/>" id="reason-form">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="statusCd"><input type="hidden" name="modDt" value="${co.modDt()}">
                    <div class="modal-header">
                        <h5 class="modal-title" id="reason-title">사유 입력</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
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
