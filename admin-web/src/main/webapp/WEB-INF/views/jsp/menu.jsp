<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-MNU-01 메뉴관리 (② JSP + API). 트리·상세는 admin-menu.js가 /api/v1/menus 로 그린다.
     마크업은 ③ ssr/menu.jsp, ① MenuPage.tsx와 같다 --%>
<ui:layout title="메뉴관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/webjars/sortablejs/${sortablejsVersion}/Sortable.min.js'/>"></script>
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-menu-tree.js'/>"></script>
        <script src="<c:url value='/common/js/admin-menu.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div id="page-message"></div>
        <div class="row row-cards">
            <div class="col-lg-5">
                <div class="card" id="menu-tree-card">
                    <div class="card-header">
                        <h3 class="card-title">메뉴</h3>
                        <div class="card-actions">
                            <button type="button" class="btn btn-primary btn-sm" id="btn-root-create" data-structure-action>최상위 메뉴 추가</button>
                        </div>
                    </div>
                    <div class="card-body">
                        <div id="order-bar" hidden>
                            <div class="alert alert-warning d-flex align-items-center gap-2 py-2">
                                <span class="me-auto">저장하지 않은 순서 변경이 있습니다</span>
                                <button type="button" class="btn btn-primary btn-sm" id="btn-order-save">순서 저장</button>
                                <button type="button" class="btn btn-sm" id="btn-order-reset">되돌리기</button>
                            </div>
                        </div>
                        <div id="menu-tree-root"></div>
                    </div>
                    <div class="card-footer d-flex gap-2">
                        <button type="button" class="btn btn-sm" id="btn-up" disabled>▲ 위로</button>
                        <button type="button" class="btn btn-sm" id="btn-down" disabled>▼ 아래로</button>
                    </div>
                </div>
            </div>
            <div class="col-lg-7" id="menu-detail-col">
                <div class="card"><div class="card-body text-secondary">왼쪽에서 메뉴를 선택하세요.</div></div>
            </div>
        </div>

        <div class="modal modal-blur fade" id="move-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" id="move-form">
                    <div class="modal-header">
                        <h5 class="modal-title">상위 메뉴 변경</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button>
                    </div>
                    <div class="modal-body">
                        <label class="form-label" for="move-parent">옮길 위치</label>
                        <select class="form-select" id="move-parent" name="parentMenuId"></select>
                        <p class="text-secondary mt-2 mb-0">옮긴 메뉴는 새 상위 메뉴의 맨 뒤 순서가 됩니다.</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                        <button type="submit" class="btn btn-primary">이동</button>
                    </div>
                </form>
            </div>
        </div>
    </jsp:body>
</ui:layout>
