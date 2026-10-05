package egovframework.admin.web.api;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.auth.MeService;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.AllowTempPassword;
import egovframework.admin.web.security.ClientInfos;
import egovframework.admin.web.security.LoginOnly;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 내 정보 수정·비밀번호 변경 API (SCR-MY-01, SCR-AUTH-02).
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeApiController {

    public record UpdateMyInfoRequest(@NotBlank @Size(max = 50) String adminNm,
                                      @NotBlank @Email @Size(max = 100) String email,
                                      @Pattern(regexp = "^[0-9]{10,11}$", message = "숫자 10~11자리") String mobileNo,
                                      @Size(max = 100) String deptNm,
                                      @NotNull LocalDateTime modDt) {
    }

    public record ChangePasswordRequest(@NotEmpty String currentPassword,
                                        @NotEmpty @Size(max = 20, message = "비밀번호는 1~20자입니다.") String newPassword) {
    }

    private final MeService meService;
    private final TokenIssuer tokenIssuer;

    public MeApiController(MeService meService, TokenIssuer tokenIssuer) {
        this.meService = meService;
        this.tokenIssuer = tokenIssuer;
    }

    @PutMapping
    @LoginOnly
    public ApiResponse<Void> updateMyInfo(@AuthenticationPrincipal AdminPrincipal principal,
                                          @Valid @RequestBody UpdateMyInfoRequest req, HttpServletRequest request) {
        meService.updateMyInfo(principal.adminId(),
                new MeService.MyInfo(req.adminNm(), req.email(), req.mobileNo(), req.deptNm(), req.modDt()),
                request.getRemoteAddr());
        return ApiResponse.ok(null);
    }

    /** 다른 로그인은 모두 끊고, 이 로그인은 새 토큰으로 이어간다 */
    @PutMapping("/password")
    @LoginOnly
    @AllowTempPassword
    public ApiResponse<TokenIssuer.Token> changePassword(@AuthenticationPrincipal AdminPrincipal principal,
                                                               @Valid @RequestBody ChangePasswordRequest req,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) {
        meService.changePassword(principal.adminId(), req.currentPassword(), req.newPassword(), null,
                request.getRemoteAddr());
        return ApiResponse.ok(tokenIssuer.issue(principal, false, ClientInfos.of(request), response));
    }
}
