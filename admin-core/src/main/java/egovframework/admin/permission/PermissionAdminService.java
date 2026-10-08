package egovframework.admin.permission;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.permission.PermissionMapper.AdminRow;
import egovframework.admin.permission.PermissionMapper.GrantRow;
import egovframework.admin.role.RoleMapper;
import egovframework.admin.role.RoleMapper.MenuRow;
import egovframework.admin.role.RoleMapper.PermRow;
import egovframework.admin.role.RoleMapper.RoleRow;

/**
 * 권한관리 (docs/04-features/07-permission.md). 역할관리와 같은 데이터(tb_role_permission)를 메뉴 기준으로 보고 고친다.
 * 권한 자체는 메뉴관리에서 생기므로 여기서 만들거나 지우지 않는다 (BR-01).
 * 보호 규칙: 시스템 역할(BR-03)·내 역할(BR-04) 변경 불가, 내가 갖지 않은 권한 부여 불가(BR-05). 슈퍼관리자는 예외.
 */
@Service
public class PermissionAdminService {

    private static final String PERMISSION = "PERMISSION";

    /** 권한 목록의 메뉴 노드 (api/openapi.yaml PermissionMenuNode) */
    public record MenuNode(long menuId, String menuCd, String menuNm, String menuTypeCd, int depth, String useYn,
                           List<Action> actions, List<MenuNode> children) {
    }

    public record MenuSummary(long menuId, String menuCd, String menuNm, List<Action> actions) {
    }

    /** 메뉴의 역할 한 행 (api/openapi.yaml MenuRoleGrant) */
    public record RoleGrant(long roleId, String roleCd, String roleNm, String systemYn, String useYn, boolean mine,
                            boolean editable, List<Action> granted) {
    }

    public record MenuGrants(MenuSummary menu, List<RoleGrant> roles, List<Action> grantableActions) {
    }

    /** 저장 요청 한 역할: 이 메뉴에서의 최종 액션 목록 */
    public record RoleActions(long roleId, List<Action> actions) {
    }

    public record RoleGrantChange(long roleId, String roleNm, List<Action> added, List<Action> removed) {
    }

    public record EffectiveMenu(long menuId, String menuNm, String menuTypeCd, int depth, List<Action> actions,
                                Map<Action, List<String>> granted) {
    }

    public record EffectivePermissions(AdminRow admin, boolean superAdmin, List<EffectiveMenu> menus) {
    }

    private final PermissionMapper mapper;
    private final RoleMapper roleMapper;
    private final AuditLogService auditLogService;
    private final AdminAuthInfoService authInfoService;

    public PermissionAdminService(PermissionMapper mapper, RoleMapper roleMapper, AuditLogService auditLogService,
                                  AdminAuthInfoService authInfoService) {
        this.mapper = mapper;
        this.roleMapper = roleMapper;
        this.auditLogService = auditLogService;
        this.authInfoService = authInfoService;
    }

    // ================= PRM-01 권한 목록 =================

    @Transactional(readOnly = true)
    public List<MenuNode> getTree() {
        Map<Long, List<Action>> actions = actionsByMenu();
        Map<Long, List<MenuRow>> childrenByParent = new LinkedHashMap<>();
        List<MenuRow> roots = new ArrayList<>();
        splitTree(roleMapper.selectMenus(), roots, childrenByParent);
        return toNodes(roots, childrenByParent, actions);
    }

    private List<MenuNode> toNodes(List<MenuRow> rows, Map<Long, List<MenuRow>> childrenByParent,
                                   Map<Long, List<Action>> actions) {
        List<MenuNode> nodes = new ArrayList<>();
        for (MenuRow m : rows) {
            nodes.add(new MenuNode(m.menuId(), m.menuCd(), m.menuNm(), m.menuTypeCd(), m.depth(), m.useYn(),
                    actions.getOrDefault(m.menuId(), List.of()),
                    toNodes(childrenByParent.getOrDefault(m.menuId(), List.of()), childrenByParent, actions)));
        }
        return nodes;
    }

    // ================= PRM-02 메뉴의 역할 × 액션 =================

