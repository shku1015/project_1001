package egovframework.admin.web.security;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import egovframework.admin.auth.AuthService;
import egovframework.admin.auth.AuthTypes.AuthType;
import egovframework.admin.auth.AuthTypes.LoginResult;
import egovframework.admin.common.BusinessException;

/**
 * ③ JSP SSR 로그인 폼 인증. API 로그인과 같은 AuthService 규칙(잠금, 이력, 동시 로그인 금지)을 쓴다.
 */
@Component
public class SessionAuthenticationProvider implements AuthenticationProvider {

    private final AuthService authService;

    public SessionAuthenticationProvider(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String loginId = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());
        try {
            LoginResult result = authService.login(loginId, password, AuthType.SESSION, ClientInfos.current());
            return new UsernamePasswordAuthenticationToken(new AdminPrincipal(result.adminId(), result.loginId()),
                    null, List.of());
        } catch (BusinessException e) {
            throw switch (e.getErrorCode()) {
                case ACCOUNT_LOCKED -> new LockedException(e.getMessage());
                case ACCOUNT_DISABLED -> new DisabledException(e.getMessage());
                default -> new BadCredentialsException(e.getMessage());
            };
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
