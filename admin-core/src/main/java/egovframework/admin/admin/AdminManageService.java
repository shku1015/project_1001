package egovframework.admin.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.admin.AdminManageMapper.DetailRow;
import egovframework.admin.admin.AdminManageMapper.GrantedRoleRow;
import egovframework.admin.admin.AdminManageMapper.ListRow;
import egovframework.admin.admin.AdminManageMapper.LoginHistRow;
import egovframework.admin.admin.AdminManageMapper.RoleRow;
import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.common.PageQuery;
import egovframework.admin.common.PageResult;
import egovframework.admin.common.TempPassword;

/**
 * 관리자관리 (docs/04-features/06-admin.md). 보호 규칙은 docs/02-access-model.md 5절
 * (R3 마지막 슈퍼관리자, R4 본인 역할 변경 금지, R5 권한 상향 금지).
 * 역할·상태가 바뀌면 그 관리자의 권한 캐시를 비우고, 사용중지·비밀번호 초기화는 현재 로그인을 모두 끊는다 (BR-09).
 */
@Service
public class AdminManageService {

    private static final String ADMIN = "ADMIN";
    private static final String SUPER_ADMIN = AdminAuthInfoService.SUPER_ADMIN_ROLE;
    private static final int HISTORY_LIMIT = 20;

    /** 목록 정렬 가능 필드 (화면 명세 SCR-ADM-01의 정렬 ○) */
    private static final Map<String, String> SORTABLE = Map.of(
            "loginId", "a.login_id", "adminNm", "a.admin_nm", "lastLoginDt", "a.last_login_dt", "regDt", "a.reg_dt");

    public record RoleRef(long roleId, String roleNm) {
    }

    public record AdminListItem(long adminId, String loginId, String adminNm, String deptNm, List<RoleRef> roles,
                                String statusCd, String statusNm, LocalDateTime lastLoginDt, LocalDateTime regDt) {
    }

    public record GrantedRole(long roleId, String roleCd, String roleNm, String useYn, String regNm,
                              LocalDateTime regDt) {
    }

    public record AdminDetail(long adminId, String loginId, String adminNm, String email, String mobileNo,
                              String deptNm, String statusCd, String statusNm, int loginFailCnt, String pwdTempYn,
                              LocalDateTime pwdChangedDt, LocalDateTime lastLoginDt, List<GrantedRole> roles,
                              boolean self, String regNm, LocalDateTime regDt, String modNm, LocalDateTime modDt) {
    }

    public record RoleOption(long roleId, String roleCd, String roleNm, String useYn, boolean assignable) {
    }

    public record CreateCommand(String loginId, String adminNm, String email, String mobileNo, String deptNm,
                                List<Long> roleIds) {
    }

    public record UpdateCommand(String adminNm, String email, String mobileNo, String deptNm, LocalDateTime modDt) {
    }

    public record Created(long adminId, String tempPassword) {
    }

    private final AdminManageMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AdminAuthInfoService authInfoService;
    private final AuthService authService;

