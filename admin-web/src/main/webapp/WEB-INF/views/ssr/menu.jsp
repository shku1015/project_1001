<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-MNU-01 메뉴관리 (③ JSP SSR). 왼쪽 트리(드래그·▲▼, 확정 시 저장), 오른쪽 상세·입력 폼.
     권한이 없는 버튼은 비활성, 상태 때문에 할 수 없는 작업은 숨긴다 (docs/05-ia-screens.md 4.2) --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<c:set var="sel" value="${selected}"/>
<c:set var="isCreate" value="${mode == 'create'}"/>
<c:set var="isProtected" value="${sel != null and (sel.systemYn() == 'Y' or sel.boardAutoYn() == 'Y')}"/>
<c:set var="parentForCreate" value="${createParent}"/>
<c:set var="createDepth" value="${parentForCreate == null ? 1 : parentForCreate.depth() + 1}"/>
<c:set var="formType" value="${isCreate ? (not empty param.menuTypeCd ? param.menuTypeCd : (createDepth == 3 ? 'PAGE' : 'FOLDER')) : sel.menuTypeCd()}"/>
<c:set var="formDepth" value="${isCreate ? createDepth : sel.depth()}"/>
<c:set var="formAction" value="${isCreate ? '/ssr/menus' : '/ssr/menus/' += sel.menuId() += '/update'}"/>
<ui:layout title="메뉴관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/webjars/sortablejs/${sortablejsVersion}/Sortable.min.js'/>"></script>
        <script src="<c:url value='/common/js/admin-menu-tree.js'/>"></script>
        <script>
          (function () {
            var card = document.getElementById('menu-tree-card');
            var selected = '${sel != null ? sel.menuId() : ""}';
            // 저장하지 않은 순서 변경이 있으면 트리 구조를 바꾸는 작업(추가·이동·삭제)을 막는다
            AdminMenuTree.init({
              root: card,
              editable: ${canUpdate},
              onChange: function (dirty) {
                document.querySelectorAll('[data-structure-action]').forEach(function (el) {
                  if (el.dataset.noPermission !== 'true') {
                    el.classList.toggle('disabled', dirty);
                    if ('disabled' in el) { el.disabled = dirty; } else { el.setAttribute('aria-disabled', String(dirty)); }
                  }
                });
              }
            });
            var up = document.getElementById('btn-up');
            var down = document.getElementById('btn-down');
            if (up) { up.addEventListener('click', function () { AdminMenuTree.move(selected, -1); }); }
            if (down) { down.addEventListener('click', function () { AdminMenuTree.move(selected, 1); }); }
            document.getElementById('btn-order-reset').addEventListener('click', AdminMenuTree.reset);
            document.getElementById('order-form').addEventListener('submit', function (e) {
              if (!confirm('메뉴 순서를 저장하시겠습니까?')) { e.preventDefault(); return; }
              this.orders.value = JSON.stringify(AdminMenuTree.collectOrders());
            });
            // 순서를 바꾼 채 다른 메뉴를 고르면 확인한다
            document.addEventListener('click', function (e) {
              var a = e.target.closest('a[href]');
              if (a && AdminMenuTree.isDirty() && !a.closest('.dropdown-menu')
                  && !confirm('저장하지 않은 순서 변경이 있습니다. 이동하시겠습니까?')) {
                e.preventDefault();
              }
            });
            var deleteForm = document.getElementById('delete-form');
            if (deleteForm) {
              deleteForm.addEventListener('submit', function (e) {
                if (!confirm('메뉴를 삭제하시겠습니까? 이 메뉴의 권한과 역할 매핑도 함께 지워집니다.')) { e.preventDefault(); }
              });
            }
            // 등록: 메뉴 종류에 따라 URL·사용 액션 칸을 보이고 숨긴다
            document.querySelectorAll('input[name="menuTypeCd"]').forEach(function (r) {
              r.addEventListener('change', function () {
                document.querySelectorAll('[data-page-only]').forEach(function (el) { el.hidden = r.value !== 'PAGE'; });
              });
            });
          })();
        </script>
    </jsp:attribute>
    <jsp:body>
        <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
        <div class="row row-cards">
            <%-- ===== 메뉴 트리 ===== --%>
            <div class="col-lg-5">
                <div class="card" id="menu-tree-card">
                    <div class="card-header">
                        <h3 class="card-title">메뉴</h3>
                        <div class="card-actions">
                            <c:choose>
                                <c:when test="${canCreate}"><a class="btn btn-primary btn-sm" id="btn-root-create" data-structure-action href="<c:url value='/ssr/menus?create=root'/>">최상위 메뉴 추가</a></c:when>
                                <c:otherwise><button type="button" class="btn btn-primary btn-sm" id="btn-root-create" disabled title="${noPerm}" data-no-permission="true">최상위 메뉴 추가</button></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="card-body">
                        <form method="post" action="<c:url value='/ssr/menus/order'/>" id="order-form">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="orders">
                            <input type="hidden" name="selected" value="${sel != null ? sel.menuId() : ''}">
                            <div id="order-bar" hidden>
                                <div class="alert alert-warning d-flex align-items-center gap-2 py-2">
                                    <span class="me-auto">저장하지 않은 순서 변경이 있습니다</span>
                                    <button type="submit" class="btn btn-primary btn-sm" id="btn-order-save">순서 저장</button>
                                    <button type="button" class="btn btn-sm" id="btn-order-reset">되돌리기</button>
                                </div>
                            </div>
                        </form>
                        <ui:menuAdminTree items="${tree}" parentId="" selectedId="${sel != null ? sel.menuId() : ''}"/>
                    </div>
                    <div class="card-footer d-flex gap-2">
                        <c:set var="arrowOff" value="${not canUpdate or sel == null}"/>
                        <button type="button" class="btn btn-sm" id="btn-up" ${arrowOff ? 'disabled' : ''} title="${not canUpdate ? noPerm : '선택한 메뉴를 위로'}">▲ 위로</button>
                        <button type="button" class="btn btn-sm" id="btn-down" ${arrowOff ? 'disabled' : ''} title="${not canUpdate ? noPerm : '선택한 메뉴를 아래로'}">▼ 아래로</button>
                    </div>
                </div>
            </div>

            <%-- ===== 메뉴 상세·입력 ===== --%>
            <div class="col-lg-7">
                <c:choose>
                    <c:when test="${mode == null}">
                        <div class="card"><div class="card-body text-secondary">왼쪽에서 메뉴를 선택하세요.</div></div>
                    </c:when>
                    <c:otherwise>
                        <div class="card" id="menu-detail-card">
                            <div class="card-header">
                                <h3 class="card-title">${isCreate ? '메뉴 등록' : '메뉴 상세'}</h3>
                            </div>
                            <form method="post" id="menu-form"
                                  action="<c:url value='${formAction}'/>">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <c:if test="${isCreate and parentForCreate != null}"><input type="hidden" name="parentMenuId" value="${parentForCreate.menuId()}"></c:if>
                                <c:if test="${not isCreate}"><input type="hidden" name="modDt" value="${sel.modDt()}"></c:if>
                                <div class="card-body">
                                    <c:if test="${isProtected}">
                                        <div class="alert alert-info" role="note">${sel.systemYn() == 'Y' ? '시스템 메뉴' : '게시판별 자동 메뉴'}는 메뉴명·아이콘만 바꿀 수 있습니다.</div>
                                    </c:if>
                                    <div class="mb-3">
                                        <label class="form-label" for="menu-parent">상위 메뉴</label>
                                        <input class="form-control" id="menu-parent" readonly
                                               value="<c:out value='${isCreate ? (parentForCreate != null ? parentForCreate.menuNm() : "-") : (sel.parentMenuNm() != null ? sel.parentMenuNm() : "-")}'/>">
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label${isCreate ? ' required' : ''}" for="menu-menuCd">메뉴 코드</label>
                                        <c:choose>
                                            <c:when test="${isCreate}"><input class="form-control" id="menu-menuCd" name="menuCd" maxlength="50" required pattern="[A-Z0-9_]+" placeholder="영문 대문자·숫자·_" value="<c:out value='${param.menuCd}'/>"></c:when>
                                            <c:otherwise><input class="form-control" id="menu-menuCd" readonly value="<c:out value='${sel.menuCd()}'/>"></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label required" for="menu-menuNm">메뉴명</label>
                                        <input class="form-control" id="menu-menuNm" name="menuNm" maxlength="100" required
                                               value="<c:out value='${not empty param.menuNm ? param.menuNm : (sel != null ? sel.menuNm() : "")}'/>">
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label">종류</label>
                                        <c:choose>
                                            <c:when test="${isCreate}">
                                                <div>
                                                    <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="menuTypeCd" value="FOLDER" id="menu-type-FOLDER" ${formType == 'FOLDER' ? 'checked' : ''} ${createDepth == 3 ? 'disabled' : ''}> 폴더</label>
                                                    <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="menuTypeCd" value="PAGE" id="menu-type-PAGE" ${formType == 'PAGE' ? 'checked' : ''}> 화면</label>
                                                </div>
                                            </c:when>
                                            <c:otherwise><div class="form-control-plaintext" id="menu-type">${sel.menuTypeCd() == 'FOLDER' ? '폴더' : '화면'}</div></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div data-page-only ${formType == 'PAGE' ? '' : 'hidden'}>
                                        <div class="mb-3">
                                            <label class="form-label required" for="menu-menuUrl">URL</label>
                                            <input class="form-control" id="menu-menuUrl" name="menuUrl" maxlength="200" placeholder="/로 시작"
                                                   ${isProtected ? 'readonly' : ''}
                                                   value="<c:out value='${not empty param.menuUrl ? param.menuUrl : (sel != null ? sel.menuUrl() : "")}'/>">
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">사용 액션</label>
                                            <div>
                                                <c:forEach var="a" items="${allActions}">
                                                    <c:set var="checked" value="${a == 'READ' or (not empty paramValues.actions ? fn:contains(fn:join(paramValues.actions, ','), a.name()) : (sel != null and sel.actions().contains(a)))}"/>
                                                    <label class="form-check form-check-inline">
                                                        <input class="form-check-input" type="checkbox" name="actions" value="${a}" id="action-${a}"
                                                               ${checked ? 'checked' : ''} ${a == 'READ' or isProtected ? 'disabled' : ''}>
                                                        ${a == 'READ' ? '조회' : a == 'CREATE' ? '등록' : a == 'UPDATE' ? '수정' : a == 'DELETE' ? '삭제' : a == 'EXCEL' ? '엑셀' : '개인정보열람'}
                                                    </label>
                                                    <c:if test="${isProtected and checked and a != 'READ'}"><input type="hidden" name="actions" value="${a}"></c:if>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    </div>
                                    <c:if test="${formDepth == 1}">
                                        <div class="mb-3">
                                            <label class="form-label" for="menu-icon">아이콘</label>
                                            <input class="form-control" id="menu-icon" name="icon" maxlength="50"
                                                   value="<c:out value='${not empty param.icon ? param.icon : (sel != null ? sel.icon() : "")}'/>">
                                        </div>
                                    </c:if>
                                    <div class="mb-3">
                                        <label class="form-label">사용 여부</label>
                                        <c:set var="useYn" value="${not empty param.useYn ? param.useYn : (sel != null ? sel.useYn() : 'Y')}"/>
                                        <div>
                                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="Y" ${useYn == 'Y' ? 'checked' : ''} ${isProtected ? 'disabled' : ''}> 사용</label>
                                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="N" ${useYn == 'N' ? 'checked' : ''} ${isProtected ? 'disabled' : ''}> 사용 안 함</label>
                                            <c:if test="${isProtected}"><input type="hidden" name="useYn" value="${sel.useYn()}"></c:if>
                                        </div>
                                    </div>
                                    <c:if test="${not empty pendingRevoke}">
                                        <div class="alert alert-warning" role="alert" id="revoke-warning">
                                            ${fn:length(pendingRevoke)}개 역할에서 권한이 회수됩니다:
                                            <c:forEach var="r" items="${pendingRevoke}" varStatus="s"><c:out value="${r.roleNm()}"/> (<c:forEach var="act" items="${r.actions()}" varStatus="as">${act}<c:if test="${!as.last}">, </c:if></c:forEach>)<c:if test="${!s.last}">, </c:if></c:forEach>
                                        </div>
                                    </c:if>
                                    <c:if test="${not empty formError}"><div class="alert alert-danger" role="alert"><c:out value="${formError}"/></div></c:if>
                                </div>
                                <div class="card-footer d-flex flex-wrap gap-2">
                                    <c:set var="canSave" value="${isCreate ? canCreate : canUpdate}"/>
                                    <c:choose>
                                        <c:when test="${not empty pendingRevoke}"><button type="submit" class="btn btn-warning" name="confirmRevoke" value="Y">회수하고 저장</button></c:when>
                                        <c:otherwise><button type="submit" class="btn btn-primary" id="btn-save" ${canSave ? '' : 'disabled'} title="${canSave ? '' : noPerm}">저장</button></c:otherwise>
                                    </c:choose>
                                    <c:if test="${not isCreate}">
                                        <%-- 하위 메뉴 추가: 폴더이고 3단계 미만일 때 --%>
                                        <c:if test="${sel.menuTypeCd() == 'FOLDER' and sel.depth() < 3}">
                                            <c:choose>
                                                <c:when test="${canCreate}"><a class="btn" id="btn-child-create" data-structure-action href="<c:url value='/ssr/menus?create=${sel.menuId()}'/>">하위 메뉴 추가</a></c:when>
                                                <c:otherwise><button type="button" class="btn" id="btn-child-create" disabled title="${noPerm}" data-no-permission="true">하위 메뉴 추가</button></c:otherwise>
                                            </c:choose>
                                        </c:if>
                                        <c:if test="${not isProtected}">
                                            <button type="button" class="btn" id="btn-move" data-bs-toggle="modal" data-bs-target="#move-modal" data-structure-action
                                                    data-no-permission="${!canUpdate}" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">상위 메뉴 변경</button>
                                        </c:if>
                                    </c:if>
                                </div>
                            </form>
                            <c:if test="${not isCreate and not isProtected and not hasChildren}">
                                <form method="post" id="delete-form" class="card-footer border-top-0 pt-0"
                                      action="<c:url value='/ssr/menus/${sel.menuId()}/delete'/>">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <button type="submit" class="btn btn-outline-danger" id="btn-delete" data-structure-action
                                            data-no-permission="${!canDelete}" ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">삭제</button>
                                </form>
                            </c:if>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- 상위 메뉴 변경 모달 --%>
        <c:if test="${sel != null and not isProtected}">
            <div class="modal modal-blur fade" id="move-modal" tabindex="-1">
                <div class="modal-dialog modal-dialog-centered">
                    <form class="modal-content" method="post" action="<c:url value='/ssr/menus/${sel.menuId()}/move'/>">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="hidden" name="modDt" value="${sel.modDt()}">
                        <div class="modal-header">
                            <h5 class="modal-title">상위 메뉴 변경</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                        </div>
                        <div class="modal-body">
                            <label class="form-label" for="move-parent">옮길 위치</label>
                            <select class="form-select" id="move-parent" name="parentMenuId">
                                <option value="">(최상위)</option>
                                <c:forEach var="f" items="${moveTargets}">
                                    <option value="${f.menuId()}" ${sel.parentMenuId() == f.menuId() ? 'selected' : ''}>${f.depth() == 2 ? '└ ' : ''}<c:out value="${f.menuNm()}"/></option>
                                </c:forEach>
                            </select>
                            <p class="text-secondary mt-2 mb-0">옮긴 메뉴는 새 상위 메뉴의 맨 뒤 순서가 됩니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                            <button type="submit" class="btn btn-primary">이동</button>
                        </div>
                    </form>
                </div>
            </div>
        </c:if>
    </jsp:body>
</ui:layout>
