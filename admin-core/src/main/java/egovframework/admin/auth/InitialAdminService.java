package egovframework.admin.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최초 관리자 admin 생성 (docs/03-initial-data.md 2절).
 * 비밀번호는 환경 변수로 받고, 임시 비밀번호로 저장해 첫 로그인 때 바꾸게 한다.
 */
@Service
public class InitialAdminService {

    public static final String LOGIN_ID = "admin";

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminService(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** @return 새로 만들었으면 true */
    @Transactional
    public boolean createIfAbsent(String rawPassword) {
        if (authMapper.existsLoginId(LOGIN_ID)) {
            return false;
        }
        authMapper.insertInitialAdmin(LOGIN_ID, passwordEncoder.encode(rawPassword), "최초관리자",
                "admin@example.com", AdminAuthInfoService.SUPER_ADMIN_ROLE);
        return true;
    }
}
