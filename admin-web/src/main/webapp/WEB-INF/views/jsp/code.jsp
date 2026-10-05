<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COD-01 코드관리 (② JSP + API). 데이터·저장은 모두 /api/v1/code-groups 로 처리한다 --%>
<ui:layout title="코드관리" mode="jsp">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-api.js'/>"></script>
        <script src="<c:url value='/common/js/admin-jsp.js'/>"></script>
        <script src="<c:url value='/common/js/admin-code-api.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="row row-cards">
            <div class="col-lg-5">
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">그룹코드</h3>
                        <div class="card-actions"><button type="button" class="btn btn-primary btn-sm d-none" id="btn-group-create" data-bs-toggle="modal" data-bs-target="#group-modal" data-mode="create">그룹 등록</button></div>
                    </div>
                    <div class="card-body border-bottom">
                        <form class="row g-2" id="group-search">
                            <div class="col"><input type="text" class="form-control" id="search-keyword" placeholder="그룹코드 / 그룹코드명"></div>
                            <div class="col-auto">
                                <select class="form-select" id="search-useYn">
                                    <option value="">전체</option><option value="Y">사용</option><option value="N">사용 안 함</option>
                                </select>
                            </div>
                            <div class="col-auto"><button type="submit" class="btn">검색</button></div>
                        </form>
                    </div>
                    <div class="table-responsive">
                        <table class="table table-vcenter table-selectable">
                            <thead><tr><th>그룹코드</th><th>그룹코드명</th><th class="text-center">상세</th><th class="text-center">사용</th></tr></thead>
                            <tbody id="group-tbody"></tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div class="col-lg-7" id="detail-panel">
                <div class="card"><div class="card-body text-secondary">왼쪽에서 그룹코드를 선택하세요.</div></div>
            </div>
        </div>

        <%-- 모달은 SSR과 같은 구조(id·입력 칸)를 쓴다 --%>
<%-- 코드관리 모달 (② JSP + API). SSR과 같은 입력 칸 id. 저장은 JS가 API로 한다 --%>
<div class="modal modal-blur fade" id="group-modal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
        <form class="modal-content" id="group-form" novalidate>
            <div class="modal-header"><h5 class="modal-title" id="group-modal-title">그룹 등록</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body">
                <div class="mb-3" id="group-groupCd-field">
                    <label class="form-label required" for="group-groupCd">그룹코드</label>
                    <input class="form-control" id="group-groupCd" maxlength="50" pattern="[A-Z0-9_]+" title="영문 대문자·숫자·_">
                    <small class="form-hint" id="group-groupCd-display" hidden></small>
                </div>
                <div class="mb-3"><label class="form-label required" for="group-groupNm">그룹코드명</label>
                    <input class="form-control" id="group-groupNm" maxlength="100"></div>
                <div class="mb-3"><label class="form-label" for="group-description">설명</label>
                    <textarea class="form-control" id="group-description" rows="2" maxlength="500"></textarea></div>
                <div class="mb-3" id="group-useYn-wrap">
                    <label class="form-label">사용 여부</label>
                    <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="group-useYn" value="Y" checked> 사용</label>
                    <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="group-useYn" value="N"> 사용 안 함</label>
                </div>
                <div id="group-message"></div>
            </div>
            <div class="modal-footer"><button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                <button type="submit" class="btn btn-primary">저장</button></div>
        </form>
    </div>
</div>

<div class="modal modal-blur fade" id="detail-modal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
        <form class="modal-content" id="detail-form" novalidate>
            <div class="modal-header"><h5 class="modal-title" id="detail-modal-title">코드 등록</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body">
                <div class="mb-3"><label class="form-label required" for="detail-code">코드</label>
                    <input class="form-control" id="detail-code" maxlength="50" pattern="[A-Z0-9_]+" title="영문 대문자·숫자·_"></div>
                <div class="mb-3"><label class="form-label required" for="detail-codeNm">코드명</label>
                    <input class="form-control" id="detail-codeNm" maxlength="100"></div>
                <div class="mb-3"><label class="form-label required" for="detail-sortOrd">정렬 순서</label>
                    <input class="form-control" type="number" id="detail-sortOrd" min="0"></div>
                <div class="mb-3"><label class="form-label" for="detail-description">설명</label>
                    <textarea class="form-control" id="detail-description" rows="2" maxlength="500"></textarea></div>
                <div class="mb-3" id="detail-useYn-wrap">
                    <label class="form-label">사용 여부</label>
                    <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="detail-useYn" value="Y" checked> 사용</label>
                    <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="detail-useYn" value="N"> 사용 안 함</label>
                </div>
                <div id="detail-message"></div>
            </div>
            <div class="modal-footer"><button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                <button type="submit" class="btn btn-primary">저장</button></div>
        </form>
    </div>
</div>
    </jsp:body>
</ui:layout>
