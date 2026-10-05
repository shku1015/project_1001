package egovframework.admin.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.auth.AuthTypes.AuthType;
import egovframework.admin.auth.AuthTypes.ClientInfo;
import egovframework.admin.auth.AuthTypes.LoginResult;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;

/**
 * 로그인 처리 (docs/04-features/09-auth.md 3절). 세션(③)과 토큰(①②)이 같은 규칙을 쓴다.
 */
@Service
public class AuthService {

    /** 비밀번호 연속 실패 잠금 기준 (NF-LG-01) */
    public static final int LOCK_THRESHOLD = 5;

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginTerminator loginTerminator;
    private final AdminAuthInfoService authInfoService;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService,
                       LoginTerminator loginTerminator, AdminAuthInfoService authInfoService) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.loginTerminator = loginTerminator;
        this.authInfoService = authInfoService;
    }

    /**
     * 실패해도 실패 횟수·잠금·로그인 이력은 저장해야 하므로 업무 예외로 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResult login(String loginId, String rawPassword, AuthType authType, ClientInfo client) {
        AuthMapper.LoginAdmin admin = authMapper.selectLoginAdmin(loginId);
        if (admin == null) {
            history(null, loginId, "FAIL_NO_ID", authType, client);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);    // BR-01: 아이디가 없어도 같은 메시지
        }
        if ("DISABLED".equals(admin.statusCd())) {
            history(admin.adminId(), loginId, "FAIL_DISABLED", authType, client);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if ("LOCKED".equals(admin.statusCd())) {
            history(admin.adminId(), loginId, "FAIL_LOCKED", authType, client);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(rawPassword, admin.password())) {
            authMapper.increaseLoginFail(admin.adminId(), LOCK_THRESHOLD);
            history(admin.adminId(), loginId, "FAIL_PWD", authType, client);
            authInfoService.evict(admin.adminId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        authMapper.updateLoginSuccess(admin.adminId());
        history(admin.adminId(), loginId, "SUCCESS", authType, client);
        terminateOtherLogins(admin.adminId(), loginId, null);    // NF-LG-02: 동시 로그인 금지
        return new LoginResult(admin.adminId(), loginId, "Y".equals(admin.pwdTempYn()));
    }

    /**
     * 이 관리자의 로그인(세션·Refresh Token)을 모두 끊는다.
     * @param keepSessionId 남겨 둘 SSR 세션 ID. 없으면 null
     */
    @Transactional
    public void terminateOtherLogins(long adminId, String loginId, String keepSessionId) {
        refreshTokenService.revokeAll(adminId);
        loginTerminator.terminateSessions(loginId, keepSessionId);
        authInfoService.evict(adminId);
    }

    private void history(Long adminId, String loginId, String resultCd, AuthType authType, ClientInfo client) {
        authMapper.insertLoginHist(adminId, truncate(loginId, 50), resultCd, authType.name(), client.ipAddr(),
                truncate(client.userAgent(), 500));
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
