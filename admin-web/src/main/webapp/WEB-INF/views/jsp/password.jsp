<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-AUTH-02 비밀번호 변경 (② JSP + API).
     ③과 달리 서버는 임시 비밀번호 여부를 모르므로, 레이아웃 안에 그린 뒤 임시 비밀번호면 헤더·메뉴를 숨긴다 --%>
<ui:layout title="비밀번호 변경" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script>
          (async function () {
            var me = await AdminJsp.requireLogin({ allowTempPassword: true });
            if (me.pwdChangeRequired) {
              document.querySelector('aside.navbar-vertical').classList.add('d-none');
              document.querySelector('header.navbar').classList.add('d-none');
              document.querySelector('.breadcrumb').classList.add('d-none');
              document.getElementById('temp-notice').classList.remove('d-none');
              document.getElementById('temp-logout').classList.remove('d-none');
            }
            document.getElementById('temp-logout').addEventListener('click', async function () {
              await AdminApi.logout();
              location.href = AdminJsp.PREFIX + '/login?logout';
            });

            var form = document.getElementById('password-form');
            var message = document.getElementById('password-message');
            form.addEventListener('submit', async function (event) {
              event.preventDefault();
              message.innerHTML = '';
              if (form.newPassword.value !== form.newPasswordConfirm.value) {
                message.appendChild(AdminJsp.el('div', { 'class': 'alert alert-danger', role: 'alert' }, '새 비밀번호 확인이 일치하지 않습니다.'));
                return;
              }
              try {
                await AdminApi.changePassword(form.currentPassword.value, form.newPassword.value);
                AdminJsp.flash('비밀번호가 변경되었습니다');
                location.href = AdminJsp.PREFIX + '/';
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
                <div class="alert alert-warning d-none" role="status" id="temp-notice">임시 비밀번호로 로그인했습니다. 비밀번호를 바꿔야 다른 화면을 쓸 수 있습니다.</div>
                <form id="password-form" novalidate>
                    <div class="mb-3">
                        <label class="form-label" for="currentPassword">현재 비밀번호</label>
                        <input class="form-control" type="password" id="currentPassword" name="currentPassword" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="newPassword">새 비밀번호</label>
                        <input class="form-control" type="password" id="newPassword" name="newPassword" required maxlength="20">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="newPasswordConfirm">새 비밀번호 확인</label>
                        <input class="form-control" type="password" id="newPasswordConfirm" name="newPasswordConfirm" required maxlength="20">
                    </div>
                    <div id="password-message"></div>
                    <button type="submit" class="btn btn-primary">변경</button>
                </form>
            </div>
        </div>
        <div class="mt-3 text-center"><button type="button" class="btn btn-link d-none" id="temp-logout">로그아웃</button></div>
    </jsp:body>
</ui:layout>
