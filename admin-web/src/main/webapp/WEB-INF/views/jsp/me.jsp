<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-MY-01 내 정보 (② JSP + API) --%>
<ui:layout title="내 정보" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script>
          (async function () {
            var me = await AdminJsp.requireLogin();
            AdminJsp.showFlash();
            var form = document.getElementById('me-form');
            var message = document.getElementById('me-message');
            form.loginId.value = me.loginId;
            form.roles.value = me.roles.map(function (r) { return r.roleNm; }).join(', ');
            form.adminNm.value = me.adminNm;
            form.email.value = me.email;
            form.mobileNo.value = me.mobileNo || '';
            form.deptNm.value = me.deptNm || '';

            form.addEventListener('submit', async function (event) {
              event.preventDefault();
              message.innerHTML = '';
              try {
                await AdminApi.updateMe({
                  adminNm: form.adminNm.value.trim(),
                  email: form.email.value.trim(),
                  mobileNo: form.mobileNo.value.trim() || null,
                  deptNm: form.deptNm.value.trim() || null,
                  modDt: me.modDt
                });
                AdminJsp.flash('저장되었습니다');
                location.reload();
              } catch (e) {
                var text = e.fieldErrors && e.fieldErrors.length ? e.fieldErrors[0].message : e.message;
                message.appendChild(AdminJsp.el('div', { 'class': 'alert alert-danger', role: 'alert' }, text));
              }
            });
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="card">
            <div class="card-body">
                <form id="me-form" novalidate>
                    <div class="mb-3">
                        <label class="form-label" for="loginId">로그인 아이디</label>
                        <input class="form-control" type="text" id="loginId" name="loginId" readonly>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="roles">역할</label>
                        <input class="form-control" type="text" id="roles" name="roles" readonly>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="adminNm">이름</label>
                        <input class="form-control" type="text" id="adminNm" name="adminNm" maxlength="50" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label required" for="email">이메일</label>
                        <input class="form-control" type="email" id="email" name="email" maxlength="100" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="mobileNo">휴대폰 번호</label>
                        <input class="form-control" type="text" id="mobileNo" name="mobileNo" maxlength="11" placeholder="숫자만">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="deptNm">부서</label>
                        <input class="form-control" type="text" id="deptNm" name="deptNm" maxlength="100">
                    </div>
                    <div id="me-message"></div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">저장</button>
                        <a class="btn btn-outline-secondary" href="<c:url value='/jsp/password'/>">비밀번호 변경</a>
                    </div>
                </form>
            </div>
        </div>
    </jsp:body>
</ui:layout>
