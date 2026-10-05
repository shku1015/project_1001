package egovframework.admin.auth;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.auth.AuthTypes.Action;

/**
 * 관리자별 최종 권한 계산과 캐시 (NF-PF-04).
 * 역할·권한·메뉴·관리자 역할·관리자 상태가 바뀌면 evict/evictAll로 비운다. 다음 요청부터 반영된다.
 */
@Service
@Transactional(readOnly = true)
public class AdminAuthInfoService {

    public static final String CACHE = "adminAuthInfo";
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final AuthMapper authMapper;

    public AdminAuthInfoService(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    /** 없는 관리자면 null */
    @Cacheable(cacheNames = CACHE, key = "#adminId", unless = "#result == null")
    public AdminAuthInfo load(long adminId) {
        AuthMapper.AdminProfile profile = authMapper.selectAdminProfile(adminId);
        if (profile == null) {
            return null;
        }
        List<AuthMapper.RoleRow> roles = authMapper.selectRoles(adminId);
        boolean superAdmin = roles.stream()
                .anyMatch(r -> SUPER_ADMIN_ROLE.equals(r.roleCd()) && "Y".equals(r.useYn()));

        Map<String, Set<Action>> permissions = new LinkedHashMap<>();
        if (!superAdmin) {
            for (AuthMapper.PermissionRow row : authMapper.selectPermissions(adminId)) {
                permissions.computeIfAbsent(row.menuCd(), k -> EnumSet.noneOf(Action.class))
                        .add(Action.valueOf(row.actionCd()));
            }
        }
        permissions.replaceAll((k, v) -> Set.copyOf(v));
        return new AdminAuthInfo(adminId, profile.loginId(), profile.statusCd(), "Y".equals(profile.pwdTempYn()),
                superAdmin, List.copyOf(roles), Map.copyOf(permissions));
    }

    @CacheEvict(cacheNames = CACHE, key = "#adminId")
    public void evict(long adminId) {
        // 캐시만 비운다
    }

    /** 역할·권한·메뉴처럼 여러 관리자에게 영향을 주는 변경 후 */
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public void evictAll() {
        // 캐시만 비운다
    }
}
