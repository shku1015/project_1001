package egovframework.admin.web.ssr;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.admin.admin.AdminManageMapper;
import egovframework.admin.admin.AdminManageService;
import egovframework.admin.admin.AdminManageService.AdminDetail;
import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 관리자관리 (SCR-ADM-01~03). Service를 직접 호출한다 (ADR-0003).
 * 임시 비밀번호는 Flash 속성으로 상세 화면에 한 번만 넘긴다 (BR-02).
 */
@Controller
public class SsrAdminController {

    private static final String ADMIN = "ADMIN";

    private final AdminManageService adminManageService;
    private final AdminAuthInfoService authInfoService;

    public SsrAdminController(AdminManageService adminManageService, AdminAuthInfoService authInfoService) {
        this.adminManageService = adminManageService;
        this.authInfoService = authInfoService;
    }

    /** 일시 표시 형식 (docs/05-ia-screens.md 4.1: 목록은 분, 상세는 초까지) */
    @ModelAttribute
    public void formats(Model model) {
        model.addAttribute("dtMin", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        model.addAttribute("dtSec", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ================= 목록 (SCR-ADM-01) =================

    @GetMapping("/ssr/admins")
    @RequirePermission(menu = ADMIN, action = Action.READ)
    public String list(@AuthenticationPrincipal AdminPrincipal principal,
                       @RequestParam(required = false) String loginId, @RequestParam(required = false) String adminNm,
                       @RequestParam(required = false) String deptNm, @RequestParam(required = false) Long roleId,
                       @RequestParam(required = false) String statusCd, @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size, HttpServletRequest request, Model model) {
        String[] sort = request.getParameterValues("sort");
        try {
            model.addAttribute("result", adminManageService.getAdmins(
                    new AdminManageMapper.Search(null, loginId, adminNm, deptNm, roleId, statusCd), page, size,
                    sort == null ? List.of() : List.of(sort)));
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("result", adminManageService.getAdmins(
                    new AdminManageMapper.Search(null, null, null, null, null, null), 1, 20, List.of()));
        }
        model.addAttribute("sort", sort == null ? "" : sort[0]);
        model.addAttribute("roleOptions", adminManageService.getRoleOptions(principal.adminId()));
        addPermissions(principal, model);
        return "ssr/admin-list";
    }

    // ================= 상세 (SCR-ADM-02) =================

    @GetMapping("/ssr/admins/{adminId}")
    @RequirePermission(menu = ADMIN, action = Action.READ)
    public String detail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId, Model model) {
        AdminDetail admin = adminManageService.getAdmin(principal.adminId(), adminId);
        model.addAttribute("admin", admin);
        model.addAttribute("mobileText", mobile(admin.mobileNo()));
        model.addAttribute("histories", adminManageService.getLoginHistories(adminId));
        List<Long> granted = admin.roles().stream().map(AdminManageService.GrantedRole::roleId).toList();
        model.addAttribute("addableRoles", adminManageService.getRoleOptions(principal.adminId()).stream()
                .filter(r -> r.assignable() && !granted.contains(r.roleId())).toList());
        addPermissions(principal, model);
        return "ssr/admin-detail";
    }

    @PostMapping("/ssr/admins/{adminId}/roles")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public String grantRole(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                            @RequestParam long roleId, HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, adminId, "역할이 부여되었습니다",
                () -> adminManageService.grantRole(principal.adminId(), request.getRemoteAddr(), adminId, roleId));
    }

    @PostMapping("/ssr/admins/{adminId}/roles/{roleId}/delete")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public String revokeRole(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                             @PathVariable long roleId, HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, adminId, "역할이 회수되었습니다",
                () -> adminManageService.revokeRole(principal.adminId(), request.getRemoteAddr(), adminId, roleId));
    }

