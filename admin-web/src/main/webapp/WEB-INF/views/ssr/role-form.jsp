<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-ROL-03 역할 등록·수정 (③ JSP SSR). role이 있으면 수정. ② jsp/role-form.jsp, ① RoleFormPage.tsx와 같은 마크업 --%>
<c:set var="isEdit" value="${role != null}"/>
<c:set var="useYn" value="${not empty param.useYn ? param.useYn : (isEdit ? role.useYn() : 'Y')}"/>
<ui:layout title="역할" mode="ssr">
    <jsp:attribute name="scripts">
        <c:if test="${isEdit}">
            <script>
              // 사용 안 함으로 바꾸면 그 역할을 가진 관리자의 권한이 빠진다 (BR-07)
              document.getElementById('role-form').addEventListener('submit', function (e) {
                var cnt = ${role.adminCnt()};
                if ('${role.useYn()}' === 'Y' && this.useYn.value === 'N' && cnt > 0
                    && !confirm('이 역할의 권한이 관리자 ' + cnt + '명에게서 빠집니다. 저장하시겠습니까?')) {
                  e.preventDefault();
                }
              });
            </script>
        </c:if>
    </jsp:attribute>
    <jsp:body>
        <div class="card" id="role-form-card">
            <div class="card-header"><h3 class="card-title" id="role-form-title">${isEdit ? '역할 수정' : '역할 등록'}</h3></div>
            <c:set var="formAction" value="/ssr/roles"/>
            <c:if test="${isEdit}"><c:set var="formAction" value="/ssr/roles/${role.roleId()}/update"/></c:if>
            <form method="post" action="<c:url value='${formAction}'/>" id="role-form" novalidate>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <c:if test="${isEdit}"><input type="hidden" name="modDt" value="${role.modDt()}"></c:if>
                <div class="card-body">
                    <div class="mb-3">
                        <label class="form-label${isEdit ? '' : ' required'}" for="role-roleCd">역할 코드</label>
                        <c:choose>
                            <c:when test="${isEdit}"><input class="form-control" id="role-roleCd" readonly value="<c:out value='${role.roleCd()}'/>"></c:when>
                            <c:otherwise><input class="form-control" id="role-roleCd" name="roleCd" maxlength="50" required placeholder="영문 대문자·숫자·_"
                                                value="<c:out value='${param.roleCd}'/>"></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="role-roleNm">역할명</label>
                        <input class="form-control" id="role-roleNm" name="roleNm" maxlength="100" required
                               value="<c:out value='${not empty param.roleNm ? param.roleNm : (isEdit ? role.roleNm() : "")}'/>">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="role-description">설명</label>
                        <textarea class="form-control" id="role-description" name="description" rows="3" maxlength="500"><c:out value="${not empty param.description ? param.description : (isEdit ? role.description() : '')}"/></textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">사용 여부</label>
                        <div>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="Y" id="role-useYn-Y" ${useYn == 'Y' ? 'checked' : ''}> 사용</label>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="N" id="role-useYn-N" ${useYn == 'N' ? 'checked' : ''}> 사용 안 함</label>
                        </div>
                    </div>
                    <div id="role-message"><c:if test="${not empty formError}"><div class="alert alert-danger" role="alert"><c:out value="${formError}"/></div></c:if></div>
                </div>
                <div class="card-footer d-flex gap-2">
                    <button type="submit" class="btn btn-primary" id="btn-save">저장</button>
                    <c:set var="cancelUrl" value="/ssr/roles"/>
                    <c:if test="${isEdit}"><c:set var="cancelUrl" value="/ssr/roles/${role.roleId()}"/></c:if>
                    <a class="btn" id="btn-cancel" href="<c:url value='${cancelUrl}'/>">취소</a>
                </div>
            </form>
        </div>
    </jsp:body>
</ui:layout>
