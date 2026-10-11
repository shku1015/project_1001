package egovframework.admin.web.api;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.masking.MaskingAdminService;
import egovframework.admin.masking.MaskingAdminService.Policy;
import egovframework.admin.masking.MaskingAdminService.PolicyChange;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 마스킹 설정 API (docs/06-api/10-masking.md, api/openapi.yaml masking 태그). 메뉴 코드 MASKING.
 */
@RestController
@RequestMapping("/api/v1/masking-policies")
public class MaskingApiController {

    private static final String MASKING = "MASKING";

    public record SaveRequest(@NotEmpty List<@Valid PolicyItem> policies) {
    }

    public record PolicyItem(@NotNull String fieldCd, @NotNull @Pattern(regexp = "^[YN]$") String screenMaskYn,
                             @NotNull @Pattern(regexp = "^[YN]$") String excelMaskYn, @NotNull LocalDateTime modDt) {
    }

    private final MaskingAdminService maskingAdminService;

    public MaskingApiController(MaskingAdminService maskingAdminService) {
        this.maskingAdminService = maskingAdminService;
    }

    @GetMapping
    @RequirePermission(menu = MASKING, action = Action.READ)
    public ApiResponse<List<Policy>> list() {
        return ApiResponse.ok(maskingAdminService.getPolicies());
    }

    @PutMapping
    @RequirePermission(menu = MASKING, action = Action.UPDATE)
    public ApiResponse<Void> save(@AuthenticationPrincipal AdminPrincipal principal, @Valid @RequestBody SaveRequest req,
                                  HttpServletRequest request) {
        maskingAdminService.save(principal.adminId(), request.getRemoteAddr(), req.policies().stream()
                .map(p -> new PolicyChange(p.fieldCd(), p.screenMaskYn(), p.excelMaskYn(), p.modDt())).toList());
        return ApiResponse.ok(null);
    }
}
