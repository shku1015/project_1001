<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-USR-02 회원 상세 (② JSP + API). admin-user-api.js가 채운다. 마크업은 ③ ssr/user-detail.jsp와 같다 --%>
<ui:layout title="사용자관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script src="<c:url value='/common/js/admin-user-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="user-page" data-page="detail" data-user-id="${userId}" hidden></div>
        <div id="temp-password-area"></div>
        <div id="withdrawn-area"></div>
        <div class="card mb-3" id="user-info"></div>

        <div class="card" id="status-history">
            <div class="card-header"><h3 class="card-title">상태 변경 이력</h3></div>
            <div class="table-responsive">
                <table class="table table-vcenter mb-0" id="history-table">
                    <thead><tr><th>변경일시</th><th>변경 전 → 후</th><th>사유</th><th>처리자</th></tr></thead>
                    <tbody id="history-tbody">
                    </tbody>
                </table>
            </div>
        </div>

        <div class="modal modal-blur fade" id="privacy-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" id="privacy-form" novalidate>
                    <div class="modal-header">
                        <h5 class="modal-title" id="privacy-title">개인정보 원문 보기</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
                        <p class="text-secondary">원문 보기는 감사로그에 남습니다.</p>
                        <label class="form-label required" for="privacy-reasonCd">열람 사유</label>
                        <select class="form-select" id="privacy-reasonCd" name="reasonCd" required>
                            <option value="">선택</option>
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
                <form class="modal-content" id="reason-form" novalidate>
                    <input type="hidden" name="statusCd"><input type="hidden" name="mode">
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