    @Transactional(readOnly = true)
    public MenuGrants getMenuGrants(long adminId, long menuId) {
        MenuRow menu = findPage(menuId);
        List<Action> actions = actionsByMenu().getOrDefault(menuId, List.of());
        AdminAuthInfo me = authInfoService.load(adminId);
        Set<Long> myRoles = new HashSet<>(mapper.selectAdminRoleIds(adminId));

        Map<Long, List<Action>> grantedByRole = new HashMap<>();
        mapper.selectMenuGrants(menuId).forEach(g ->
                grantedByRole.computeIfAbsent(g.roleId(), k -> new ArrayList<>()).add(Action.valueOf(g.actionCd())));

        List<RoleGrant> roles = roleMapper.selectRoles(null, null).stream().map(r -> {
            boolean system = isSystem(r);
            boolean mine = myRoles.contains(r.roleId());
            // 슈퍼관리자(시스템 역할)는 권한 체크 예외라 사용 액션 전체를 가진 것으로 보여 준다
            List<Action> granted = system ? actions
                    : actions.stream().filter(grantedByRole.getOrDefault(r.roleId(), List.of())::contains).toList();
            return new RoleGrant(r.roleId(), r.roleCd(), r.roleNm(), r.systemYn(), r.useYn(), mine,
                    !system && (me.superAdmin() || !mine), granted);
        }).toList();
        List<Action> grantable = actions.stream().filter(a -> me.has(menu.menuCd(), a)).toList();
        return new MenuGrants(new MenuSummary(menu.menuId(), menu.menuCd(), menu.menuNm(), actions), roles, grantable);
    }

    // ================= PRM-03 메뉴 기준 부여·회수 =================

