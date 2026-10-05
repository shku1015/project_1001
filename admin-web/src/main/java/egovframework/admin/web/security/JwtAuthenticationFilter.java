package egovframework.admin.web.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.common.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization: Bearer 토큰으로 인증한다 (/api/** 전용, ADR-0005).
 * 토큰이 유효해도 관리자가 사용 상태가 아니면 인증하지 않는다.
 * 실패 이유는 요청 속성에 남겨 401 응답의 오류 코드로 쓴다 (TOKEN_EXPIRED / UNAUTHORIZED).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTR = JwtAuthenticationFilter.class.getName() + ".error";

    private final JwtProvider jwtProvider;
    private final AdminAuthInfoService authInfoService;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, AdminAuthInfoService authInfoService) {
        this.jwtProvider = jwtProvider;
        this.authInfoService = authInfoService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AdminPrincipal principal = jwtProvider.parse(header.substring(7));
                AdminAuthInfo auth = authInfoService.load(principal.adminId());
                if (auth != null && auth.active()) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
                } else {
                    request.setAttribute(AUTH_ERROR_ATTR, ErrorCode.UNAUTHORIZED);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(AUTH_ERROR_ATTR, ErrorCode.TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(AUTH_ERROR_ATTR, ErrorCode.UNAUTHORIZED);
            }
        }
        chain.doFilter(request, response);
    }
}
