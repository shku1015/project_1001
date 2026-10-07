package egovframework.admin.role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
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
import egovframework.admin.role.RoleMapper.MenuRow;
import egovframework.admin.role.RoleMapper.PermRow;
import egovframework.admin.role.RoleMapper.RoleRow;

/**
 * 역할관리 (docs/04-features/08-role.md). 보호 규칙은 docs/02-access-model.md 5절 (R2 시스템 역할, R4 내 역할, R5 권한 상향 금지, R6 사용 중 역할).
 * 역할의 권한·사용 여부가 바뀌면 관리자 권한 캐시를 모두 비운다 (BR-09 다음 요청부터 반영).
 */
@Service
public class RoleAdminService {

    private static final String ROLE = "ROLE";
    private static final String CODE_PATTERN = "^[A-Z0-9_]+$";

    /** 목록 한 행 (api/openapi.yaml RoleListItem) */
    public record RoleListItem(long roleId, String roleCd, String roleNm, String description, int adminCnt,
                               String systemYn, String useYn) {
    }

    /** 상세 (api/openapi.yaml RoleDetail). editable: 수정·권한 설정 가능 여부 */
    public record RoleDetail(long roleId, String roleCd, String roleNm, String description, int adminCnt,
                             String systemYn, String useYn, boolean mine, boolean editable, String regNm,
                             LocalDateTime regDt, String modNm, LocalDateTime modDt) {
    }

    /** 권한 표의 메뉴 노드 (api/openapi.yaml RolePermissionNode) */
    public record PermissionNode(long menuId, String menuCd, String menuNm, String menuTypeCd, int depth,
                                 String useYn, List<Action> actions, List<Action> granted,
                                 List<Action> grantableActions, List<PermissionNode> children) {
    }

    public record PermissionChange(long menuId, String menuNm, Action action) {
    }

    public record SaveResult(List<PermissionChange> added, List<PermissionChange> removed) {
    }

    /** 권한 설정 요청 한 메뉴 */
    public record MenuActions(long menuId, List<Action> actions) {
    }

    public record CreateCommand(String roleCd, String roleNm, String description, String useYn) {
    }

    public record UpdateCommand(String roleNm, String description, String useYn, LocalDateTime modDt) {
    }

    private final RoleMapper mapper;
    private final AuditLogService auditLogService;
    private final AdminAuthInfoService authInfoService;

