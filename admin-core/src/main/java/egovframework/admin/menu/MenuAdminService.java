package egovframework.admin.menu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.menu.MenuAdminMapper.GrantRow;
import egovframework.admin.menu.MenuAdminMapper.MenuRow;

/**
 * 메뉴관리 (docs/04-features/03-menu.md). 메뉴와 권한(메뉴 × 액션)을 함께 관리한다 (docs/02-access-model.md 3절).
 * 메뉴·권한이 바뀌면 관리자 권한 캐시를 모두 비운다 (다음 요청부터 반영).
 */
@Service
public class MenuAdminService {

    public static final int MAX_DEPTH = 3;
    private static final String MENU = "MENU";
    private static final String CODE_PATTERN = "^[A-Z0-9_]+$";

    /** 관리용 트리 노드 (api/openapi.yaml MenuAdminNode) */
    public record MenuAdminNode(long menuId, Long parentMenuId, String menuCd, String menuNm, String menuTypeCd,
                                String menuUrl, int depth, int sortOrd, String useYn, String systemYn,
                                String boardAutoYn, List<MenuAdminNode> children) {
    }

    /** 메뉴 상세 (api/openapi.yaml MenuDetail) */
    public record MenuDetail(long menuId, Long parentMenuId, String parentMenuNm, String menuCd, String menuNm,
                             String menuTypeCd, String menuUrl, String icon, int depth, int sortOrd, String useYn,
                             String systemYn, String boardAutoYn, List<Action> actions, LocalDateTime modDt) {

        @JsonIgnore
        public boolean isSystem() {
            return "Y".equals(systemYn);
        }

        @JsonIgnore
        public boolean isBoardAuto() {
            return "Y".equals(boardAutoYn);
        }

        @JsonIgnore
        public boolean isFolder() {
            return "FOLDER".equals(menuTypeCd);
        }
    }

    public record CreateCommand(Long parentMenuId, String menuCd, String menuNm, String menuTypeCd, String menuUrl,
                                List<Action> actions, String icon, String useYn) {
    }

    public record UpdateCommand(String menuNm, String menuUrl, List<Action> actions, String icon, String useYn,
                                LocalDateTime modDt) {
    }

    /** 액션을 빼서 회수되는(dryRun이면 회수될) 역할 */
    public record RevokedRole(long roleId, String roleNm, List<Action> actions) {
    }

    /** 순서 저장 한 묶음: 상위 메뉴(null이면 최상위)의 하위 메뉴 전부를 새 순서대로 */
    public record OrderEntry(Long parentMenuId, List<Long> menuIds) {
    }

    private final MenuAdminMapper mapper;
    private final AuditLogService auditLogService;
    private final AdminAuthInfoService authInfoService;

