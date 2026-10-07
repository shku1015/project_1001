<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- SCR-COD-01 코드관리 (③ JSP SSR). 왼쪽 그룹코드, 오른쪽 상세코드. 등록·수정은 모달 폼 --%>
<c:set var="noPerm" value="권한이 없습니다"/>
<ui:layout title="코드관리" mode="ssr">
    <jsp:attribute name="scripts">
        <script src="<c:url value='/common/js/admin-code.js'/>"></script>
    </jsp:attribute>
    <jsp:body>
        <div class="row row-cards">
            <%-- ===== 그룹코드 ===== --%>
            <div class="col-lg-5">
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">그룹코드</h3>
                        <div class="card-actions">
                            <button type="button" class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#group-modal"
                                    data-mode="create" id="btn-group-create" ${canCreate ? '' : 'disabled'} title="${canCreate ? '' : noPerm}">그룹 등록</button>
                        </div>
                    </div>
                    <div class="card-body border-bottom">
                        <form method="get" action="<c:url value='/ssr/codes'/>" class="row g-2">
                            <div class="col">
                                <input type="text" class="form-control" name="keyword" placeholder="그룹코드 / 그룹코드명"
                                       value="<c:out value='${keyword}'/>">
                            </div>
                            <div class="col-auto">
                                <select class="form-select" name="useYn">
                                    <option value="">전체</option>
                                    <option value="Y" ${searchUseYn == 'Y' ? 'selected' : ''}>사용</option>
                                    <option value="N" ${searchUseYn == 'N' ? 'selected' : ''}>사용 안 함</option>
                                </select>
                            </div>
                            <div class="col-auto"><button type="submit" class="btn">검색</button></div>
                        </form>
                    </div>
                    <div class="table-responsive">
                        <table class="table table-vcenter table-selectable" id="group-table">
                            <thead><tr><th>그룹코드</th><th>그룹코드명</th><th class="text-center">상세</th><th class="text-center">사용</th></tr></thead>
                            <tbody>
                            <c:forEach var="g" items="${groups}">
                                <tr class="${selectedGroup != null and selectedGroup.groupCd() == g.groupCd() ? 'table-active' : ''}"
                                    data-group-cd="${g.groupCd()}">
                                    <td>
                                        <a href="<c:url value='/ssr/codes?group=${g.groupCd()}'/>">
                                            <c:if test="${g.systemYn() == 'Y'}">🔒 </c:if><c:out value="${g.groupCd()}"/>
                                        </a>
                                    </td>
                                    <td><c:out value="${g.groupNm()}"/></td>
                                    <td class="text-center">${g.codeCnt()}</td>
                                    <td class="text-center">${g.useYn() == 'Y' ? '사용' : '사용 안 함'}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty groups}"><tr><td colspan="4" class="text-secondary text-center">조회된 데이터가 없습니다</td></tr></c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <%-- ===== 상세코드 ===== --%>
            <div class="col-lg-7">
                <c:choose>
                    <c:when test="${selectedGroup == null}">
                        <div class="card"><div class="card-body text-secondary">왼쪽에서 그룹코드를 선택하세요.</div></div>
                    </c:when>
                    <c:otherwise>
                        <div class="card">
                            <div class="card-header">
                                <h3 class="card-title">
                                    <c:if test="${selectedGroup.systemYn() == 'Y'}">🔒 </c:if><c:out value="${selectedGroup.groupNm()}"/>
                                    <span class="text-secondary ms-1">(<c:out value="${selectedGroup.groupCd()}"/>)</span>
                                </h3>
                                <div class="card-actions btn-list">
                                    <button type="button" class="btn btn-sm" data-bs-toggle="modal" data-bs-target="#group-modal"
                                            data-mode="edit"
                                            data-group-cd="${selectedGroup.groupCd()}"
                                            data-group-nm="<c:out value='${selectedGroup.groupNm()}'/>"
                                            data-description="<c:out value='${selectedGroup.description()}'/>"
                                            data-use-yn="${selectedGroup.useYn()}"
                                            data-system-yn="${selectedGroup.systemYn()}"
                                            data-mod-dt="${selectedGroup.modDt()}" id="btn-group-edit" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">그룹 수정</button>
                                    <c:if test="${selectedGroup.systemYn() != 'Y' and empty details}">
                                        <form method="post" action="<c:url value='/ssr/codes/groups/${selectedGroup.groupCd()}/delete'/>"
                                              onsubmit="return confirm('그룹코드를 삭제하시겠습니까?')" class="d-inline">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                            <button type="submit" class="btn btn-sm btn-ghost-danger" id="btn-group-delete" ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">그룹 삭제</button>
                                        </form>
                                    </c:if>
                                    <c:if test="${selectedGroup.systemYn() != 'Y'}">
                                        <button type="button" class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#detail-modal"
                                                data-mode="create" data-next-sort="${nextSortOrd}" id="btn-detail-create" ${canCreate ? '' : 'disabled'} title="${canCreate ? '' : noPerm}">코드 등록</button>
                                    </c:if>
                                </div>
                            </div>
                            <div class="table-responsive">
                                <table class="table table-vcenter">
                                    <thead><tr><th>코드</th><th>코드명</th><th class="text-center">정렬</th><th>설명</th><th class="text-center">사용</th><th></th></tr></thead>
                                    <tbody>
                                    <c:forEach var="d" items="${details}">
                                        <tr>
                                            <td><c:out value="${d.code()}"/></td>
                                            <td><c:out value="${d.codeNm()}"/></td>
                                            <td class="text-center">${d.sortOrd()}</td>
                                            <td><c:out value="${d.description()}"/></td>
                                            <td class="text-center">${d.useYn() == 'Y' ? '사용' : '사용 안 함'}</td>
                                            <td class="text-end btn-list">
                                                <button type="button" class="btn btn-sm" data-bs-toggle="modal" data-bs-target="#detail-modal"
                                                        data-mode="edit"
                                                        data-code="${d.code()}"
                                                        data-code-nm="<c:out value='${d.codeNm()}'/>"
                                                        data-sort-ord="${d.sortOrd()}"
                                                        data-description="<c:out value='${d.description()}'/>"
                                                        data-use-yn="${d.useYn()}"
                                                        data-system-yn="${selectedGroup.systemYn()}"
                                                        data-mod-dt="${d.modDt()}" ${canUpdate ? '' : 'disabled'} title="${canUpdate ? '' : noPerm}">수정</button>
                                                <c:if test="${selectedGroup.systemYn() != 'Y'}">
                                                    <form method="post" action="<c:url value='/ssr/codes/groups/${selectedGroup.groupCd()}/codes/${d.code()}/delete'/>"
                                                          onsubmit="return confirm('삭제하면 복구할 수 없습니다. 사용 안 함으로 바꾸는 것을 권장합니다. 삭제하시겠습니까?')" class="d-inline">
                                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                        <button type="submit" class="btn btn-sm btn-ghost-danger" ${canDelete ? '' : 'disabled'} title="${canDelete ? '' : noPerm}">삭제</button>
                                                    </form>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty details}"><tr><td colspan="6" class="text-secondary text-center">상세코드가 없습니다</td></tr></c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- ===== 그룹코드 모달 ===== --%>
        <div class="modal modal-blur fade" id="group-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" method="post" id="group-form">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="modDt" id="group-modDt">
                    <div class="modal-header"><h5 class="modal-title" id="group-modal-title">그룹 등록</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label required" for="group-groupCd">그룹코드</label>
                            <input class="form-control" name="groupCd" id="group-groupCd" maxlength="50" required
                                   pattern="[A-Z0-9_]+" title="영문 대문자·숫자·_">
                            <small class="form-hint" id="group-groupCd-display" hidden></small>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="group-groupNm">그룹코드명</label>
                            <input class="form-control" name="groupNm" id="group-groupNm" maxlength="100" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="group-description">설명</label>
                            <textarea class="form-control" name="description" id="group-description" rows="2" maxlength="500"></textarea>
                        </div>
                        <div class="mb-3" id="group-useYn-wrap">
                            <label class="form-label">사용 여부</label>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="Y" checked> 사용</label>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="N"> 사용 안 함</label>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                        <button type="submit" class="btn btn-primary">저장</button>
                    </div>
                </form>
            </div>
        </div>

        <%-- ===== 상세코드 모달 ===== --%>
        <div class="modal modal-blur fade" id="detail-modal" tabindex="-1">
            <div class="modal-dialog modal-dialog-centered">
                <form class="modal-content" method="post" id="detail-form">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <input type="hidden" name="modDt" id="detail-modDt">
                    <div class="modal-header"><h5 class="modal-title" id="detail-modal-title">코드 등록</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label required" for="detail-code">코드</label>
                            <input class="form-control" name="code" id="detail-code" maxlength="50" required
                                   pattern="[A-Z0-9_]+" title="영문 대문자·숫자·_">
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="detail-codeNm">코드명</label>
                            <input class="form-control" name="codeNm" id="detail-codeNm" maxlength="100" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label required" for="detail-sortOrd">정렬 순서</label>
                            <input class="form-control" type="number" name="sortOrd" id="detail-sortOrd" min="0" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="detail-description">설명</label>
                            <textarea class="form-control" name="description" id="detail-description" rows="2" maxlength="500"></textarea>
                        </div>
                        <div class="mb-3" id="detail-useYn-wrap">
                            <label class="form-label">사용 여부</label>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="Y" checked> 사용</label>
                            <label class="form-check form-check-inline"><input class="form-check-input" type="radio" name="useYn" value="N"> 사용 안 함</label>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn" data-bs-dismiss="modal">취소</button>
                        <button type="submit" class="btn btn-primary">저장</button>
                    </div>
                </form>
            </div>
        </div>

        <c:if test="${selectedGroup != null}">
            <script>window.CODE_GROUP = '${selectedGroup.groupCd()}';</script>
        </c:if>
    </jsp:body>
</ui:layout>
