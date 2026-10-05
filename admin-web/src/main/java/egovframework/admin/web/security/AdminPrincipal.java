package egovframework.admin.web.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * 로그인한 관리자. 권한은 담지 않는다 (요청마다 AdminAuthInfoService로 확인, ADR-0004).
 * SSR 세션(Spring Session JDBC)에 저장되므로 Serializable이고, 세션을 관리자별로 찾도록 getName()은 로그인 아이디다.
 */
public record AdminPrincipal(long adminId, String loginId) implements Principal, Serializable {

    @Override
    public String getName() {
        return loginId;
    }
}
