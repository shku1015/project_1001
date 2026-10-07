<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ROL-03 역할 등록·수정 (② JSP + API). roleId가 있으면 수정. 마크업은 ③ ssr/role-form.jsp와 같다 --%>
<ui:layout title="역할" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-perm.js'/>"></script>
        <script src="<c:url value='/common/js/admin-role-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="role-page" data-page="form" data-role-id="${roleId}" hidden></div>
        <div class="card" id="role-form-card">
            <div class="card-header"><h3 class="card-title" id="role-form-title">${roleId == null ? '역할 등록' : '역할 수정'}</h3></div>
            <form id="role-form" novalidate>
                <div class="card-body">
                    <div class="mb-3">
                        <label class="form-label${roleId == null ? ' required' : ''}" for="role-roleCd">역할 코드</label>
                        <input class="form-control" id="role-roleCd" name="roleCd" maxlength="50" required placeholder="영문 대문자·숫자·_" ${roleId == null ? '' : 'readonly'}>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="role-roleNm">역할명</label>
                        <input class="form-control" id="role-roleNm" name="roleNm" maxlength="100" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="role-description">설명</label>
                        <textarea class="form-control" id="role-description" name="description" rows="3" maxlength="500"></textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">사용 여부</label>
                        <div>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="Y" id="role-useYn-Y" checked> 사용</label>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="N" id="role-useYn-N"> 사용 안 함</label>
                        </div>
                    </div>
                    <div id="role-message"></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <a class="btn" id="btn-cancel" href="<c:url value='/jsp/roles'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