    /**
     * 바뀐 역할만, 이 메뉴에서의 최종 액션 목록으로 받는다.
     * READ가 아닌 액션이 있으면 READ를 함께 주고, 액션이 없으면 모두 회수한다 (BR-02).
     */
    @Transactional
    public List<RoleGrantChange> saveMenuGrants(long adminId, String ipAddr, long menuId, List<RoleActions> roles) {
        MenuRow menu = findPage(menuId);
        Map<Action, Long> permIdByAction = new EnumMap<>(Action.class);
        roleMapper.selectAllPermissions().stream().filter(p -> p.menuId() == menuId)
                .forEach(p -> permIdByAction.put(Action.valueOf(p.actionCd()), p.permId()));
        AdminAuthInfo me = authInfoService.load(adminId);
        Set<Long> myRoles = new HashSet<>(mapper.selectAdminRoleIds(adminId));
        Map<Long, RoleRow> roleById = new HashMap<>();
        roleMapper.selectRoles(null, null).forEach(r -> roleById.put(r.roleId(), r));

        Map<Long, Set<Action>> current = new HashMap<>();
        for (GrantRow g : mapper.selectMenuGrants(menuId)) {
            current.computeIfAbsent(g.roleId(), k -> EnumSet.noneOf(Action.class)).add(Action.valueOf(g.actionCd()));
        }

        // 1) 모두 확인한 뒤 2) 반영한다 (하나라도 어기면 아무것도 바꾸지 않는다)
        List<RoleGrantChange> changes = new ArrayList<>();
        for (RoleActions entry : roles) {
            RoleRow role = roleById.get(entry.roleId());
            if (role == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "역할이 없습니다: " + entry.roleId());
            }
            if (isSystem(role) || (!me.superAdmin() && myRoles.contains(role.roleId()))) {
                throw new BusinessException(ErrorCode.ROLE_NOT_EDITABLE);
            }
            Set<Action> desired = EnumSet.noneOf(Action.class);
            desired.addAll(entry.actions());
            if (!desired.isEmpty()) {
                desired.add(Action.READ);
            }
            if (!permIdByAction.keySet().containsAll(desired)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "메뉴에서 쓰지 않는 액션입니다: " + desired);
            }
            Set<Action> before = current.getOrDefault(role.roleId(), EnumSet.noneOf(Action.class));
            List<Action> added = desired.stream().filter(a -> !before.contains(a)).toList();
            List<Action> removed = before.stream().filter(a -> !desired.contains(a)).toList();
            if (added.stream().anyMatch(a -> !me.has(menu.menuCd(), a))) {
                throw new BusinessException(ErrorCode.PRIVILEGE_ESCALATION);
            }
            if (!added.isEmpty() || !removed.isEmpty()) {
                changes.add(new RoleGrantChange(role.roleId(), role.roleNm(), added, removed));
            }
        }
        for (RoleGrantChange c : changes) {
            if (!c.removed().isEmpty()) {
                roleMapper.deleteRolePermissions(c.roleId(), c.removed().stream().map(permIdByAction::get).toList());
            }
            if (!c.added().isEmpty()) {
                roleMapper.insertRolePermissions(c.roleId(), c.added().stream().map(permIdByAction::get).toList(),
                        adminId);
            }
        }
        if (!changes.isEmpty()) {
            auditLogService.record(new AuditEntry(adminId, PERMISSION, "UPDATE", "MENU", String.valueOf(menuId),
                    "메뉴 기준 권한 변경: " + menu.menuCd() + " (역할 " + changes.size() + "개)", null,
                    Map.of("menuCd", menu.menuCd(), "changes", changes), null, ipAddr));
            authInfoService.evictAll();
        }
        return changes;
    }

    // ================= PRM-04 관리자별 최종 권한 =================

    @Transactional(readOnly = true)
    public EffectivePermissions getEffectivePermissions(long targetAdminId) {
        AdminRow admin = mapper.selectAdmin(targetAdminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "관리자가 없습니다: " + targetAdminId);
        }
        if (authInfoService.load(targetAdminId).superAdmin()) {
            return new EffectivePermissions(admin, true, List.of());
        }
        Map<Long, Map<Action, List<String>>> grants = new HashMap<>();
        mapper.selectAdminGrants(targetAdminId).forEach(g -> {
            List<String> roleNms = grants.computeIfAbsent(g.menuId(), k -> new EnumMap<>(Action.class))
                    .computeIfAbsent(Action.valueOf(g.actionCd()), k -> new ArrayList<>());
            if (!roleNms.contains(g.roleNm())) {
                roleNms.add(g.roleNm());
            }
        });
        Map<Long, List<Action>> actions = actionsByMenu();
        Map<Long, List<MenuRow>> childrenByParent = new LinkedHashMap<>();
        List<MenuRow> roots = new ArrayList<>();
        splitTree(roleMapper.selectMenus(), roots, childrenByParent);
        List<EffectiveMenu> menus = new ArrayList<>();
        flatten(roots, childrenByParent, actions, grants, menus);
        return new EffectivePermissions(admin, false, menus);
    }

    private void flatten(List<MenuRow> rows, Map<Long, List<MenuRow>> childrenByParent,
                         Map<Long, List<Action>> actions, Map<Long, Map<Action, List<String>>> grants,
                         List<EffectiveMenu> out) {
        for (MenuRow m : rows) {
            out.add(new EffectiveMenu(m.menuId(), m.menuNm(), m.menuTypeCd(), m.depth(),
                    actions.getOrDefault(m.menuId(), List.of()), grants.getOrDefault(m.menuId(), Map.of())));
            flatten(childrenByParent.getOrDefault(m.menuId(), List.of()), childrenByParent, actions, grants, out);
        }
    }

    // ================= 공통 =================

    private MenuRow findPage(long menuId) {
        MenuRow menu = roleMapper.selectMenus().stream().filter(m -> m.menuId() == menuId).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메뉴가 없습니다: " + menuId));
        if (!"PAGE".equals(menu.menuTypeCd())) {
            throw new BusinessException(ErrorCode.MENU_NOT_PAGE);
        }
        return menu;
    }

    private Map<Long, List<Action>> actionsByMenu() {
        Map<Long, List<Action>> result = new HashMap<>();
        for (PermRow p : roleMapper.selectAllPermissions()) {
            result.computeIfAbsent(p.menuId(), k -> new ArrayList<>()).add(Action.valueOf(p.actionCd()));
        }
        return result;
    }

    private static void splitTree(List<MenuRow> rows, List<MenuRow> roots, Map<Long, List<MenuRow>> childrenByParent) {
        for (MenuRow m : rows) {
            if (m.parentMenuId() == null) {
                roots.add(m);
            } else {
                childrenByParent.computeIfAbsent(m.parentMenuId(), k -> new ArrayList<>()).add(m);
            }
        }
    }

    private static boolean isSystem(RoleRow r) {
        return "Y".equals(r.systemYn());
    }
}
