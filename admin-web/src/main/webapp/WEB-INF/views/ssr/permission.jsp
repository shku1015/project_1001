<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-PRM-01 권한관리 (③ JSP SSR). 왼쪽 메뉴 트리, 오른쪽 선택한 메뉴의 역할 × 액션 표.
     ② jsp/permission.jsp(admin-permission-api.js), ① PermissionPage.tsx와 같은 마크업 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<ui:layout title="권한" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-role-perm.js'/>"></script>
        <script>
          (function () {
            var table = document.getElementById('grant-table');
            if (table) {
              AdminRolePerm.bind(table);
              AdminRolePerm.snapshot(table);
              document.getElementById('grant-form').addEventListener('submit', function (e) {
                var changes = AdminRolePerm.changes(table);
                if (!changes.length) {
                  e.preventDefault();
                  document.getElementById('grant-message').innerHTML = '<div class="alert alert-info" role="note">바뀐 내용이 없습니다.</div>';
                  return;
                }
                if (!confirm('역할 ' + changes.length + '개의 권한이 바뀝니다. 저장하시겠습니까?')) { e.preventDefault(); return; }
                this.roles.value = JSON.stringify(changes);
              });
            }
            // 저장하지 않은 변경이 있으면 다른 메뉴로 가기 전에 확인한다
            document.addEventListener('click', function (e) {
              var a = e.target.closest('a[href]');
              if (a && table && AdminRolePerm.changes(table).length && !a.closest('.dropdown-menu')
                  && !confirm('저장하지 않은 변경이 있습니다. 이동하시겠습니까?')) {
                e.preventDefault();
              }
            });
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <div class="mb-3 text-end"><a class="btn btn-sm" id="btn-effective" href="<c:url value='/ssr/permissions/admins'/>">관리자별 최종 권한</a></div>
        <div class="row row-cards">
            <div class="col-lg-4">
                <div class="card" id="perm-menu-card">
                    <div class="card-header"><h3 class="card-title">메뉴</h3></div>
                    <div class="card-body">
                        <ui:permMenuTree items="${tree}" selectedId="${grants != null ? grants.menu().menuId() : ''}"/>
                    </div>
                </div>
            </div>
            <div class="col-lg-8">
                <c:choose>
                    <c:when test="${grants == null}">
                        <div class="card"><div class="card-body text-secondary">왼쪽에서 화면 메뉴를 선택하세요.</div></div>
                    </c:when>
                    <c:otherwise>
                        <div class="card" id="grant-card">
                            <div class="card-header">
                                <h3 class="card-title" id="grant-title"><c:out value="${grants.menu().menuNm()}"/>
                                    <span class="text-secondary ms-1">(<c:out value="${grants.menu().menuCd()}"/>)</span></h3>
                            </div>
                            <div class="table-responsive">
                                <table class="table table-vcenter mb-0" id="grant-table">
                                    <thead>
                                    <tr>
                                        <th>역할</th>
                                        <c:forEach var="a" items="${grants.menu().actions()}">
                                            <th class="text-center">${a == 'READ' ? '조회' : a == 'CREATE' ? '등록' : a == 'UPDATE' ? '수정' : a == 'DELETE' ? '삭제' : a == 'EXCEL' ? '엑셀' : '개인정보'}</th>
                                        </c:forEach>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="r" items="${grants.roles()}">
                                        <c:set var="system" value="${r.systemYn() == 'Y'}"/>
                                        <tr class="grant-row${r.useYn() == 'N' ? ' text-secondary' : ''}" data-role-id="${r.roleId()}">
                                            <td>
                                                <c:out value="${r.roleNm()}"/><c:if test="${system}"> &#x1F512;</c:if>
                                                <c:if test="${r.mine()}"><span class="badge bg-blue-lt ms-1">내 역할</span></c:if>
                                                <c:if test="${r.useYn() == 'N'}"><span class="badge bg-secondary-lt ms-1">사용 안 함</span></c:if>
                                            </td>
                                            <c:forEach var="a" items="${grants.menu().actions()}">
                                                <td class="text-center">
                                                    <c:choose>
                                                        <c:when test="${system}"><span class="perm-all" title="모든 권한">■</span></c:when>
                                                        <c:otherwise>
                                                            <c:set var="label" value="${a == 'READ' ? '조회' : a == 'CREATE' ? '등록' : a == 'UPDATE' ? '수정' : a == 'DELETE' ? '삭제' : a == 'EXCEL' ? '엑셀' : '개인정보'}"/>
                                                            <input type="checkbox" class="form-check-input m-0 perm-check" id="grant-${r.roleId()}-${a}" data-action="${a}"
                                                                   aria-label="<c:out value='${r.roleNm()}'/> ${label}"
                                                                   ${r.granted().contains(a) ? 'checked' : ''}
                                                                   ${canUpdate and r.editable() and grants.grantableActions().contains(a) ? '' : 'disabled'}>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </c:forEach>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <div class="card-footer">
                                <div id="grant-message"></div>
                                <form method="post" action="<c:url value='/ssr/permissions/menus/${grants.menu().menuId()}'/>" id="grant-form" class="text-end">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <input type="hidden" name="roles">
                                    <button type="submit" class="btn btn-primary" id="btn-grant-save"
                                            ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">저장</button>
                                </form>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </jsp:body>
</ui:layout>
