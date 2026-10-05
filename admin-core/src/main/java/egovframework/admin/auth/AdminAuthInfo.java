package egovframework.admin.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import egovframework.admin.auth.AuthTypes.Action;

/**
 * 요청마다 확인하는 관리자 인증 정보: 상태, 임시 비밀번호 여부, 최종 권한(역할 합집합).
 * 토큰에 권한을 넣지 않고 이 값을 서버에서 확인한다 (ADR-0004). AdminAuthInfoService가 캐시한다.
 */
public record AdminAuthInfo(long adminId, String loginId, String statusCd, boolean pwdTemp, boolean superAdmin,
                            List<AuthMapper.RoleRow> roles, Map<String, Set<Action>> permissions) {

    public boolean active() {
        return "ACTIVE".equals(statusCd);
    }

    /** 슈퍼관리자는 권한 체크 예외 (ADR-0002) */
    public boolean has(String menuCd, Action action) {
        return superAdmin || permissions.getOrDefault(menuCd, Set.of()).contains(action);
    }

    /** 여러 메뉴 중 하나라도 권한이 있으면 통과 (게시글: POST 또는 POST_{게시판 코드}) */
    public boolean hasAny(Collection<String> menuCds, Action action) {
        return menuCds.stream().anyMatch(menuCd -> has(menuCd, action));
    }
}