    public AdminManageService(AdminManageMapper mapper, PasswordEncoder passwordEncoder,
                              AuditLogService auditLogService, AdminAuthInfoService authInfoService,
                              AuthService authService) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.authInfoService = authInfoService;
        this.authService = authService;
    }

    // ================= 조회 =================

    /** ADM-01 관리자 목록. 기본 정렬은 등록일시 역순 */
    @Transactional(readOnly = true)
    public PageResult<AdminListItem> getAdmins(AdminManageMapper.Search search, Integer page, Integer size,
                                               List<String> sort) {
        PageQuery query = PageQuery.of(page, size, sort, SORTABLE, "a.reg_dt DESC", "a.admin_id DESC");
        long total = mapper.countAdmins(search);
        List<ListRow> rows = total == 0 ? List.of() : mapper.selectAdmins(search, query, query.offset());
        Map<Long, List<RoleRef>> roles = new HashMap<>();
        if (!rows.isEmpty()) {
            mapper.selectRolesOf(rows.stream().map(ListRow::adminId).toList()).forEach(r ->
                    roles.computeIfAbsent(r.adminId(), k -> new ArrayList<>()).add(new RoleRef(r.roleId(), r.roleNm())));
        }
        return PageResult.of(rows.stream().map(r -> new AdminListItem(r.adminId(), r.loginId(), r.adminNm(),
                r.deptNm(), roles.getOrDefault(r.adminId(), List.of()), r.statusCd(), r.statusNm(), r.lastLoginDt(),
                r.regDt())).toList(), query, total);
    }

    /** ADM-02 관리자 상세 */
    @Transactional(readOnly = true)
    public AdminDetail getAdmin(long myAdminId, long adminId) {
        DetailRow a = find(adminId);
        List<GrantedRole> roles = mapper.selectGrantedRoles(adminId).stream()
                .map(r -> new GrantedRole(r.roleId(), r.roleCd(), r.roleNm(), r.useYn(), r.regNm(), r.regDt()))
                .toList();
        return new AdminDetail(a.adminId(), a.loginId(), a.adminNm(), a.email(), a.mobileNo(), a.deptNm(),
                a.statusCd(), a.statusNm(), a.loginFailCnt(), a.pwdTempYn(), a.pwdChangedDt(), a.lastLoginDt(),
                roles, myAdminId == adminId, a.regNm(), a.regDt(), a.modNm(), a.modDt());
    }

    @Transactional(readOnly = true)
    public boolean isLoginIdAvailable(String loginId) {
        return !mapper.existsLoginId(loginId);
    }

    /** 역할 선택 목록. assignable: 사용 중이고 내 권한 범위 안의 역할 (R5). 슈퍼관리자 역할은 슈퍼관리자만 줄 수 있다 */
    @Transactional(readOnly = true)
    public List<RoleOption> getRoleOptions(long myAdminId) {
        AdminAuthInfo me = authInfoService.load(myAdminId);
        Map<Long, List<AdminManageMapper.RolePermRow>> perms = rolePermissions();
        return mapper.selectRoles().stream().map(r -> new RoleOption(r.roleId(), r.roleCd(), r.roleNm(), r.useYn(),
                "Y".equals(r.useYn()) && withinMyPermissions(me, r, perms))).toList();
    }

    /** ADM-09 최근 로그인 이력 20건 */
    @Transactional(readOnly = true)
    public List<LoginHistRow> getLoginHistories(long adminId) {
        find(adminId);
        return mapper.selectLoginHistories(adminId, HISTORY_LIMIT);
    }

    // ================= 등록·수정 =================

    /** ADM-03 관리자 등록. 임시 비밀번호를 만들어 한 번만 돌려준다 (BR-02) */
    @Transactional
    public Created create(long myAdminId, String ipAddr, CreateCommand cmd) {
        if (cmd.loginId() == null || !cmd.loginId().matches("^[a-z0-9]{4,50}$")) {
            throw BusinessException.field("loginId", "로그인 아이디는 영문 소문자·숫자 4~50자입니다.");
        }
        validateInfo(cmd.adminNm(), cmd.email(), cmd.mobileNo(), cmd.deptNm());
        if (cmd.roleIds() == null || cmd.roleIds().isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_REQUIRED);
        }
        if (mapper.existsLoginId(cmd.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 로그인 아이디입니다.");
        }
        Set<Long> roleIds = new LinkedHashSet<>(cmd.roleIds());
        for (long roleId : roleIds) {
            checkAssignable(myAdminId, roleId);
        }
        String tempPassword = TempPassword.generate();
        long adminId = mapper.insertAdmin(cmd.loginId(), passwordEncoder.encode(tempPassword), cmd.adminNm().trim(),
                cmd.email().trim(), blankToNull(cmd.mobileNo()), blankToNull(cmd.deptNm()), myAdminId);
        roleIds.forEach(roleId -> mapper.insertAdminRole(adminId, roleId, myAdminId));
        audit(myAdminId, ipAddr, "CREATE", adminId, "관리자 등록: " + cmd.loginId(), null,
                Map.of("loginId", cmd.loginId(), "adminNm", cmd.adminNm().trim(), "email", cmd.email().trim(),
                        "roleIds", roleIds));
        return new Created(adminId, tempPassword);
    }

    /** ADM-04 정보 수정. 로그인 아이디·역할은 여기서 바꾸지 않는다 */
    @Transactional
    public void update(long myAdminId, String ipAddr, long adminId, UpdateCommand cmd) {
        DetailRow before = find(adminId);
        validateInfo(cmd.adminNm(), cmd.email(), cmd.mobileNo(), cmd.deptNm());
        if (mapper.updateInfo(adminId, cmd.adminNm().trim(), cmd.email().trim(), blankToNull(cmd.mobileNo()),
                blankToNull(cmd.deptNm()), myAdminId, cmd.modDt()) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(myAdminId, ipAddr, "UPDATE", adminId, "관리자 정보 수정: " + before.loginId(),
                info(before.adminNm(), before.email(), before.mobileNo(), before.deptNm()),
                info(cmd.adminNm().trim(), cmd.email().trim(), blankToNull(cmd.mobileNo()), blankToNull(cmd.deptNm())));
    }

    // ================= 역할 부여·회수 (ADM-05) =================

    @Transactional
    public void grantRole(long myAdminId, String ipAddr, long adminId, long roleId) {
        DetailRow admin = find(adminId);
        if (myAdminId == adminId) {
            throw new BusinessException(ErrorCode.SELF_ROLE_CHANGE);
        }
        RoleRow role = checkAssignable(myAdminId, roleId);
        if (mapper.insertAdminRole(adminId, roleId, myAdminId) == 0) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 가진 역할입니다.");
        }
        audit(myAdminId, ipAddr, "UPDATE", adminId, "역할 부여: " + admin.loginId() + " ← " + role.roleNm(), null,
                Map.of("addedRole", role.roleCd()));
        authInfoService.evict(adminId);
    }

    @Transactional
    public void revokeRole(long myAdminId, String ipAddr, long adminId, long roleId) {
        DetailRow admin = find(adminId);
        if (myAdminId == adminId) {
            throw new BusinessException(ErrorCode.SELF_ROLE_CHANGE);
        }
        List<GrantedRoleRow> roles = mapper.selectGrantedRoles(adminId);
        GrantedRoleRow role = roles.stream().filter(r -> r.roleId() == roleId).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "부여되지 않은 역할입니다."));
        if (roles.size() == 1) {
            throw new BusinessException(ErrorCode.ROLE_REQUIRED);
        }
        if (SUPER_ADMIN.equals(role.roleCd()) && isLastSuperAdmin(admin)) {
            throw new BusinessException(ErrorCode.LAST_SUPER_ADMIN);
        }
        mapper.deleteAdminRole(adminId, roleId);
        audit(myAdminId, ipAddr, "UPDATE", adminId, "역할 회수: " + admin.loginId() + " → " + role.roleNm(),
                Map.of("removedRole", role.roleCd()), null);
        authInfoService.evict(adminId);
    }

    // ================= 상태 (ADM-06, 07, 08) =================

    /** ADM-06 잠금 해제. 잠금 상태만. 실패 횟수를 0으로 */
    @Transactional
    public void unlock(long myAdminId, String ipAddr, long adminId) {
        DetailRow admin = find(adminId);
        if (!"LOCKED".equals(admin.statusCd())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_CHANGE);
        }
        mapper.updateStatus(adminId, "ACTIVE", true, myAdminId, null);
        audit(myAdminId, ipAddr, "UPDATE", adminId, "잠금 해제: " + admin.loginId(), Map.of("statusCd", "LOCKED"),
                Map.of("statusCd", "ACTIVE"));
        authInfoService.evict(adminId);
    }

    /** ADM-07 비밀번호 초기화. 사용중지 상태는 불가. 대상의 로그인을 모두 끊는다 (BR-09) */
    @Transactional
    public String resetPassword(long myAdminId, String ipAddr, long adminId) {
        DetailRow admin = find(adminId);
        if ("DISABLED".equals(admin.statusCd())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_CHANGE);
        }
        String tempPassword = TempPassword.generate();
        mapper.updateTempPassword(adminId, passwordEncoder.encode(tempPassword), myAdminId);
        // 비밀번호 값은 감사로그에 남기지 않는다
        audit(myAdminId, ipAddr, "UPDATE", adminId, "비밀번호 초기화: " + admin.loginId(), null, null);
        authService.terminateOtherLogins(adminId, admin.loginId(), null);
        return tempPassword;
    }

    /**
     * ADM-08 사용중지(ACTIVE·LOCKED → DISABLED) / 재사용(DISABLED → ACTIVE).
     * 본인 사용중지 불가(BR-05), 마지막 슈퍼관리자 사용중지 불가(BR-07). 사용중지하면 로그인을 모두 끊는다 (BR-09).
     */
    @Transactional
    public void changeStatus(long myAdminId, String ipAddr, long adminId, String statusCd, LocalDateTime modDt) {
        DetailRow admin = find(adminId);
        boolean disable = "DISABLED".equals(statusCd);
        boolean allowed = disable ? !"DISABLED".equals(admin.statusCd())
                : "ACTIVE".equals(statusCd) && "DISABLED".equals(admin.statusCd());
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_CHANGE);
        }
        if (disable && myAdminId == adminId) {
            throw new BusinessException(ErrorCode.SELF_DISABLE);
        }
        if (disable && isLastSuperAdmin(admin)) {
            throw new BusinessException(ErrorCode.LAST_SUPER_ADMIN);
        }
        if (mapper.updateStatus(adminId, statusCd, !disable, myAdminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(myAdminId, ipAddr, "UPDATE", adminId, (disable ? "사용중지: " : "재사용: ") + admin.loginId(),
                Map.of("statusCd", admin.statusCd()), Map.of("statusCd", statusCd));
        if (disable) {
            authService.terminateOtherLogins(adminId, admin.loginId(), null);
        } else {
            authInfoService.evict(adminId);
        }
    }

    // ================= 공통 =================

    private DetailRow find(long adminId) {
        DetailRow a = mapper.selectAdmin(adminId);
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "관리자가 없습니다: " + adminId);
        }
        return a;
    }

    /** 사용 중인 역할이고(BR-08 역할관리), 내 권한 범위 안이어야 부여할 수 있다 (R5) */
    private RoleRow checkAssignable(long myAdminId, long roleId) {
        RoleRow role = mapper.selectRoles().stream().filter(r -> r.roleId() == roleId).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "역할이 없습니다: " + roleId));
        if (!"Y".equals(role.useYn())) {
            throw new BusinessException(ErrorCode.ROLE_NOT_USABLE);
        }
        if (!withinMyPermissions(authInfoService.load(myAdminId), role, rolePermissions())) {
            throw new BusinessException(ErrorCode.PRIVILEGE_ESCALATION);
        }
        return role;
    }

    private static boolean withinMyPermissions(AdminAuthInfo me, RoleRow role,
                                               Map<Long, List<AdminManageMapper.RolePermRow>> perms) {
        if (me.superAdmin()) {
            return true;
        }
        if ("Y".equals(role.systemYn())) {
            return false;
        }
        return perms.getOrDefault(role.roleId(), List.of()).stream()
                .allMatch(p -> me.has(p.menuCd(), Action.valueOf(p.actionCd())));
    }

    private Map<Long, List<AdminManageMapper.RolePermRow>> rolePermissions() {
        Map<Long, List<AdminManageMapper.RolePermRow>> result = new HashMap<>();
        mapper.selectRolePermissions().forEach(p -> result.computeIfAbsent(p.roleId(), k -> new ArrayList<>()).add(p));
        return result;
    }

    /** 대상이 사용 중인 슈퍼관리자이고, 그런 관리자가 1명뿐인지 (R3) */
    private boolean isLastSuperAdmin(DetailRow admin) {
        boolean activeSuper = "ACTIVE".equals(admin.statusCd()) && mapper.selectGrantedRoles(admin.adminId()).stream()
                .anyMatch(r -> SUPER_ADMIN.equals(r.roleCd()) && "Y".equals(r.useYn()));
        return activeSuper && mapper.countActiveSuperAdmins() <= 1;
    }

    private static void validateInfo(String adminNm, String email, String mobileNo, String deptNm) {
        if (adminNm == null || adminNm.isBlank() || adminNm.trim().length() > 50) {
            throw BusinessException.field("adminNm", "이름은 1~50자로 입력하세요.");
        }
        if (email == null || email.trim().length() > 100 || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw BusinessException.field("email", "이메일 형식이 올바르지 않습니다.");
        }
        if (mobileNo != null && !mobileNo.isBlank() && !mobileNo.matches("^[0-9]{10,11}$")) {
            throw BusinessException.field("mobileNo", "휴대폰 번호는 숫자 10~11자리입니다.");
        }
        if (deptNm != null && deptNm.length() > 100) {
            throw BusinessException.field("deptNm", "부서는 100자 이내로 입력하세요.");
        }
    }

    private static Map<String, String> info(String adminNm, String email, String mobileNo, String deptNm) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("adminNm", adminNm);
        m.put("email", email);
        m.put("mobileNo", mobileNo);
        m.put("deptNm", deptNm);
        return m;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(long myAdminId, String ipAddr, String action, long adminId, String summary, Object before,
                       Object after) {
        auditLogService.record(new AuditEntry(myAdminId, ADMIN, action, "ADMIN", String.valueOf(adminId), summary,
                before, after, null, ipAddr));
    }
}