    public RoleAdminService(RoleMapper mapper, AuditLogService auditLogService,
                            AdminAuthInfoService authInfoService) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.authInfoService = authInfoService;
    }

    // ================= 조회 =================

    /** ROL-01 역할 목록 (등록순) */
    @Transactional(readOnly = true)
    public List<RoleListItem> getRoles(String keyword, String useYn) {
        return mapper.selectRoles(keyword == null ? null : keyword.trim(), useYn).stream()
                .map(r -> new RoleListItem(r.roleId(), r.roleCd(), r.roleNm(), r.description(), r.adminCnt(),
                        r.systemYn(), r.useYn()))
                .toList();
    }

    /** ROL-02 역할 상세 */
    @Transactional(readOnly = true)
    public RoleDetail getRole(long adminId, long roleId) {
        RoleRow r = findRole(roleId);
        boolean mine = mapper.hasRole(adminId, roleId);
        return new RoleDetail(r.roleId(), r.roleCd(), r.roleNm(), r.description(), r.adminCnt(), r.systemYn(),
                r.useYn(), mine, editable(authInfoService.load(adminId), r, mine), r.regNm(), r.regDt(), r.modNm(),
                r.modDt());
    }

    @Transactional(readOnly = true)
    public boolean isRoleCdAvailable(String roleCd) {
        return !mapper.existsRoleCd(roleCd);
    }

    /** 권한 탭: 메뉴 트리 × 액션. grantableActions는 내가 가진 액션 (BR-05) */
    @Transactional(readOnly = true)
    public List<PermissionNode> getPermissionTree(long adminId, long roleId) {
        findRole(roleId);
        AdminAuthInfo me = authInfoService.load(adminId);
        Map<Long, List<Action>> actionsByMenu = groupActions(mapper.selectAllPermissions());
        Map<Long, List<Action>> grantedByMenu = groupActions(mapper.selectRolePermissions(roleId));

        Map<Long, List<MenuRow>> childrenByParent = new LinkedHashMap<>();
        List<MenuRow> roots = new ArrayList<>();
        for (MenuRow m : mapper.selectMenus()) {
            if (m.parentMenuId() == null) {
                roots.add(m);
            } else {
                childrenByParent.computeIfAbsent(m.parentMenuId(), k -> new ArrayList<>()).add(m);
            }
        }
        return toNodes(roots, childrenByParent, actionsByMenu, grantedByMenu, me);
    }

    private List<PermissionNode> toNodes(List<MenuRow> rows, Map<Long, List<MenuRow>> childrenByParent,
                                         Map<Long, List<Action>> actionsByMenu, Map<Long, List<Action>> grantedByMenu,
                                         AdminAuthInfo me) {
        List<PermissionNode> nodes = new ArrayList<>();
        for (MenuRow m : rows) {
            List<Action> actions = actionsByMenu.getOrDefault(m.menuId(), List.of());
            List<Action> grantable = actions.stream().filter(a -> me.has(m.menuCd(), a)).toList();
            nodes.add(new PermissionNode(m.menuId(), m.menuCd(), m.menuNm(), m.menuTypeCd(), m.depth(), m.useYn(),
                    actions, grantedByMenu.getOrDefault(m.menuId(), List.of()), grantable,
                    toNodes(childrenByParent.getOrDefault(m.menuId(), List.of()), childrenByParent, actionsByMenu,
                            grantedByMenu, me)));
        }
        return nodes;
    }

    private static Map<Long, List<Action>> groupActions(List<PermRow> rows) {
        Map<Long, List<Action>> result = new HashMap<>();
        rows.forEach(p -> result.computeIfAbsent(p.menuId(), k -> new ArrayList<>()).add(Action.valueOf(p.actionCd())));
        return result;
    }

    /** 관리자 탭 */
    @Transactional(readOnly = true)
    public List<RoleMapper.AdminRow> getAdmins(long roleId) {
        findRole(roleId);
        return mapper.selectRoleAdmins(roleId);
    }

    // ================= 등록·복사 =================

    /** ROL-03 역할 등록. 권한은 상세 화면에서 설정한다 */
    @Transactional
    public long create(long adminId, String ipAddr, CreateCommand cmd) {
        validateRoleCd(cmd.roleCd());
        validateInfo(cmd.roleNm(), cmd.description(), cmd.useYn());
        long roleId = mapper.insertRole(cmd.roleCd(), cmd.roleNm().trim(), blankToNull(cmd.description()),
                cmd.useYn(), adminId);
        audit(adminId, ipAddr, "CREATE", roleId, "역할 등록: " + cmd.roleCd(), null, cmd);
        return roleId;
    }

    /** ROL-04 역할 복사. 원본의 설명·권한을 그대로 가지고 사용 여부는 Y. 원본 권한이 내 권한을 넘으면 거부 (R5) */
    @Transactional
    public long copy(long adminId, String ipAddr, long sourceRoleId, String roleCd, String roleNm) {
        RoleRow source = findRole(sourceRoleId);
        validateRoleCd(roleCd);
        validateInfo(roleNm, null, "Y");
        List<PermRow> perms = mapper.selectRolePermissions(sourceRoleId);
        AdminAuthInfo me = authInfoService.load(adminId);
        if (perms.stream().anyMatch(p -> !me.has(p.menuCd(), Action.valueOf(p.actionCd())))) {
            throw new BusinessException(ErrorCode.PRIVILEGE_ESCALATION);
        }
        long roleId = mapper.insertRole(roleCd, roleNm.trim(), source.description(), "Y", adminId);
        if (!perms.isEmpty()) {
            mapper.insertRolePermissions(roleId, perms.stream().map(PermRow::permId).toList(), adminId);
        }
        audit(adminId, ipAddr, "CREATE", roleId, "역할 복사: " + source.roleCd() + " → " + roleCd, null,
                Map.of("sourceRoleCd", source.roleCd(), "roleCd", roleCd, "roleNm", roleNm,
                        "permissions", changes(perms)));
        return roleId;
    }

    // ================= 수정 =================

    /** ROL-05 역할 정보 수정. 사용 안 함으로 바꾸면 그 역할의 권한이 최종 권한에서 빠진다 (BR-07) */
    @Transactional
    public void update(long adminId, String ipAddr, long roleId, UpdateCommand cmd) {
        RoleRow before = findEditable(adminId, roleId);
        validateInfo(cmd.roleNm(), cmd.description(), cmd.useYn());
        if (mapper.updateRole(roleId, cmd.roleNm().trim(), blankToNull(cmd.description()), cmd.useYn(), adminId,
                cmd.modDt()) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(adminId, ipAddr, "UPDATE", roleId, "역할 수정: " + before.roleCd(),
                Map.of("roleNm", before.roleNm(), "description", String.valueOf(before.description()),
                        "useYn", before.useYn()),
                Map.of("roleNm", cmd.roleNm().trim(), "description", String.valueOf(blankToNull(cmd.description())),
                        "useYn", cmd.useYn()));
        if (!before.useYn().equals(cmd.useYn())) {
            authInfoService.evictAll();
        }
    }

    /**
     * ROL-06 권한 설정. 권한을 줄 메뉴만 메뉴별 최종 액션 목록으로 받는다. 목록에 없는 메뉴는 모두 회수한다.
     * READ가 아닌 액션이 있으면 READ를 함께 준다 (BR-03). 내가 갖지 않은 권한은 새로 줄 수 없다 (BR-05).
     */
    @Transactional
    public SaveResult savePermissions(long adminId, String ipAddr, long roleId, List<MenuActions> permissions,
                                      LocalDateTime modDt) {
        RoleRow role = findEditable(adminId, roleId);
        Map<String, PermRow> all = new HashMap<>();
        mapper.selectAllPermissions().forEach(p -> all.put(key(p.menuId(), p.actionCd()), p));

        Map<String, PermRow> desired = new LinkedHashMap<>();
        for (MenuActions entry : permissions) {
            Set<Action> actions = EnumSet.noneOf(Action.class);
            actions.addAll(entry.actions());
            if (!actions.isEmpty()) {
                actions.add(Action.READ);
            }
            for (Action a : actions) {
                PermRow p = all.get(key(entry.menuId(), a.name()));
                if (p == null) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST,
                            "메뉴에서 쓰지 않는 액션입니다: " + entry.menuId() + " " + a);
                }
                desired.put(key(p.menuId(), p.actionCd()), p);
            }
        }
        Map<String, PermRow> current = new LinkedHashMap<>();
        mapper.selectRolePermissions(roleId).forEach(p -> current.put(key(p.menuId(), p.actionCd()), p));

        List<PermRow> added = desired.values().stream().filter(p -> !current.containsKey(key(p.menuId(), p.actionCd())))
                .sorted(PERM_ORDER).toList();
        List<PermRow> removed = current.values().stream().filter(p -> !desired.containsKey(key(p.menuId(), p.actionCd())))
                .sorted(PERM_ORDER).toList();

        AdminAuthInfo me = authInfoService.load(adminId);
        if (added.stream().anyMatch(p -> !me.has(p.menuCd(), Action.valueOf(p.actionCd())))) {
            throw new BusinessException(ErrorCode.PRIVILEGE_ESCALATION);
        }
        if (mapper.touchRole(roleId, adminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        if (!removed.isEmpty()) {
            mapper.deleteRolePermissions(roleId, removed.stream().map(PermRow::permId).toList());
        }
        if (!added.isEmpty()) {
            mapper.insertRolePermissions(roleId, added.stream().map(PermRow::permId).toList(), adminId);
        }
        SaveResult result = new SaveResult(changes(added), changes(removed));
        audit(adminId, ipAddr, "UPDATE", roleId,
                "역할 권한 설정: " + role.roleCd() + " (추가 " + added.size() + ", 제거 " + removed.size() + ")",
                Map.of("removed", result.removed()), Map.of("added", result.added()));
        authInfoService.evictAll();
        return result;
    }

    private static final Comparator<PermRow> PERM_ORDER = Comparator
            .comparingLong(PermRow::menuId).thenComparing(p -> Action.valueOf(p.actionCd()));

    // ================= 삭제 (ROL-07) =================

    /** 시스템 역할(R2)·관리자에게 부여된 역할(R6)은 삭제할 수 없다. 권한 매핑도 함께 지운다 (BR-06) */
    @Transactional
    public void delete(long adminId, String ipAddr, long roleId) {
        RoleRow role = findRole(roleId);
        if (isSystem(role)) {
            throw new BusinessException(ErrorCode.ROLE_SYSTEM_PROTECTED);
        }
        if (role.adminCnt() > 0) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE);
        }
        List<PermRow> perms = mapper.selectRolePermissions(roleId);
        mapper.deleteAllRolePermissions(roleId);
        mapper.deleteRole(roleId);
        audit(adminId, ipAddr, "DELETE", roleId, "역할 삭제: " + role.roleCd(),
                Map.of("roleCd", role.roleCd(), "roleNm", role.roleNm(), "permissions", changes(perms)), null);
        authInfoService.evictAll();
    }

    // ================= 공통 =================

    private RoleRow findRole(long roleId) {
        RoleRow r = mapper.selectRole(roleId);
        if (r == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "역할이 없습니다: " + roleId);
        }
        return r;
    }

    /** 시스템 역할(R2)이나 내 역할(BR-04, 슈퍼관리자 예외)은 수정·권한 설정을 할 수 없다 */
    private RoleRow findEditable(long adminId, long roleId) {
        RoleRow r = findRole(roleId);
        if (!editable(authInfoService.load(adminId), r, mapper.hasRole(adminId, roleId))) {
            throw new BusinessException(ErrorCode.ROLE_NOT_EDITABLE);
        }
        return r;
    }

    private static boolean editable(AdminAuthInfo me, RoleRow r, boolean mine) {
        return !isSystem(r) && (me.superAdmin() || !mine);
    }

    private static boolean isSystem(RoleRow r) {
        return "Y".equals(r.systemYn());
    }

    private void validateRoleCd(String roleCd) {
        if (roleCd == null || roleCd.length() > 50 || !roleCd.matches(CODE_PATTERN)) {
            throw BusinessException.field("roleCd", "역할 코드는 영문 대문자·숫자·_ 50자 이내입니다.");
        }
        if (mapper.existsRoleCd(roleCd)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 역할 코드입니다.");
        }
    }

    private static void validateInfo(String roleNm, String description, String useYn) {
        if (roleNm == null || roleNm.isBlank() || roleNm.trim().length() > 100) {
            throw BusinessException.field("roleNm", "역할명은 1~100자로 입력하세요.");
        }
        if (description != null && description.length() > 500) {
            throw BusinessException.field("description", "설명은 500자 이내로 입력하세요.");
        }
        if (!"Y".equals(useYn) && !"N".equals(useYn)) {
            throw BusinessException.field("useYn", "사용 여부를 고르세요.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String key(long menuId, String actionCd) {
        return menuId + ":" + actionCd;
    }

    private static List<PermissionChange> changes(List<PermRow> rows) {
        return rows.stream().map(p -> new PermissionChange(p.menuId(), p.menuNm(), Action.valueOf(p.actionCd())))
                .toList();
    }

    private void audit(long adminId, String ipAddr, String action, long roleId, String summary, Object before,
                       Object after) {
        auditLogService.record(new AuditEntry(adminId, ROLE, action, "ROLE", String.valueOf(roleId), summary, before,
                after, null, ipAddr));
    }
}
