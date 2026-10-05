package egovframework.admin.web.api;

import java.time.LocalDateTime;
import java.util.List;

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
import egovframework.admin.code.CodeAdminService;
import egovframework.admin.code.CodeDetail;
import egovframework.admin.code.CodeGroup;
import egovframework.admin.code.CodeGroupSummary;
import egovframework.admin.code.CodeMapper;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 코드관리 API (docs/06-api/04-code.md, api/openapi.yaml code 태그). 메뉴 코드 CODE.
 */
@RestController
@RequestMapping("/api/v1/code-groups")
public class CodeApiController {

    private static final String PATTERN = "^[A-Z0-9_]+$";

    public record CodeGroupRequest(@NotNull @Pattern(regexp = PATTERN) @Size(max = 50) String groupCd,
                                   @NotNull @Size(min = 1, max = 100) String groupNm,
                                   @Size(max = 500) String description,
                                   @NotNull @Pattern(regexp = "^[YN]$") String useYn) {
    }

    public record CodeGroupUpdateRequest(@NotNull @Size(min = 1, max = 100) String groupNm,
                                         @Size(max = 500) String description,
                                         @NotNull @Pattern(regexp = "^[YN]$") String useYn,
                                         @NotNull LocalDateTime modDt) {
    }

    public record CodeDetailRequest(@NotNull @Pattern(regexp = PATTERN) @Size(max = 50) String code,
                                    @NotNull @Size(min = 1, max = 100) String codeNm,
                                    @NotNull Integer sortOrd,
                                    @Size(max = 500) String description,
                                    @NotNull @Pattern(regexp = "^[YN]$") String useYn) {
    }

    public record CodeDetailUpdateRequest(@NotNull @Size(min = 1, max = 100) String codeNm,
                                          @NotNull Integer sortOrd,
                                          @Size(max = 500) String description,
                                          @NotNull @Pattern(regexp = "^[YN]$") String useYn,
                                          @NotNull LocalDateTime modDt) {
    }

    private final CodeAdminService codeAdminService;

    public CodeApiController(CodeAdminService codeAdminService) {
        this.codeAdminService = codeAdminService;
    }

    // ===== 그룹코드 =====

    @GetMapping
    @RequirePermission(menu = "CODE", action = Action.READ)
    public ApiResponse<List<CodeGroupSummary>> groups(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String useYn) {
        return ApiResponse.ok(codeAdminService.getGroups(keyword, useYn));
    }

    @GetMapping("/{groupCd}")
    @RequirePermission(menu = "CODE", action = Action.READ)
    public ApiResponse<CodeGroup> group(@PathVariable String groupCd) {
        return ApiResponse.ok(codeAdminService.getGroup(groupCd));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = "CODE", action = Action.CREATE)
    public ApiResponse<Void> createGroup(@AuthenticationPrincipal AdminPrincipal principal,
                                         @Valid @RequestBody CodeGroupRequest req, HttpServletRequest request) {
        codeAdminService.createGroup(principal.adminId(), request.getRemoteAddr(),
                new CodeMapper.CodeGroupRequest(req.groupCd(), req.groupNm(), req.description(), req.useYn()));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{groupCd}")
    @RequirePermission(menu = "CODE", action = Action.UPDATE)
    public ApiResponse<Void> updateGroup(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable String groupCd,
                                         @Valid @RequestBody CodeGroupUpdateRequest req, HttpServletRequest request) {
        codeAdminService.updateGroup(principal.adminId(), request.getRemoteAddr(), groupCd, req.groupNm(),
                req.description(), req.useYn(), req.modDt());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{groupCd}")
    @RequirePermission(menu = "CODE", action = Action.DELETE)
    public ApiResponse<Void> deleteGroup(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable String groupCd,
                                         HttpServletRequest request) {
        codeAdminService.deleteGroup(principal.adminId(), request.getRemoteAddr(), groupCd);
        return ApiResponse.ok(null);
    }

    // ===== 상세코드 =====

    @GetMapping("/{groupCd}/codes")
    @RequirePermission(menu = "CODE", action = Action.READ)
    public ApiResponse<List<CodeDetail>> details(@PathVariable String groupCd) {
        return ApiResponse.ok(codeAdminService.getDetails(groupCd));
    }

    @PostMapping("/{groupCd}/codes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = "CODE", action = Action.CREATE)
    public ApiResponse<Void> createDetail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable String groupCd,
                                          @Valid @RequestBody CodeDetailRequest req, HttpServletRequest request) {
        codeAdminService.createDetail(principal.adminId(), request.getRemoteAddr(), groupCd,
                new CodeMapper.CodeDetailRequest(req.code(), req.codeNm(), req.sortOrd(), req.description(), req.useYn()));
        return ApiResponse.ok(null);
    }

    @PutMapping("/{groupCd}/codes/{code}")
    @RequirePermission(menu = "CODE", action = Action.UPDATE)
    public ApiResponse<Void> updateDetail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable String groupCd,
                                          @PathVariable String code, @Valid @RequestBody CodeDetailUpdateRequest req,
                                          HttpServletRequest request) {
        codeAdminService.updateDetail(principal.adminId(), request.getRemoteAddr(), groupCd, code, req.codeNm(),
                req.sortOrd(), req.description(), req.useYn(), req.modDt());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{groupCd}/codes/{code}")
    @RequirePermission(menu = "CODE", action = Action.DELETE)
    public ApiResponse<Void> deleteDetail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable String groupCd,
                                          @PathVariable String code, HttpServletRequest request) {
        codeAdminService.deleteDetail(principal.adminId(), request.getRemoteAddr(), groupCd, code);
        return ApiResponse.ok(null);
    }
}
