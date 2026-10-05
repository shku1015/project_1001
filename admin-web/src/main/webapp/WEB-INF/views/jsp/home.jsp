<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-HOME 홈 (② JSP + API). 데이터는 /api/v1/auth/me 로 채운다 --%>
<ui:layout title="홈" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script>
          (async function () {
            var me = await AdminJsp.requireLogin();
            AdminJsp.showFlash();
            document.getElementById('home-name').textContent = me.adminNm;
            document.getElementById('home-roles').textContent =
                me.roles.length ? me.roles.map(function (r) { return r.roleNm; }).join(', ') : '-';
            document.getElementById('home-last-login').textContent = me.lastLoginDt
                ? AdminJsp.formatDateTime(me.lastLoginDt) + ' (' + me.lastLoginIp + ')'
                : '이전 로그인 기록이 없습니다';
            document.getElementById('home-pwd-changed').textContent = AdminJsp.formatDate(me.pwdChangedDt);

            var shortcuts = document.getElementById('home-shortcuts');
            var pages = AdminJsp.pageMenus(me.menus).slice(0, 6);
            pages.forEach(function (m) {
              shortcuts.appendChild(AdminJsp.el('a', { 'class': 'btn btn-outline-primary', href: AdminJsp.PREFIX + m.menuUrl }, m.menuNm));
            });
            if (!pages.length) {
              shortcuts.appendChild(AdminJsp.el('p', { 'class': 'text-secondary mb-0' },
                  '사용 가능한 메뉴가 없습니다. 관리자에게 권한을 요청하세요'));
            }
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="row row-cards">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header"><h3 class="card-title">내 정보</h3></div>
                    <div class="card-body">
                        <dl class="row mb-0">
                            <dt class="col-5">이름</dt><dd class="col-7" id="home-name"></dd>
                            <dt class="col-5">역할</dt><dd class="col-7" id="home-roles"></dd>
                            <dt class="col-5">마지막 로그인</dt><dd class="col-7" id="home-last-login"></dd>
                            <dt class="col-5">비밀번호 변경일</dt><dd class="col-7" id="home-pwd-changed"></dd>
                        </dl>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header"><h3 class="card-title">바로가기</h3></div>
                    <div class="card-body shortcut-list d-flex flex-wrap gap-2" id="home-shortcuts"></div>
                </div>
            </div>
        </div>
    </jsp:body>
</ui:layout>
