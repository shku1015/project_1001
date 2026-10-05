package egovframework.admin.web.api;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import egovframework.admin.auth.AuthTypes.ClientInfo;
import egovframework.admin.auth.RefreshTokenService;
import egovframework.admin.auth.RefreshTokenService.IssuedToken;
import egovframework.admin.web.config.AuthProperties;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 새 로그인(로그인, 비밀번호 변경) 때 Access Token과 Refresh Token 쿠키를 발급한다.
 */
@Component
public class TokenIssuer {

    public record Token(String accessToken, long expiresIn, boolean pwdChangeRequired) {
    }

    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final TokenCookies cookies;
    private final AuthProperties props;

    public TokenIssuer(RefreshTokenService refreshTokenService, JwtProvider jwtProvider, TokenCookies cookies,
                       AuthProperties props) {
        this.refreshTokenService = refreshTokenService;
        this.jwtProvider = jwtProvider;
        this.cookies = cookies;
        this.props = props;
    }

    /** Refresh Token 만료는 지금부터 최대 유효 시간 (이후 재발급은 이 시각을 넘지 않는다) */
    public Token issue(AdminPrincipal principal, boolean pwdChangeRequired, ClientInfo client,
                       HttpServletResponse response) {
        IssuedToken refresh = refreshTokenService.issue(principal.adminId(),
                LocalDateTime.now().plus(props.refreshTokenTtl()), client);
        cookies.set(response, refresh);
        return accessToken(principal, pwdChangeRequired);
    }

    public Token accessToken(AdminPrincipal principal, boolean pwdChangeRequired) {
        return new Token(jwtProvider.create(principal), jwtProvider.accessTokenSeconds(), pwdChangeRequired);
    }
}
