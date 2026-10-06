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

import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.menu.MenuAdminService;
import egovframework.admin.menu.MenuAdminService.MenuAdminNode;
import egovframework.admin.menu.MenuAdminService.MenuDetail;
import egovframework.admin.menu.MenuAdminService.OrderEntry;
import egovframework.admin.menu.MenuAdminService.RevokedRole;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 메뉴관리 API (docs/06-api/03-menu.md, api/openapi.yaml menu 태그). 메뉴 코드 MENU.
 */
@RestController
@RequestMapping("/api/v1/menus")
public class MenuApiController {

    public record MenuCreateRequest(Long parentMenuId,
                                    @NotNull @Pattern(regexp = "^[A-Z0-9_]+$") @Size(max = 50) String menuCd,
                                    @NotNull @Size(min = 1, max = 100) String menuNm,
                                    @NotNull @Pattern(regexp = "^(FOLDER|PAGE)$") String menuTypeCd,
                                    @Size(max = 200) String menuUrl,
                                    @NotNull List<Action> actions,
                                    @Size(max = 50) String icon,
                                    @NotNull @Pattern(regexp = "^[YN]$") String useYn) {
    }

    public record MenuUpdateRequest(@NotNull @Size(min = 1, max = 100) String menuNm,
                                    @Size(max = 200) String menuUrl,
                                    @NotNull List<Action> actions,
                                    @Size(max = 50) String icon,
                                    @NotNull @Pattern(regexp = "^[YN]$") String useYn,
                                    @NotNull LocalDateTime modDt) {
    }

    public record MenuMoveRequest(Long parentMenuId, @NotNull LocalDateTime modDt) {
    }

    public record MenuOrderRequest(@NotEmpty List<@Valid OrderItem> orders) {
    }

    public record OrderItem(Long parentMenuId, @NotEmpty List<Long> menuIds) {
    }

    private final MenuAdminService menuAdminService;

    public MenuApiController(MenuAdminService menuAdminService) {
        this.menuAdminService = menuAdminService;
    }

    @GetMapping("/tree")
    @RequirePermission(menu = "MENU", action = Action.READ)
    public ApiResponse<List<MenuAdminNode>> tree() {
        return ApiResponse.ok(menuAdminService.getTree());
    }

    @GetMapping("/check-menu-cd")
    @RequirePermission(menu = "MENU", action = Action.READ)
    public ApiResponse<Map<String, Boolean>> checkMenuCd(@RequestParam String menuCd) {
        return ApiResponse.ok(Map.of("available", menuAdminService.isMenuCdAvailable(menuCd)));
    }

    @GetMapping("/{menuId}")
    @RequirePermission(menu = "MENU", action = Action.READ)
    public ApiResponse<MenuDetail> menu(@PathVariable long menuId) {
        return ApiResponse.ok(menuAdminService.getMenu(menuId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = "MENU", action = Action.CREATE)
    public ApiResponse<Map<String, Long>> create(@AuthenticationPrincipal AdminPrincipal principal,
                                                 @Valid @RequestBody MenuCreateRequest req, HttpServletRequest request) {
        long menuId = menuAdminService.create(principal.adminId(), request.getRemoteAddr(),
                new MenuAdminService.CreateCommand(req.parentMenuId(), req.menuCd(), req.menuNm(), req.menuTypeCd(),
                        req.menuUrl(), req.actions(), req.icon(), req.useYn()));
        return ApiResponse.ok(Map.of("menuId", menuId));
    }

    /** dryRun=Y면 저장하지 않고 회수될 역할만 돌려준다 */
    @PutMapping("/{menuId}")
    @RequirePermission(menu = "MENU", action = Action.UPDATE)
    public ApiResponse<Map<String, List<RevokedRole>>> update(@AuthenticationPrincipal AdminPrincipal principal,
                                                              @PathVariable long menuId,
                                                              @RequestParam(defaultValue = "N") String dryRun,
                                                              @Valid @RequestBody MenuUpdateRequest req,
                                                              HttpServletRequest request) {
        List<RevokedRole> revoked = menuAdminService.update(principal.adminId(), request.getRemoteAddr(), menuId,
                new MenuAdminService.UpdateCommand(req.menuNm(), req.menuUrl(), req.actions(), req.icon(), req.useYn(),
                        req.modDt()), "Y".equals(dryRun));
        return ApiResponse.ok(Map.of("revokedRoles", revoked));
    }

    @PatchMapping("/{menuId}/parent")
    @RequirePermission(menu = "MENU", action = Action.UPDATE)
    public ApiResponse<Void> move(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId,
                                  @Valid @RequestBody MenuMoveRequest req, HttpServletRequest request) {
        menuAdminService.move(principal.adminId(), request.getRemoteAddr(), menuId, req.parentMenuId(), req.modDt());
        return ApiResponse.ok(null);
    }

    @PutMapping("/order")
    @RequirePermission(menu = "MENU", action = Action.UPDATE)
    public ApiResponse<Void> saveOrder(@Valid @RequestBody MenuOrderRequest req) {
        menuAdminService.saveOrder(req.orders().stream()
                .map(o -> new OrderEntry(o.parentMenuId(), o.menuIds())).toList());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{menuId}")
    @RequirePermission(menu = "MENU", action = Action.DELETE)
    public ApiResponse<Void> delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId,
                                    HttpServletRequest request) {
        menuAdminService.delete(principal.adminId(), request.getRemoteAddr(), menuId);
        return ApiResponse.ok(null);
    }
}
