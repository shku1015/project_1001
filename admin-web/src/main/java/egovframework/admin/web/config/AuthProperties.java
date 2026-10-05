package egovframework.admin.web.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 설정 (app.auth.*). 비밀값은 환경 변수로만 넣는다.
 *
 * @param jwtSecret             Access Token 서명 키 (HS256, 32바이트 이상). 환경 변수 JWT_SECRET
 * @param accessTokenTtl        Access Token 유효 시간 (NF 1.4: 30분)
 * @param refreshTokenTtl       Refresh Token 최대 유효 시간, 처음 로그인 기준 (NF 1.4: 8시간)
 * @param cookieSecure          Refresh Token 쿠키 Secure (운영 HTTPS: true, 로컬 HTTP: false)
 * @param allowedOrigins        토큰 API(쿠키 사용)에 추가로 허용할 Origin. 같은 호스트는 항상 허용
 * @param initialAdminPassword  최초 관리자 admin 비밀번호. 환경 변수 INITIAL_ADMIN_PASSWORD
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(String jwtSecret, Duration accessTokenTtl, Duration refreshTokenTtl, boolean cookieSecure,
                             List<String> allowedOrigins, String initialAdminPassword) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.auth.jwt-secret(환경 변수 JWT_SECRET)이 없거나 32바이트보다 짧습니다. 예: openssl rand -base64 48");
        }
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(30) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofHours(8) : refreshTokenTtl;
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
