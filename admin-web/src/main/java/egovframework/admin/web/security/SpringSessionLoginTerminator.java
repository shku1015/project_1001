package egovframework.admin.web.security;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import egovframework.admin.auth.LoginTerminator;

/**
 * 관리자의 SSR 세션을 DB(Spring Session JDBC)에서 찾아 지운다 (동시 로그인 금지, 강제 로그아웃).
 */
@Component
public class SpringSessionLoginTerminator implements LoginTerminator {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SpringSessionLoginTerminator(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void terminateSessions(String loginId, String keepSessionId) {
        sessions.findByPrincipalName(loginId).keySet().stream()
                .filter(id -> !id.equals(keepSessionId))
                .forEach(sessions::deleteById);
    }
}
