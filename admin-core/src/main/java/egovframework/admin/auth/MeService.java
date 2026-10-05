package egovframework.admin.auth;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.menu.MenuService;
import egovframework.admin.menu.MenuService.MenuNode;

/**
 * 내 정보 (docs/04-features/09-auth.md AUTH-04~06).
 */
@Service
public class MeService {

    public record RoleSummary(String roleCd, String roleNm) {
    }

    public record Me(long adminId, String loginId, String adminNm, String email, String mobileNo, String deptNm,
                     List<RoleSummary> roles, boolean superAdmin, LocalDateTime lastLoginDt, String lastLoginIp,
                     LocalDateTime pwdChangedDt, boolean pwdChangeRequired, List<MenuNode> menus,
                     Map<String, List<Action>> permissions, LocalDateTime modDt) {
    }

    public record MyInfo(String adminNm, String email, String mobileNo, String deptNm, LocalDateTime modDt) {
    }

    /** 내 정보 메뉴는 없으므로 감사로그 메뉴 코드로 ME를 쓴다 */
    private static final String AUDIT_MENU = "ME";

    private final AuthMapper authMapper;
    private final AdminAuthInfoService authInfoService;
    private final MenuService menuService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public MeService(AuthMapper authMapper, AdminAuthInfoService authInfoService, MenuService menuService,
                     PasswordEncoder passwordEncoder, AuthService authService, AuditLogService auditLogService) {
        this.authMapper = authMapper;
        this.authInfoService = authInfoService;
        this.menuService = menuService;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Me getMe(long adminId) {
        AuthMapper.AdminProfile p = authMapper.selectAdminProfile(adminId);
        if (p == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AdminAuthInfo auth = authInfoService.load(adminId);
        AuthMapper.LoginHistRow prev = authMapper.selectPreviousLogin(adminId);
        Map<String, List<Action>> permissions = new TreeMap<>();
        auth.permissions().forEach((menuCd, actions) -> permissions.put(menuCd, List.copyOf(new TreeSet<>(actions))));
        List<RoleSummary> roles = auth.roles().stream().map(r -> new RoleSummary(r.roleCd(), r.roleNm())).toList();
        return new Me(p.adminId(), p.loginId(), p.adminNm(), p.email(), p.mobileNo(), p.deptNm(), roles,
                auth.superAdmin(), prev == null ? null : prev.regDt(), prev == null ? null : prev.ipAddr(),
                p.pwdChangedDt(), "Y".equals(p.pwdTempYn()), menuService.getMyMenuTree(auth), permissions, p.modDt());
    }

    @Transactional
    public void updateMyInfo(long adminId, MyInfo info, String ipAddr) {
        AuthMapper.AdminProfile before = authMapper.selectAdminProfile(adminId);
        int updated = authMapper.updateMyInfo(adminId, info.adminNm(), info.email(), blankToNull(info.mobileNo()),
                blankToNull(info.deptNm()), info.modDt());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        auditLogService.record(new AuditEntry(adminId, AUDIT_MENU, "UPDATE", "ADMIN", String.valueOf(adminId),
                "내 정보 수정", myInfoOf(before.adminNm(), before.email(), before.mobileNo(), before.deptNm()),
                myInfoOf(info.adminNm(), info.email(), blankToNull(info.mobileNo()), blankToNull(info.deptNm())),
                null, ipAddr));
    }

    /**
     * 비밀번호를 바꾸고 이 관리자의 모든 로그인(세션·Refresh Token)을 끊는다.
     * 현재 로그인을 이어가는 것은 호출한 쪽(API: 새 토큰 발급, SSR: 현재 세션 유지)이 맡는다.
     */
    @Transactional
    public void changePassword(long adminId, String currentPassword, String newPassword, String keepSessionId,
                               String ipAddr) {
        String hash = authMapper.selectPasswordHash(adminId);
        if (hash == null || !passwordEncoder.matches(currentPassword, hash)) {
            throw BusinessException.field("currentPassword", "현재 비밀번호가 올바르지 않습니다.");
        }
        if (newPassword == null || newPassword.isEmpty() || newPassword.length() > 20) {
            throw BusinessException.field("newPassword", "비밀번호는 1~20자입니다.");
        }
        if (passwordEncoder.matches(newPassword, hash)) {
            throw BusinessException.field("newPassword", "현재 비밀번호와 다른 값을 입력하세요.");
        }
        authMapper.updatePassword(adminId, passwordEncoder.encode(newPassword));
        AuthMapper.AdminProfile p = authMapper.selectAdminProfile(adminId);
        authService.terminateOtherLogins(adminId, p.loginId(), keepSessionId);
        auditLogService.record(new AuditEntry(adminId, AUDIT_MENU, "UPDATE", "ADMIN", String.valueOf(adminId),
                "비밀번호 변경", null, null, null, ipAddr));
    }

    /** 관리자 정보는 마스킹 대상이 아니다 (docs/05-screens/06-admin.md) */
    private static Map<String, String> myInfoOf(String adminNm, String email, String mobileNo, String deptNm) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("adminNm", adminNm);
        map.put("email", email);
        map.put("mobileNo", mobileNo);
        map.put("deptNm", deptNm);
        return map;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
