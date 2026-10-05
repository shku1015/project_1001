package egovframework.admin.web.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 컨트롤러 메서드 단위 권한 확인 (docs/02-access-model.md 4절). /api/**, /ssr/** 에 적용한다.
 * - @PublicEndpoint: 통과
 * - 임시 비밀번호 상태: @AllowTempPassword가 없으면 막는다
 * - @RequirePermission: 메뉴 × 액션 권한 확인
 * - @LoginOnly: 로그인만 확인
 * - 아무 표시도 없으면 기본 거부
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    static final String SSR_PACKAGE = "egovframework.admin.web.ssr";

    private final AdminAuthInfoService authInfoService;

    public PermissionInterceptor(AdminAuthInfoService authInfoService) {
        this.authInfoService = authInfoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod method) || method.hasMethodAnnotation(PublicEndpoint.class)) {
            return true;
        }
        boolean ssr = method.getBeanType().getPackageName().startsWith(SSR_PACKAGE);

        AdminAuthInfo auth = currentAuth();
        if (auth == null || !auth.active()) {
            if (ssr) {
                request.getSession().invalidate();
                response.sendRedirect(request.getContextPath() + "/ssr/login");
                return false;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (auth.pwdTemp() && !method.hasMethodAnnotation(AllowTempPassword.class)) {
            if (ssr) {
                response.sendRedirect(request.getContextPath() + "/ssr/password");
                return false;
            }
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        }

        RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
        if (required != null) {
            if (!auth.hasAny(resolveMenus(required.menu(), request), required.action())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return true;
        }
        if (method.hasMethodAnnotation(LoginOnly.class)) {
            return true;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "권한 설정이 없는 기능입니다.");    // 기본 거부
    }

    private AdminAuthInfo currentAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal) {
            return authInfoService.load(principal.adminId());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<String> resolveMenus(String[] menus, HttpServletRequest request) {
        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return Arrays.stream(menus).map(menu -> {
            String resolved = menu;
            if (vars != null) {
                for (Map.Entry<String, String> var : vars.entrySet()) {
                    resolved = resolved.replace("{" + var.getKey() + "}", var.getValue());
                }
            }
            return resolved;
        }).toList();
    }
}
