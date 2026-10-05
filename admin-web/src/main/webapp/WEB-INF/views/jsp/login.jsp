<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-AUTH-01 로그인 (② JSP + API). 화면 구조는 ③ 로그인과 같다 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <ui:head title="로그인"/>
</head>
<body>
<div class="page page-center login-page">
    <div class="container container-tight py-4">
        <div class="text-center mb-4"><h1>관리자 서비스</h1></div>
        <div class="card card-md">
            <div class="card-body">
                <h2 class="h2 text-center mb-4">로그인</h2>
                <form id="login-form" novalidate>
                    <div class="mb-3">
                        <label class="form-label" for="loginId">아이디</label>
                        <input class="form-control" type="text" id="loginId" name="loginId" required autofocus>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="password">비밀번호</label>
                        <input class="form-control" type="password" id="password" name="password" required>
                    </div>
                    <div id="login-message"></div>
                    <button type="submit" class="btn btn-primary w-100">로그인</button>
                </form>
            </div>
        </div>
    </div>
</div>
<script src="<c:url value='/common/js/admin-api.js'/>"></script>
<script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
<script>
  (function () {
    var message = document.getElementById('login-message');
    if (location.search.indexOf('logout') >= 0) {
      message.appendChild(AdminJsp.el('div', { 'class': 'alert alert-info', role: 'status' }, '로그아웃되었습니다.'));
    }
    document.getElementById('login-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      message.innerHTML = '';
      try {
        var token = await AdminApi.login(this.loginId.value, this.password.value);
        location.href = AdminJsp.PREFIX + (token.pwdChangeRequired ? '/password' : '/');
      } catch (e) {
        message.appendChild(AdminJsp.el('div', { 'class': 'alert alert-danger', role: 'alert' }, e.message));
      }
    });
  })();
</script>
</body>
</html>
