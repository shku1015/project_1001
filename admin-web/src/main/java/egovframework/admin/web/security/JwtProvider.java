package egovframework.admin.web.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import egovframework.admin.web.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Access Token(JWT, HS256). 관리자 ID와 발급·만료 시각만 담는다. 권한은 담지 않는다 (ADR-0004).
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final AuthProperties props;

    public JwtProvider(AuthProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String create(AdminPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.adminId()))
                .claim("loginId", principal.loginId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    /** 서명·만료를 확인한다. 만료면 ExpiredJwtException, 그 밖의 문제는 JwtException */
    public AdminPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AdminPrincipal(Long.parseLong(claims.getSubject()), claims.get("loginId", String.class));
    }

    public long accessTokenSeconds() {
        return props.accessTokenTtl().toSeconds();
    }
}
