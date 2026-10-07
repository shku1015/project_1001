package egovframework.admin.web.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.role.RoleAdminService;
import egovframework.admin.role.RoleAdminService.MenuActions;
import egovframework.admin.role.RoleAdminService.PermissionNode;
import egovframework.admin.role.RoleAdminService.RoleDetail;
import egovframework.admin.role.RoleAdminService.RoleListItem;
import egovframework.admin.role.RoleAdminService.SaveResult;
import egovframework.admin.role.RoleMapper.AdminRow;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 역할관리 API (docs/06-api/08-role.md, api/openapi.yaml role 태그). 메뉴 코드 ROLE.
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleApiController {

    private static final String ROLE = "ROLE";

    public record RoleCreateRequest(@NotNull @Pattern(regexp = "^[A-Z0-9_]+$") @Size(max = 50) String roleCd,
                                    @NotNull @Size(min = 1, max = 100) String roleNm,
                                    @Size(max = 500) String description,
                                    @NotNull @Pattern(regexp = "^[YN]$") String useYn) {
    }

    public record RoleUpdateRequest(@NotNull @Size(min = 1, max = 100) String roleNm,
                                    @Size(max = 500) String description,
                                    @NotNull @Pattern(regexp = "^[YN]$") String useYn,
                                    @NotNull LocalDateTime modDt) {
    }

    public record RoleCopyRequest(@NotNull @Pattern(regexp = "^[A-Z0-9_]+$") @Size(max = 50) String roleCd,
                                  @NotNull @Size(min = 1, max = 100) String roleNm) {
    }

    public record PermissionSaveRequest(@NotNull List<@Valid PermissionItem> permissions,
                                        @NotNull LocalDateTime modDt) {
    }

    public record PermissionItem(@NotNull Long menuId, @NotNull List<Action> actions) {
    }

    private final RoleAdminService roleAdminService;

    public RoleApiController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @GetMapping
    @RequirePermission(menu = ROLE, action = Action.READ)
    public ApiResponse<List<RoleListItem>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String useYn) {
        return ApiResponse.ok(roleAdminService.getRoles(keyword, useYn));
    }

    @GetMapping("/check-role-cd")
    @RequirePermission(menu = ROLE, action = Action.CREATE)
    public ApiResponse<Map<String, Boolean>> checkRoleCd(@RequestParam String roleCd) {
        return ApiResponse.ok(Map.of("available", roleAdminService.isRoleCdAvailable(roleCd)));
    }

    @GetMapping("/{roleId}")
    @RequirePermission(menu = ROLE, action = Action.READ)
    public ApiResponse<RoleDetail> role(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId) {
        return ApiResponse.ok(roleAdminService.getRole(principal.adminId(), roleId));
    }

    @GetMapping("/{roleId}/permissions")
    @RequirePermission(menu = ROLE, action = Action.READ)
    public ApiResponse<List<PermissionNode>> permissions(@AuthenticationPrincipal AdminPrincipal principal,
                                                         @PathVariable long roleId) {
        return ApiResponse.ok(roleAdminService.getPermissionTree(principal.adminId(), roleId));
    }

    @GetMapping("/{roleId}/admins")
    @RequirePermission(menu = ROLE, action = Action.READ)
    public ApiResponse<List<AdminRow>> admins(@PathVariable long roleId) {
        return ApiResponse.ok(roleAdminService.getAdmins(roleId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = ROLE, action = Action.CREATE)
    public ApiResponse<Map<String, Long>> create(@AuthenticationPrincipal AdminPrincipal principal,
                                                 @Valid @RequestBody RoleCreateRequest req, HttpServletRequest request) {
        long roleId = roleAdminService.create(principal.adminId(), request.getRemoteAddr(),
                new RoleAdminService.CreateCommand(req.roleCd(), req.roleNm(), req.description(), req.useYn()));
        return ApiResponse.ok(Map.of("roleId", roleId));
    }

    @PostMapping("/{roleId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = ROLE, action = Action.CREATE)
    public ApiResponse<Map<String, Long>> copy(@AuthenticationPrincipal AdminPrincipal principal,
                                               @PathVariable long roleId, @Valid @RequestBody RoleCopyRequest req,
                                               HttpServletRequest request) {
        long newId = roleAdminService.copy(principal.adminId(), request.getRemoteAddr(), roleId, req.roleCd(),
                req.roleNm());
        return ApiResponse.ok(Map.of("roleId", newId));
    }

    @PutMapping("/{roleId}")
    @RequirePermission(menu = ROLE, action = Action.UPDATE)
    public ApiResponse<Void> update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId,
                                    @Valid @RequestBody RoleUpdateRequest req, HttpServletRequest request) {
        roleAdminService.update(principal.adminId(), request.getRemoteAddr(), roleId,
                new RoleAdminService.UpdateCommand(req.roleNm(), req.description(), req.useYn(), req.modDt()));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{roleId}/permissions")
    @RequirePermission(menu = ROLE, action = Action.UPDATE)
    public ApiResponse<SaveResult> savePermissions(@AuthenticationPrincipal AdminPrincipal principal,
                                                   @PathVariable long roleId,
                                                   @Valid @RequestBody PermissionSaveRequest req,
                                                   HttpServletRequest request) {
        return ApiResponse.ok(roleAdminService.savePermissions(principal.adminId(), request.getRemoteAddr(), roleId,
                req.permissions().stream().map(p -> new MenuActions(p.menuId(), p.actions())).toList(), req.modDt()));
    }

    @DeleteMapping("/{roleId}")
    @RequirePermission(menu = ROLE, action = Action.DELETE)
    public ApiResponse<Void> delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long roleId,
                                    HttpServletRequest request) {
        roleAdminService.delete(principal.adminId(), request.getRemoteAddr(), roleId);
        return ApiResponse.ok(null);
    }
}