    @PostMapping("/ssr/admins/{adminId}/unlock")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public String unlock(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                         HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, adminId, "잠금이 해제되었습니다",
                () -> adminManageService.unlock(principal.adminId(), request.getRemoteAddr(), adminId));
    }

    @PostMapping("/ssr/admins/{adminId}/password-reset")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public String resetPassword(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                                HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, adminId, "비밀번호가 초기화되었습니다", () -> redirect.addFlashAttribute("tempPassword",
                adminManageService.resetPassword(principal.adminId(), request.getRemoteAddr(), adminId)));
    }

    @PostMapping("/ssr/admins/{adminId}/status")
    @RequirePermission(menu = ADMIN, action = Action.DELETE)
    public String changeStatus(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                               @RequestParam String statusCd, @RequestParam String modDt,
                               HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, adminId, "DISABLED".equals(statusCd) ? "사용중지되었습니다" : "재사용 처리되었습니다",
                () -> adminManageService.changeStatus(principal.adminId(), request.getRemoteAddr(), adminId, statusCd,
                        LocalDateTime.parse(modDt)));
    }

    // ================= 등록·수정 (SCR-ADM-03) =================

    @GetMapping("/ssr/admins/new")
    @RequirePermission(menu = ADMIN, action = Action.CREATE)
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        addRoleChoices(principal, model);
        return "ssr/admin-form";
    }

    @PostMapping("/ssr/admins")
    @RequirePermission(menu = ADMIN, action = Action.CREATE)
    public String create(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam String loginId,
                         @RequestParam String adminNm, @RequestParam String email,
                         @RequestParam(required = false) String mobileNo, @RequestParam(required = false) String deptNm,
                         @RequestParam(required = false) List<Long> roleIds, HttpServletRequest request,
                         RedirectAttributes redirect, Model model) {
        try {
            AdminManageService.Created created = adminManageService.create(principal.adminId(),
                    request.getRemoteAddr(), new AdminManageService.CreateCommand(loginId.trim(), adminNm, email,
                            mobileNo, deptNm, roleIds == null ? List.of() : roleIds));
            redirect.addFlashAttribute("notice", "등록되었습니다");
            redirect.addFlashAttribute("tempPassword", created.tempPassword());
            return "redirect:/ssr/admins/" + created.adminId();
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            addRoleChoices(principal, model);
            return "ssr/admin-form";
        }
    }

    @GetMapping("/ssr/admins/{adminId}/edit")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public String editForm(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId, Model model) {
        model.addAttribute("admin", adminManageService.getAdmin(principal.adminId(), adminId));
        return "ssr/admin-form";
    }

    @PostMapping("/ssr/admins/{adminId}/update")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public String update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                         @RequestParam String adminNm, @RequestParam String email,
                         @RequestParam(required = false) String mobileNo, @RequestParam(required = false) String deptNm,
                         @RequestParam String modDt, HttpServletRequest request, RedirectAttributes redirect,
                         Model model) {
        try {
            adminManageService.update(principal.adminId(), request.getRemoteAddr(), adminId,
                    new AdminManageService.UpdateCommand(adminNm, email, mobileNo, deptNm, LocalDateTime.parse(modDt)));
            redirect.addFlashAttribute("notice", "수정되었습니다");
            return "redirect:/ssr/admins/" + adminId;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            model.addAttribute("admin", adminManageService.getAdmin(principal.adminId(), adminId));
            return "ssr/admin-form";
        }
    }

    // ================= 공통 =================

    private String run(RedirectAttributes redirect, long adminId, String notice, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", notice);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/ssr/admins/" + adminId;
    }

    private void addRoleChoices(AdminPrincipal principal, Model model) {
        model.addAttribute("roleChoices", adminManageService.getRoleOptions(principal.adminId()).stream()
                .filter(AdminManageService.RoleOption::assignable).toList());
    }

    /** 권한이 없는 버튼은 비활성으로 보여 준다 (docs/05-ia-screens.md 4.2) */
    private void addPermissions(AdminPrincipal principal, Model model) {
        AdminAuthInfo auth = authInfoService.load(principal.adminId());
        model.addAttribute("canCreate", auth.has(ADMIN, Action.CREATE));
        model.addAttribute("canUpdate", auth.has(ADMIN, Action.UPDATE));
        model.addAttribute("canDelete", auth.has(ADMIN, Action.DELETE));
    }

    /** 01012345678 → 010-1234-5678 (docs/05-ia-screens.md 4.1) */
    static String mobile(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.length() == 11 ? value.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3")
                : value.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
    }

    private static String message(BusinessException e) {
        return e.getFieldErrors().isEmpty() ? e.getMessage() : e.getFieldErrors().get(0).message();
    }
}
