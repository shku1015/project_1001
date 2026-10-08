package egovframework.admin.web.api;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.permission.PermissionAdminService;
import egovframework.admin.permission.PermissionAdminService.EffectivePermissions;
import egovframework.admin.permission.PermissionAdminService.MenuGrants;
import egovframework.admin.permission.PermissionAdminService.MenuNode;
import egovframework.admin.permission.PermissionAdminService.RoleActions;
import egovframework.admin.permission.PermissionAdminService.RoleGrantChange;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 권한관리 API (docs/06-api/07-permission.md, api/openapi.yaml permission 태그). 메뉴 코드 PERMISSION.
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionApiController {

    private static final String PERMISSION = "PERMISSION";

    public record SaveRequest(@NotEmpty List<@Valid RoleItem> roles) {
    }

    public record RoleItem(@NotNull Long roleId, @NotNull List<Action> actions) {
    }

    private final PermissionAdminService permissionAdminService;

    public PermissionApiController(PermissionAdminService permissionAdminService) {
        this.permissionAdminService = permissionAdminService;
    }

    @GetMapping
    @RequirePermission(menu = PERMISSION, action = Action.READ)
    public ApiResponse<List<MenuNode>> tree() {
        return ApiResponse.ok(permissionAdminService.getTree());
    }

    @GetMapping("/menus/{menuId}")
    @RequirePermission(menu = PERMISSION, action = Action.READ)
    public ApiResponse<MenuGrants> menu(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId) {
        return ApiResponse.ok(permissionAdminService.getMenuGrants(principal.adminId(), menuId));
    }

    @PutMapping("/menus/{menuId}")
    @RequirePermission(menu = PERMISSION, action = Action.UPDATE)
    public ApiResponse<Map<String, List<RoleGrantChange>>> save(@AuthenticationPrincipal AdminPrincipal principal,
                                                                @PathVariable long menuId,
                                                                @Valid @RequestBody SaveRequest req,
                                                                HttpServletRequest request) {
        List<RoleGrantChange> changes = permissionAdminService.saveMenuGrants(principal.adminId(),
                request.getRemoteAddr(), menuId,
                req.roles().stream().map(r -> new RoleActions(r.roleId(), r.actions())).toList());
        return ApiResponse.ok(Map.of("changes", changes));
    }

    @GetMapping("/admins/{adminId}")
    @RequirePermission(menu = PERMISSION, action = Action.READ)
    public ApiResponse<EffectivePermissions> admin(@PathVariable long adminId) {
        return ApiResponse.ok(permissionAdminService.getEffectivePermissions(adminId));
    }
}
