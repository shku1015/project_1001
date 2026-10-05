package egovframework.admin.auth;

/**
 * 인증 관련 작은 타입 모음.
 */
public final class AuthTypes {

    private AuthTypes() {
    }

    /** 인증 방식 (코드 그룹 AUTH_TYPE) */
    public enum AuthType {
        SESSION, TOKEN
    }

    /** 액션 (코드 그룹 ACTION, docs/02-access-model.md 2절) */
    public enum Action {
        READ, CREATE, UPDATE, DELETE, EXCEL, PRIVACY
    }

    /** 요청한 브라우저 정보 (로그인 이력, 감사로그, Refresh Token에 남긴다) */
    public record ClientInfo(String ipAddr, String userAgent) {
    }

    /** 로그인 성공 결과 */
    public record LoginResult(long adminId, String loginId, boolean pwdChangeRequired) {
    }
}
