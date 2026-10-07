package egovframework.admin.web.ssr;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.role.RoleAdminService;
import egovframework.admin.role.RoleAdminService.MenuActions;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 역할관리 (SCR-ROL-01~03). Service를 직접 호출한다 (ADR-0003).
 * 권한 표는 비활성(내가 줄 수 없는) 칸도 현재 값을 유지해야 하므로, JavaScript가 표 전체를 JSON으로 모아 보낸다.
 */
@Controller
public class SsrRoleController {

    private static final String ROLE = "ROLE";

    private final RoleAdminService roleAdminService;
    private final AdminAuthInfoService authInfoService;
    private final ObjectMapper objectMapper;

    public SsrRoleController(RoleAdminService roleAdminService, AdminAuthInfoService authInfoService,
                             ObjectMapper objectMapper) {
        this.roleAdminService = roleAdminService;
        this.authInfoService = authInfoService;
        this.objectMapper = objectMapper;
    }

    // ================= 목록 (SCR-ROL-01) =================

    @GetMapping("/ssr/roles")
    @RequirePermission(menu = ROLE, action = Action.READ)
    public String list(@AuthenticationPrincipal AdminPrincipal principal,
                       @RequestParam(required = false) String keyword, @RequestParam(required = false) String useYn,
                       Model model) {
        model.addAttribute("roles", roleAdminService.getRoles(keyword, useYn));
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchUseYn", useYn);
        addPermissions(principal, model);
        return "ssr/role-list";
    }

    // ================= 상세 (SCR-ROL-02) =================

    @GetMapping("/ssr/roles/{roleId}")
    @RequirePermission(menu = ROLE, action = Action.READ)
    public String detail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId, Model model) {
        model.addAttribute("role", roleAdminService.getRole(principal.adminId(), roleId));
        model.addAttribute("permTree", roleAdminService.getPermissionTree(principal.adminId(), roleId));
        model.addAttribute("admins", roleAdminService.getAdmins(roleId));
        model.addAttribute("allActions", Action.values());
        addPermissions(principal, model);
        return "ssr/role-detail";
    }

    @PostMapping("/ssr/roles/{roleId}/permissions")
    @RequirePermission(menu = ROLE, action = Action.UPDATE)
    public String savePermissions(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId,
                                  @RequestParam String permissions, @RequestParam String modDt,
                                  HttpServletRequest request, RedirectAttributes redirect) {
        try {
            List<MenuActions> list = objectMapper.readValue(permissions, new TypeReference<List<MenuActions>>() { });
            roleAdminService.savePermissions(principal.adminId(), request.getRemoteAddr(), roleId, list,
                    LocalDateTime.parse(modDt));
            redirect.addFlashAttribute("notice", "권한이 저장되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        } catch (JsonProcessingException e) {
            redirect.addFlashAttribute("error", ErrorCode.INVALID_REQUEST.message());
        }
        return "redirect:/ssr/roles/" + roleId;
    }

    @PostMapping("/ssr/roles/{roleId}/copy")
    @RequirePermission(menu = ROLE, action = Action.CREATE)
    public String copy(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId,
                       @RequestParam String roleCd, @RequestParam String roleNm, HttpServletRequest request,
                       RedirectAttributes redirect) {
        try {
            long newId = roleAdminService.copy(principal.adminId(), request.getRemoteAddr(), roleId, roleCd.trim(),
                    roleNm);
            redirect.addFlashAttribute("notice", "복사되었습니다");
            return "redirect:/ssr/roles/" + newId;
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
            return "redirect:/ssr/roles/" + roleId;
        }
    }

    @PostMapping("/ssr/roles/{roleId}/delete")
    @RequirePermission(menu = ROLE, action = Action.DELETE)
    public String delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId,
                         HttpServletRequest request, RedirectAttributes redirect) {
        try {
            roleAdminService.delete(principal.adminId(), request.getRemoteAddr(), roleId);
            redirect.addFlashAttribute("notice", "삭제되었습니다");
            return "redirect:/ssr/roles";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
            return "redirect:/ssr/roles/" + roleId;
        }
    }

    // ================= 등록·수정 (SCR-ROL-03) =================

    @GetMapping("/ssr/roles/new")
    @RequirePermission(menu = ROLE, action = Action.CREATE)
    public String createForm() {
        return "ssr/role-form";
    }

    @PostMapping("/ssr/roles")
    @RequirePermission(menu = ROLE, action = Action.CREATE)
    public String create(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam String roleCd,
                         @RequestParam String roleNm, @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "Y") String useYn, HttpServletRequest request,
                         RedirectAttributes redirect, Model model) {
        try {
            long roleId = roleAdminService.create(principal.adminId(), request.getRemoteAddr(),
                    new RoleAdminService.CreateCommand(roleCd.trim(), roleNm, description, useYn));
            redirect.addFlashAttribute("notice", "등록되었습니다");
            return "redirect:/ssr/roles/" + roleId;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            return "ssr/role-form";
        }
    }

    @GetMapping("/ssr/roles/{roleId}/edit")
    @RequirePermission(menu = ROLE, action = Action.UPDATE)
    public String editForm(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId, Model model) {
        model.addAttribute("role", roleAdminService.getRole(principal.adminId(), roleId));
        return "ssr/role-form";
    }

    @PostMapping("/ssr/roles/{roleId}/update")
    @RequirePermission(menu = ROLE, action = Action.UPDATE)
    public String update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId,
                         @RequestParam String roleNm, @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "Y") String useYn, @RequestParam String modDt,
                         HttpServletRequest request, RedirectAttributes redirect, Model model) {
        try {
            roleAdminService.update(principal.adminId(), request.getRemoteAddr(), roleId,
                    new RoleAdminService.UpdateCommand(roleNm, description, useYn, LocalDateTime.parse(modDt)));
            redirect.addFlashAttribute("notice", "수정되었습니다");
            return "redirect:/ssr/roles/" + roleId;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            model.addAttribute("role", roleAdminService.getRole(principal.adminId(), roleId));
            return "ssr/role-form";
        }
    }

    // ================= 공통 =================

    /** 권한이 없는 버튼은 비활성으로 보여 준다 (docs/05-ia-screens.md 4.2) */
    private void addPermissions(AdminPrincipal principal, Model model) {
        AdminAuthInfo auth = authInfoService.load(principal.adminId());
        model.addAttribute("canCreate", auth.has(ROLE, Action.CREATE));
        model.addAttribute("canUpdate", auth.has(ROLE, Action.UPDATE));
        model.addAttribute("canDelete", auth.has(ROLE, Action.DELETE));
    }

    private static String message(BusinessException e) {
        return e.getFieldErrors().isEmpty() ? e.getMessage() : e.getFieldErrors().get(0).message();
    }
}