    public MenuAdminService(MenuAdminMapper mapper, AuditLogService auditLogService,
                            AdminAuthInfoService authInfoService) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.authInfoService = authInfoService;
    }

    // ================= 조회 =================

    /** MNU-01 전체 메뉴 트리 (사용 안 함 메뉴 포함) */
    @Transactional(readOnly = true)
    public List<MenuAdminNode> getTree() {
        Map<Long, List<MenuRow>> childrenByParent = new LinkedHashMap<>();
        List<MenuRow> roots = new ArrayList<>();
        for (MenuRow row : mapper.selectAllMenus()) {
            if (row.parentMenuId() == null) {
                roots.add(row);
            } else {
                childrenByParent.computeIfAbsent(row.parentMenuId(), k -> new ArrayList<>()).add(row);
            }
        }
        return toNodes(roots, childrenByParent);
    }

    private List<MenuAdminNode> toNodes(List<MenuRow> rows, Map<Long, List<MenuRow>> childrenByParent) {
        List<MenuAdminNode> nodes = new ArrayList<>();
        for (MenuRow r : rows) {
            nodes.add(new MenuAdminNode(r.menuId(), r.parentMenuId(), r.menuCd(), r.menuNm(), r.menuTypeCd(),
                    r.menuUrl(), r.depth(), r.sortOrd(), r.useYn(), r.systemYn(), r.boardAutoYn(),
                    toNodes(childrenByParent.getOrDefault(r.menuId(), List.of()), childrenByParent)));
        }
        return nodes;
    }

    /** MNU-02 메뉴 상세 */
    @Transactional(readOnly = true)
    public MenuDetail getMenu(long menuId) {
        MenuRow r = mapper.selectMenu(menuId);
        if (r == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "메뉴가 없습니다: " + menuId);
        }
        List<Action> actions = mapper.selectActions(menuId).stream().map(Action::valueOf).toList();
        return new MenuDetail(r.menuId(), r.parentMenuId(), r.parentMenuNm(), r.menuCd(), r.menuNm(), r.menuTypeCd(),
                r.menuUrl(), r.icon(), r.depth(), r.sortOrd(), r.useYn(), r.systemYn(), r.boardAutoYn(), actions,
                r.modDt());
    }

    @Transactional(readOnly = true)
    public boolean isMenuCdAvailable(String menuCd) {
        return !mapper.existsMenuCd(menuCd);
    }

    // ================= 등록 (MNU-03) =================

    @Transactional
    public long create(long adminId, String ipAddr, CreateCommand cmd) {
        validateName(cmd.menuNm());
        if (cmd.menuCd() == null || !cmd.menuCd().matches(CODE_PATTERN) || cmd.menuCd().length() > 50) {
            throw BusinessException.field("menuCd", "메뉴 코드는 영문 대문자·숫자·_ 50자 이내입니다.");
        }
        if (mapper.existsMenuCd(cmd.menuCd())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 메뉴 코드입니다.");    // BR-03
        }
        boolean folder = "FOLDER".equals(cmd.menuTypeCd());
        if (!folder && !"PAGE".equals(cmd.menuTypeCd())) {
            throw BusinessException.field("menuTypeCd", "메뉴 종류를 고르세요.");
        }

        int depth = 1;
        if (cmd.parentMenuId() != null) {
            MenuDetail parent = getMenu(cmd.parentMenuId());
            if (!parent.isFolder()) {
                throw new BusinessException(ErrorCode.MENU_PARENT_NOT_FOLDER);    // BR-02
            }
            depth = parent.depth() + 1;
        }
        // BR-01: 3단계까지. 3단계는 하위를 둘 수 없으므로 화면 메뉴만 된다
        if (depth > MAX_DEPTH || (folder && depth == MAX_DEPTH)) {
            throw new BusinessException(ErrorCode.MENU_DEPTH_EXCEEDED);
        }

        String url = folder ? null : requireUrl(cmd.menuUrl());
        List<String> actions = folder ? List.of() : normalizeActions(cmd.actions());    // BR-05
        String icon = depth == 1 ? blankToNull(cmd.icon()) : null;    // 아이콘은 1단계 메뉴만 쓴다
        Integer max = mapper.selectMaxSortOrd(cmd.parentMenuId());
        long menuId = mapper.insertMenu(cmd.parentMenuId(), cmd.menuCd(), cmd.menuNm().trim(), cmd.menuTypeCd(), url,
                icon, depth, max == null ? 1 : max + 1, yn(cmd.useYn()), adminId);
        if (!actions.isEmpty()) {
            mapper.insertPermissions(menuId, actions);    // BR-06: 권한만 만들고 역할에는 부여하지 않는다
        }
        audit(adminId, ipAddr, "CREATE", menuId, "메뉴 등록: " + cmd.menuCd(), null, getMenu(menuId));
        authInfoService.evictAll();
        return menuId;
    }

    // ================= 수정 (MNU-04) =================

    /**
     * @param dryRun true면 저장하지 않고 회수될 역할만 돌려준다 (BR-07 확인창용)
     * @return 액션을 빼서 회수된(될) 역할
     */
    @Transactional
    public List<RevokedRole> update(long adminId, String ipAddr, long menuId, UpdateCommand cmd, boolean dryRun) {
        MenuDetail before = getMenu(menuId);
        validateName(cmd.menuNm());

        String url = before.isFolder() ? null : requireUrl(cmd.menuUrl());
        List<String> actions = before.isFolder() ? List.of() : normalizeActions(cmd.actions());
        String useYn = yn(cmd.useYn());
        String icon = before.depth() == 1 ? blankToNull(cmd.icon()) : null;
        List<String> current = before.actions().stream().map(Enum::name).toList();

        // 시스템 메뉴·게시판 자동 메뉴는 메뉴명·아이콘만 바꿀 수 있다 (BR-10, BR-12)
        boolean protectedChange = !Objects.equals(url, before.menuUrl()) || !useYn.equals(before.useYn())
                || !new HashSet<>(actions).equals(new HashSet<>(current));
        if (protectedChange && before.isSystem()) {
            throw new BusinessException(ErrorCode.MENU_SYSTEM_PROTECTED);
        }
        if (protectedChange && before.isBoardAuto()) {
            throw new BusinessException(ErrorCode.MENU_BOARD_MANAGED);
        }

        List<String> removed = current.stream().filter(a -> !actions.contains(a)).toList();
        List<String> added = actions.stream().filter(a -> !current.contains(a)).toList();
        List<RevokedRole> revoked = removed.isEmpty() ? List.of() : revokedRoles(mapper.selectGrants(menuId, removed));
        if (dryRun) {
            return revoked;
        }

        if (mapper.updateMenu(menuId, cmd.menuNm().trim(), url, icon, useYn, adminId, cmd.modDt()) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        if (!removed.isEmpty()) {
            mapper.deleteRolePermissions(menuId, removed);    // BR-07: 역할 매핑까지 회수
            mapper.deletePermissions(menuId, removed);
        }
        if (!added.isEmpty()) {
            mapper.insertPermissions(menuId, added);    // BR-06
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("menu", getMenu(menuId));
        after.put("addedActions", added);
        after.put("removedActions", removed);
        after.put("revokedRoles", revoked);
        audit(adminId, ipAddr, "UPDATE", menuId, "메뉴 수정: " + before.menuCd(), before, after);
        authInfoService.evictAll();
        return revoked;
    }

    // ================= 상위 메뉴 변경 (MNU-05) =================

    @Transactional
    public void move(long adminId, String ipAddr, long menuId, Long newParentId, LocalDateTime modDt) {
        MenuDetail menu = getMenu(menuId);
        if (menu.isSystem()) {
            throw new BusinessException(ErrorCode.MENU_SYSTEM_PROTECTED);
        }
        if (menu.isBoardAuto()) {
            throw new BusinessException(ErrorCode.MENU_BOARD_MANAGED);
        }
        int newDepth = 1;
        if (newParentId != null) {
            if (mapper.selectSubtreeIds(menuId).contains(newParentId)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "자기 자신이나 하위 메뉴 아래로 옮길 수 없습니다.");
            }
            MenuDetail parent = getMenu(newParentId);
            if (!parent.isFolder()) {
                throw new BusinessException(ErrorCode.MENU_PARENT_NOT_FOLDER);    // BR-11 (BR-02)
            }
            newDepth = parent.depth() + 1;
        }
        int delta = newDepth - menu.depth();
        // BR-11 (BR-01): 옮긴 뒤 하위 트리의 가장 깊은 메뉴가 3단계를 넘으면 안 된다.
        // 3단계에는 폴더를 둘 수 없다 (하위를 둘 수 없으므로)
        if (mapper.selectSubtreeMaxDepth(menuId) + delta > MAX_DEPTH || (menu.isFolder() && newDepth == MAX_DEPTH)) {
            throw new BusinessException(ErrorCode.MENU_DEPTH_EXCEEDED);
        }
        Integer max = mapper.selectMaxSortOrd(newParentId);
        if (mapper.updateParent(menuId, newParentId, newDepth, max == null ? 1 : max + 1, adminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        if (delta != 0) {
            mapper.shiftDescendantDepth(menuId, delta);
        }
        audit(adminId, ipAddr, "UPDATE", menuId, "상위 메뉴 변경: " + menu.menuCd(),
                Map.of("parentMenuId", String.valueOf(menu.parentMenuId())),
                Map.of("parentMenuId", String.valueOf(newParentId)));
        authInfoService.evictAll();
    }

    // ================= 순서 저장 (MNU-06) =================

    /** 상위 메뉴별 하위 메뉴가 빠짐없이 있어야 한다 (그사이 바뀌었으면 MENU_ORDER_MISMATCH). 순서 변경은 감사로그를 남기지 않는다 */
    @Transactional
    public void saveOrder(List<OrderEntry> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        for (OrderEntry entry : orders) {
            List<Long> requested = entry.menuIds() == null ? List.of() : entry.menuIds();
            Set<Long> current = new HashSet<>(mapper.selectChildIds(entry.parentMenuId()));
            if (requested.size() != current.size() || !current.equals(new HashSet<>(requested))) {
                throw new BusinessException(ErrorCode.MENU_ORDER_MISMATCH);
            }
            for (int i = 0; i < requested.size(); i++) {
                mapper.updateSortOrd(requested.get(i), i + 1);
            }
        }
        authInfoService.evictAll();
    }

    // ================= 삭제 (MNU-07) =================

    @Transactional
    public void delete(long adminId, String ipAddr, long menuId) {
        MenuDetail menu = getMenu(menuId);
        if (menu.isSystem()) {
            throw new BusinessException(ErrorCode.MENU_SYSTEM_PROTECTED);    // BR-10
        }
        if (menu.isBoardAuto()) {
            throw new BusinessException(ErrorCode.MENU_BOARD_MANAGED);    // BR-12
        }
        if (mapper.countChildren(menuId) > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);    // BR-08
        }
        List<RevokedRole> revoked = revokedRoles(mapper.selectGrants(menuId, null));
        mapper.deleteRolePermissions(menuId, null);
        mapper.deletePermissions(menuId, null);
        mapper.deleteMenu(menuId);
        audit(adminId, ipAddr, "DELETE", menuId, "메뉴 삭제: " + menu.menuCd(), menu, Map.of("revokedRoles", revoked));
        authInfoService.evictAll();
    }

    // ================= 공통 =================

    private static List<RevokedRole> revokedRoles(List<GrantRow> grants) {
        Map<Long, RevokedRole> byRole = new LinkedHashMap<>();
        for (GrantRow g : grants) {
            RevokedRole role = byRole.computeIfAbsent(g.roleId(), id -> new RevokedRole(id, g.roleNm(), new ArrayList<>()));
            role.actions().add(Action.valueOf(g.actionCd()));
        }
        return byRole.values().stream().map(r -> new RevokedRole(r.roleId(), r.roleNm(), List.copyOf(r.actions())))
                .toList();
    }

    /** 화면 메뉴의 액션: READ를 항상 넣고 정해진 순서로 (BR-05) */
    private static List<String> normalizeActions(List<Action> actions) {
        EnumSet<Action> set = EnumSet.of(Action.READ);
        if (actions != null) {
            set.addAll(actions);
        }
        return set.stream().map(Enum::name).toList();
    }

    private static String requireUrl(String url) {
        if (url == null || url.isBlank() || !url.startsWith("/") || url.length() > 200) {
            throw BusinessException.field("menuUrl", "화면 메뉴는 /로 시작하는 URL이 필요합니다.");
        }
        return url.trim();
    }

    private static void validateName(String menuNm) {
        if (menuNm == null || menuNm.isBlank() || menuNm.length() > 100) {
            throw BusinessException.field("menuNm", "메뉴명은 1~100자입니다.");
        }
    }

    private static String yn(String value) {
        return "N".equals(value) ? "N" : "Y";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(long adminId, String ipAddr, String action, long menuId, String summary, Object before,
                       Object after) {
        auditLogService.record(new AuditEntry(adminId, MENU, action, "MENU", String.valueOf(menuId), summary, before,
                after, null, ipAddr));
    }
}
