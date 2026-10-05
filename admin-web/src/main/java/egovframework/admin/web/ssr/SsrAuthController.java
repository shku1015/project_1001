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
    public String home() {
        return "ssr/home";
    }

    @GetMapping("/ssr/password")
    @LoginOnly
    @AllowTempPassword
    public String passwordForm() {
        return "ssr/password";
    }

    @GetMapping("/ssr/me")
    @LoginOnly
    public String myInfoForm() {
        return "ssr/me";
    }

    /** SCR-MY-01 내 정보 저장. 입력 오류·동시 수정 오류는 같은 화면에 다시 보여 준다 */
    @PostMapping("/ssr/me")
    @LoginOnly
    public String saveMyInfo(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam String adminNm,
                             @RequestParam String email, @RequestParam(required = false) String mobileNo,
                             @RequestParam(required = false) String deptNm, @RequestParam String modDt,
                             HttpServletRequest request, RedirectAttributes redirect, Model model) {
        String error = validateMyInfo(adminNm, email, mobileNo, deptNm);
        if (error == null) {
            try {
                meService.updateMyInfo(principal.adminId(), new MeService.MyInfo(adminNm.trim(), email.trim(),
                        mobileNo, deptNm, java.time.LocalDateTime.parse(modDt)), request.getRemoteAddr());
                redirect.addFlashAttribute("notice", "저장되었습니다");
                return "redirect:/ssr/me";
            } catch (BusinessException e) {
                error = e.getFieldErrors().isEmpty() ? e.getMessage() : e.getFieldErrors().get(0).message();
            }
        }
        model.addAttribute("formError", error);
        return "ssr/me";
    }

    /** API(MeApiController)의 검증 규칙과 같다 */
    static String validateMyInfo(String adminNm, String email, String mobileNo, String deptNm) {
        if (adminNm == null || adminNm.isBlank() || adminNm.length() > 50) {
            return "이름은 1~50자입니다.";
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 100) {
            return "이메일 형식을 확인하세요.";
        }
        if (mobileNo != null && !mobileNo.isBlank() && !mobileNo.matches("^[0-9]{10,11}$")) {
            return "휴대폰 번호는 숫자 10~11자리입니다.";
        }
        if (deptNm != null && deptNm.length() > 100) {
            return "부서는 100자 이내입니다.";
        }
        return null;
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
            model.addAttribute("fieldError", "새 비밀번호 확인이 일치하지 않습니다.");
            return "ssr/password";
        }
        try {
            meService.changePassword(principal.adminId(), currentPassword, newPassword,
                    request.getSession().getId(), request.getRemoteAddr());
        } catch (BusinessException e) {
            model.addAttribute("fieldError", e.getFieldErrors().isEmpty()
                    ? e.getMessage() : e.getFieldErrors().get(0).message());
            return "ssr/password";
        }
        redirect.addFlashAttribute("notice", "비밀번호가 변경되었습니다");
        return "redirect:/ssr/";
    }
}
