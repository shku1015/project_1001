package egovframework.admin.web.ssr;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.admin.auth.MeService;
import egovframework.admin.common.BusinessException;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.AllowTempPassword;
import egovframework.admin.web.security.LoginOnly;
import egovframework.admin.web.security.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 로그인·홈·비밀번호 변경 (SCR-AUTH-01, SCR-HOME, SCR-AUTH-02).
 * 로그인 처리(POST /ssr/login)와 로그아웃(POST /ssr/logout)은 Spring Security가 맡는다.
 * 화면 디자인(Tabler)은 화면 단계에서 입힌다.
 */
@Controller
public class SsrAuthController {

    private final MeService meService;

    public SsrAuthController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping("/ssr/login")
    @PublicEndpoint
    public String loginForm() {
        return "ssr/login";
    }

    @GetMapping({"/ssr", "/ssr/"})
    @LoginOnly
    public String home(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("me", meService.getMe(principal.adminId()));
        return "ssr/home";
    }

    @GetMapping("/ssr/password")
    @LoginOnly
    @AllowTempPassword
    public String passwordForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("me", meService.getMe(principal.adminId()));
        return "ssr/password";
    }

    /** 다른 로그인은 끊고 현재 세션은 유지한다 */
    @PostMapping("/ssr/password")
    @LoginOnly
    @AllowTempPassword
    public String changePassword(@AuthenticationPrincipal AdminPrincipal principal,
                                 @RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String newPasswordConfirm, HttpServletRequest request,
                                 RedirectAttributes redirect, Model model) {
        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("me", meService.getMe(principal.adminId()));
            model.addAttribute("fieldError", "새 비밀번호 확인이 일치하지 않습니다.");
            return "ssr/password";
        }
        try {
            meService.changePassword(principal.adminId(), currentPassword, newPassword,
                    request.getSession().getId(), request.getRemoteAddr());
        } catch (BusinessException e) {
            model.addAttribute("me", meService.getMe(principal.adminId()));
            model.addAttribute("fieldError", e.getFieldErrors().isEmpty()
                    ? e.getMessage() : e.getFieldErrors().get(0).message());
            return "ssr/password";
        }
        redirect.addFlashAttribute("notice", "비밀번호가 변경되었습니다");
        return "redirect:/ssr/";
    }
}
