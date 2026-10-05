package egovframework.admin.auth;

/**
 * 관리자의 SSR 세션을 끊는다. 세션 저장소(Spring Session)는 웹 모듈에 있으므로 구현은 admin-web이 한다.
 */
public interface LoginTerminator {

    /**
     * @param keepSessionId 남겨 둘 세션 ID (SSR에서 비밀번호를 바꾼 현재 세션). 없으면 null
     */
    void terminateSessions(String loginId, String keepSessionId);
}
