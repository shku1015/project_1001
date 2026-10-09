package egovframework.admin.web.ssr;

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

import egovframework.admin.admin.AdminManageMapper;
import egovframework.admin.admin.AdminManageService;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.permission.PermissionAdminService;
import egovframework.admin.permission.PermissionAdminService.RoleActions;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 권한관리 (SCR-PRM-01). Service를 직접 호출한다 (ADR-0003).
 * 바뀐 역할만 보내야 하므로 JavaScript가 처음 값과 비교해 JSON으로 모아 보낸다.
 */
@Controller
public class SsrPermissionController {

    private static final String PERMISSION = "PERMISSION";

    private final PermissionAdminService permissionAdminService;
    private final AdminManageService adminManageService;
    private final AdminAuthInfoService authInfoService;
    private final ObjectMapper objectMapper;

    public SsrPermissionController(PermissionAdminService permissionAdminService,
                                   AdminManageService adminManageService, AdminAuthInfoService authInfoService,
                                   ObjectMapper objectMapper) {
        this.permissionAdminService = permissionAdminService;
        this.adminManageService = adminManageService;
        this.authInfoService = authInfoService;
        this.objectMapper = objectMapper;
    }

    /**
     * SCR-PRM-02 관리자별 최종 권한. 관리자 선택창(CMP-12)은 이름·아이디 검색 결과(10건)를 같은 화면에 보여 준다.
     */
    @GetMapping("/ssr/permissions/admins")
    @RequirePermission(menu = PERMISSION, action = Action.READ)
    public String adminPermissions(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Long adminId, Model model) {
        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("candidates", adminManageService.getAdmins(
                    new AdminManageMapper.Search(keyword.trim(), null, null, null, null, null), 1, 10, List.of()));
        }
        if (adminId != null) {
            try {
                model.addAttribute("effective", permissionAdminService.getEffectivePermissions(adminId));
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("allActions", Action.values());
        return "ssr/permission-admin";
    }

    @GetMapping("/ssr/permissions")
    @RequirePermission(menu = PERMISSION, action = Action.READ)
    public String permissions(@AuthenticationPrincipal AdminPrincipal principal,
                              @RequestParam(required = false) Long menu, Model model) {
        model.addAttribute("tree", permissionAdminService.getTree());
        model.addAttribute("canUpdate", authInfoService.load(principal.adminId()).has(PERMISSION, Action.UPDATE));
        if (menu != null) {
            try {
                model.addAttribute("grants", permissionAdminService.getMenuGrants(principal.adminId(), menu));
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "ssr/permission";
    }

    @PostMapping("/ssr/permissions/menus/{menuId}")
    @RequirePermission(menu = PERMISSION, action = Action.UPDATE)
    public String save(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId,
                       @RequestParam String roles, HttpServletRequest request, RedirectAttributes redirect) {
        try {
            List<RoleActions> list = objectMapper.readValue(roles, new TypeReference<List<RoleActions>>() { });
            permissionAdminService.saveMenuGrants(principal.adminId(), request.getRemoteAddr(), menuId, list);
            redirect.addFlashAttribute("notice", "권한이 저장되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getFieldErrors().isEmpty() ? e.getMessage()
                    : e.getFieldErrors().get(0).message());
        } catch (JsonProcessingException e) {
            redirect.addFlashAttribute("error", ErrorCode.INVALID_REQUEST.message());
        }
        return "redirect:/ssr/permissions?menu=" + menuId;
    }
}
