package egovframework.admin.web.api;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import egovframework.admin.auth.RefreshTokenService.IssuedToken;
import egovframework.admin.web.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Refresh Token 쿠키 (NF-WS-02): HttpOnly, Secure, SameSite=Strict, 토큰 API 경로에만 전송.
 */
@Component
public class TokenCookies {

    public static final String NAME = "refreshToken";
    public static final String PATH = "/api/v1/auth/token";

    private final AuthProperties props;

    public TokenCookies(AuthProperties props) {
        this.props = props;
    }

    public void set(HttpServletResponse response, IssuedToken token) {
        long seconds = Math.max(0, Duration.between(LocalDateTime.now(), token.expiresDt()).toSeconds());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(token.rawToken(), seconds).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
