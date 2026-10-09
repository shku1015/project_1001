<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COM-02 기업 상세 (② JSP + API). admin-company-api.js가 채운다. 마크업은 ③ ssr/company-detail.jsp와 같다 --%>
<ui:layout title="기업정보관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-pager.js'/>"></script>
        <script src="<c:url value='/common/js/admin-form-kit.js'/>"></script>
        <script src="<c:url value='/common/js/admin-company-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="company-page" data-page="detail" data-company-id="${companyId}" hidden></div>
        <div class="card mb-3" id="company-info"></div>
        <div class="card" id="company-users"></div>

        <div class="modal modal-blur fade" id="reason-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" id="reason-form" novalidate>
                    <input type="hidden" name="statusCd">
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
