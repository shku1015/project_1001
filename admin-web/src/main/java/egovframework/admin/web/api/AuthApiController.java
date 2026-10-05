package egovframework.admin.web.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthService;
import egovframework.admin.auth.AuthTypes.AuthType;
import egovframework.admin.auth.AuthTypes.ClientInfo;
import egovframework.admin.auth.AuthTypes.LoginResult;
import egovframework.admin.auth.MeService;
import egovframework.admin.auth.RefreshTokenService;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.web.config.AuthProperties;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.AllowTempPassword;
import egovframework.admin.web.security.ClientInfos;
import egovframework.admin.web.security.LoginOnly;
import egovframework.admin.web.security.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 API (docs/06-api/00-auth.md, api/openapi.yaml auth 태그). ①② 토큰 방식.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    public record LoginRequest(@NotBlank @Size(max = 50) String loginId, @NotBlank @Size(max = 100) String password) {
    }

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AdminAuthInfoService authInfoService;
    private final MeService meService;
    private final TokenIssuer tokenIssuer;
    private final TokenCookies cookies;
    private final AuthProperties props;

    public AuthApiController(AuthService authService, RefreshTokenService refreshTokenService,
                             AdminAuthInfoService authInfoService, MeService meService, TokenIssuer tokenIssuer,
                             TokenCookies cookies, AuthProperties props) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.authInfoService = authInfoService;
        this.meService = meService;
        this.tokenIssuer = tokenIssuer;
        this.cookies = cookies;
        this.props = props;
    }

    @PostMapping("/token")
    @PublicEndpoint
    public ApiResponse<TokenIssuer.Token> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                                    HttpServletResponse response) {
        checkOrigin(request);
        ClientInfo client = ClientInfos.of(request);
        LoginResult result = authService.login(req.loginId(), req.password(), AuthType.TOKEN, client);
        AdminPrincipal principal = new AdminPrincipal(result.adminId(), result.loginId());
        return ApiResponse.ok(tokenIssuer.issue(principal, result.pwdChangeRequired(), client, response));
    }

    @PostMapping("/token/refresh")
    @PublicEndpoint
    public ApiResponse<TokenIssuer.Token> refresh(HttpServletRequest request, HttpServletResponse response) {
        checkOrigin(request);
        RefreshTokenService.Rotation rotation;
        try {
            rotation = refreshTokenService.rotate(cookies.read(request), ClientInfos.of(request));
        } catch (BusinessException e) {
            cookies.clear(response);
            throw e;
        }
        AdminAuthInfo auth = authInfoService.load(rotation.adminId());
        if (auth == null || !auth.active()) {
            refreshTokenService.revokeAll(rotation.adminId());
            cookies.clear(response);
            throw new BusinessException(ErrorCode.REFRESH_FAILED);
        }
        cookies.set(response, rotation.newToken());
        return ApiResponse.ok(tokenIssuer.accessToken(new AdminPrincipal(auth.adminId(), auth.loginId()),
                auth.pwdTemp()));
    }

    @DeleteMapping("/token")
    @LoginOnly
    @AllowTempPassword
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenService.revoke(cookies.read(request));
        cookies.clear(response);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    @LoginOnly
    @AllowTempPassword
    public ApiResponse<MeService.Me> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(meService.getMe(principal.adminId()));
    }

    /**
     * 쿠키를 쓰는 토큰 API의 CSRF 방어 (NF-WS-05): SameSite=Strict에 더해 Origin을 확인한다.
     * Origin이 없으면(같은 출처의 일부 요청, 테스트 도구) 통과시킨다.
     */
    private void checkOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            return;
        }
        String self = request.getScheme() + "://" + request.getHeader("Host");
        if (!origin.equals(self) && !props.allowedOrigins().contains(origin)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "허용되지 않은 출처의 요청입니다.");
        }
    }
}
