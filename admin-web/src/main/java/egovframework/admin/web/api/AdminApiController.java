package egovframework.admin.web.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.admin.AdminManageMapper;
import egovframework.admin.admin.AdminManageMapper.LoginHistRow;
import egovframework.admin.admin.AdminManageService;
import egovframework.admin.admin.AdminManageService.AdminDetail;
import egovframework.admin.admin.AdminManageService.AdminListItem;
import egovframework.admin.admin.AdminManageService.Created;
import egovframework.admin.admin.AdminManageService.RoleOption;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.common.PageResult;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자관리 API (docs/06-api/06-admin.md, api/openapi.yaml admin 태그). 메뉴 코드 ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admins")
public class AdminApiController {

    private static final String ADMIN = "ADMIN";

    public record AdminCreateRequest(@NotNull @Pattern(regexp = "^[a-z0-9]{4,50}$") String loginId,
                                     @NotNull @Size(min = 1, max = 50) String adminNm,
                                     @NotNull @Size(max = 100) String email,
                                     @Pattern(regexp = "^[0-9]{10,11}$") String mobileNo,
                                     @Size(max = 100) String deptNm,
                                     @NotEmpty List<Long> roleIds) {
    }

    public record AdminUpdateRequest(@NotNull @Size(min = 1, max = 50) String adminNm,
                                     @NotNull @Size(max = 100) String email,
                                     @Pattern(regexp = "^[0-9]{10,11}$") String mobileNo,
                                     @Size(max = 100) String deptNm,
                                     @NotNull LocalDateTime modDt) {
    }

    public record RoleGrantRequest(@NotNull Long roleId) {
    }

    public record StatusRequest(@NotNull @Pattern(regexp = "^(ACTIVE|DISABLED)$") String statusCd,
                                @NotNull LocalDateTime modDt) {
    }

    private final AdminManageService adminManageService;

    public AdminApiController(AdminManageService adminManageService) {
        this.adminManageService = adminManageService;
    }

    @GetMapping
    @RequirePermission(menu = ADMIN, action = Action.READ)
    public ApiResponse<PageResult<AdminListItem>> list(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String loginId,
                                                       @RequestParam(required = false) String adminNm,
                                                       @RequestParam(required = false) String deptNm,
                                                       @RequestParam(required = false) Long roleId,
                                                       @RequestParam(required = false) String statusCd,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       HttpServletRequest request) {
        // sort는 "필드,방향"이라 List<String>으로 받으면 쉼표에서 나뉜다. 원래 값 그대로 읽는다
        String[] sort = request.getParameterValues("sort");
        return ApiResponse.ok(adminManageService.getAdmins(
                new AdminManageMapper.Search(keyword, loginId, adminNm, deptNm, roleId, statusCd), page, size,
                sort == null ? List.of() : List.of(sort)));
    }

    @GetMapping("/check-login-id")
    @RequirePermission(menu = ADMIN, action = Action.CREATE)
    public ApiResponse<Map<String, Boolean>> checkLoginId(@RequestParam String loginId) {
        return ApiResponse.ok(Map.of("available", adminManageService.isLoginIdAvailable(loginId)));
    }

    @GetMapping("/role-options")
    @RequirePermission(menu = ADMIN, action = Action.READ)
    public ApiResponse<List<RoleOption>> roleOptions(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(adminManageService.getRoleOptions(principal.adminId()));
    }

    @GetMapping("/{adminId}")
    @RequirePermission(menu = ADMIN, action = Action.READ)
    public ApiResponse<AdminDetail> admin(@AuthenticationPrincipal AdminPrincipal principal,
                                          @PathVariable long adminId) {
        return ApiResponse.ok(adminManageService.getAdmin(principal.adminId(), adminId));
    }

    @GetMapping("/{adminId}/login-histories")
    @RequirePermission(menu = ADMIN, action = Action.READ)
    public ApiResponse<List<LoginHistRow>> loginHistories(@PathVariable long adminId) {
        return ApiResponse.ok(adminManageService.getLoginHistories(adminId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = ADMIN, action = Action.CREATE)
    public ApiResponse<Created> create(@AuthenticationPrincipal AdminPrincipal principal,
                                       @Valid @RequestBody AdminCreateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(adminManageService.create(principal.adminId(), request.getRemoteAddr(),
                new AdminManageService.CreateCommand(req.loginId(), req.adminNm(), req.email(), req.mobileNo(),
                        req.deptNm(), req.roleIds())));
    }

    @PutMapping("/{adminId}")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public ApiResponse<Void> update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                                    @Valid @RequestBody AdminUpdateRequest req, HttpServletRequest request) {
        adminManageService.update(principal.adminId(), request.getRemoteAddr(), adminId,
                new AdminManageService.UpdateCommand(req.adminNm(), req.email(), req.mobileNo(), req.deptNm(),
                        req.modDt()));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{adminId}/roles")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public ApiResponse<Void> grantRole(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                                       @Valid @RequestBody RoleGrantRequest req, HttpServletRequest request) {
        adminManageService.grantRole(principal.adminId(), request.getRemoteAddr(), adminId, req.roleId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{adminId}/roles/{roleId}")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public ApiResponse<Void> revokeRole(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                                        @PathVariable long roleId, HttpServletRequest request) {
        adminManageService.revokeRole(principal.adminId(), request.getRemoteAddr(), adminId, roleId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{adminId}/unlock")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public ApiResponse<Void> unlock(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long adminId,
                                    HttpServletRequest request) {
        adminManageService.unlock(principal.adminId(), request.getRemoteAddr(), adminId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{adminId}/password-reset")
    @RequirePermission(menu = ADMIN, action = Action.UPDATE)
    public ApiResponse<Map<String, String>> resetPassword(@AuthenticationPrincipal AdminPrincipal principal,
                                                          @PathVariable long adminId, HttpServletRequest request) {
        return ApiResponse.ok(Map.of("tempPassword",
                adminManageService.resetPassword(principal.adminId(), request.getRemoteAddr(), adminId)));
    }

    @PatchMapping("/{adminId}/status")
    @RequirePermission(menu = ADMIN, action = Action.DELETE)
    public ApiResponse<Void> changeStatus(@AuthenticationPrincipal AdminPrincipal principal,
                                          @PathVariable long adminId, @Valid @RequestBody StatusRequest req,
                                          HttpServletRequest request) {
        adminManageService.changeStatus(principal.adminId(), request.getRemoteAddr(), adminId, req.statusCd(),
                req.modDt());
        return ApiResponse.ok(null);
    }
}
