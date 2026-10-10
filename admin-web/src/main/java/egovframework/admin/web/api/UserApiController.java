package egovframework.admin.web.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.common.PageResult;
import egovframework.admin.user.UserAdminMapper;
import egovframework.admin.user.UserAdminMapper.CompanyOption;
import egovframework.admin.user.UserAdminMapper.HistoryRow;
import egovframework.admin.user.UserAdminMapper.UserValues;
import egovframework.admin.user.UserAdminService;
import egovframework.admin.user.UserAdminService.Created;
import egovframework.admin.user.UserAdminService.UserDetail;
import egovframework.admin.user.UserAdminService.UserForm;
import egovframework.admin.user.UserAdminService.UserListItem;
import egovframework.admin.user.UserAdminService.UserPrivacy;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import egovframework.admin.web.support.ExcelResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자관리 API (docs/06-api/02-user.md, api/openapi.yaml user 태그). 메뉴 코드 USER.
 * 등록·수정·수정 화면 조회는 개인정보 원문을 다루므로 PRIVACY 권한도 함께 필요하다 (BR-05).
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserApiController {

    private static final String USER = "USER";

    public record UserRequest(@Pattern(regexp = "^(PERSONAL|CORPORATE)$") String userTypeCd,
                              @Pattern(regexp = "^[a-z0-9]{4,50}$") String loginId,
                              @NotNull @Size(min = 1, max = 50) String userNm,
                              @NotNull @Size(max = 100) String email,
                              @Pattern(regexp = "^[0-9]{10,11}$") String mobileNo,
                              LocalDate birthDate, Long companyId,
                              @Size(max = 100) String deptNm, @Size(max = 50) String positionNm,
                              LocalDateTime modDt) {

        UserValues values() {
            return new UserValues(userNm, email, mobileNo, birthDate, companyId, deptNm, positionNm);
        }
    }

    public record PrivacyRequest(@NotNull @Size(max = 50) String reasonCd, @Size(max = 200) String reasonEtc) {
    }

    public record StatusRequest(@NotNull @Pattern(regexp = "^(ACTIVE|SUSPENDED|WITHDRAWN)$") String statusCd,
                                @NotNull @Size(min = 1, max = 500) String reason, @NotNull LocalDateTime modDt) {
    }

    public record DeleteRequest(@NotNull @Size(min = 1, max = 500) String reason) {
    }

    private final UserAdminService userAdminService;
    private final AdminAuthInfoService authInfoService;

    public UserApiController(UserAdminService userAdminService, AdminAuthInfoService authInfoService) {
        this.userAdminService = userAdminService;
        this.authInfoService = authInfoService;
    }

    @GetMapping
    @RequirePermission(menu = USER, action = Action.READ)
    public ApiResponse<PageResult<UserListItem>> list(@RequestParam(required = false) String userTypeCd,
                                                      @RequestParam(required = false) String loginId,
                                                      @RequestParam(required = false) String userNm,
                                                      @RequestParam(required = false) String email,
                                                      @RequestParam(required = false) String mobileNo,
                                                      @RequestParam(required = false) Long companyId,
                                                      @RequestParam(required = false) String companyNm,
                                                      @RequestParam(required = false) String statusCd,
                                                      @RequestParam(required = false) String joinPath,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDtFrom,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDtTo,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size,
                                                      HttpServletRequest request) {
        return ApiResponse.ok(userAdminService.getUsers(new UserAdminMapper.Search(userTypeCd, loginId, userNm, email,
                mobileNo, companyId, companyNm, statusCd, joinPath, joinDtFrom, joinDtTo), page, size, sort(request)));
    }

    @GetMapping("/excel")
    @RequirePermission(menu = USER, action = Action.EXCEL)
    public void excel(@AuthenticationPrincipal AdminPrincipal principal,
                      @RequestParam(required = false) String userTypeCd, @RequestParam(required = false) String loginId,
                      @RequestParam(required = false) String userNm, @RequestParam(required = false) String email,
                      @RequestParam(required = false) String mobileNo, @RequestParam(required = false) Long companyId,
                      @RequestParam(required = false) String companyNm, @RequestParam(required = false) String statusCd,
                      @RequestParam(required = false) String joinPath,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDtFrom,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDtTo,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean privacy = authInfoService.load(principal.adminId()).has(USER, Action.PRIVACY);
        userAdminService.writeExcel(principal.adminId(), request.getRemoteAddr(), privacy,
                new UserAdminMapper.Search(userTypeCd, loginId, userNm, email, mobileNo, companyId, companyNm,
                        statusCd, joinPath, joinDtFrom, joinDtTo), sort(request),
                ExcelResponse.target(response, "사용자관리"));
    }

    @GetMapping("/check-login-id")
    @RequirePermission(menu = USER, action = Action.CREATE)
    public ApiResponse<Map<String, Boolean>> checkLoginId(@RequestParam String loginId) {
        return ApiResponse.ok(Map.of("available", userAdminService.isLoginIdAvailable(loginId)));
    }

    @GetMapping("/company-options")
    @RequirePermission(menu = USER, action = Action.READ)
    public ApiResponse<List<CompanyOption>> companyOptions(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(userAdminService.getCompanyOptions(keyword));
    }

    @GetMapping("/{userId}")
    @RequirePermission(menu = USER, action = Action.READ)
    public ApiResponse<UserDetail> user(@PathVariable long userId) {
        return ApiResponse.ok(userAdminService.getUser(userId));
    }

    @GetMapping("/{userId}/status-histories")
    @RequirePermission(menu = USER, action = Action.READ)
    public ApiResponse<List<HistoryRow>> histories(@PathVariable long userId) {
        return ApiResponse.ok(userAdminService.getStatusHistories(userId));
    }

    /** 원문은 캐시하지 않는다 */
    @PostMapping("/{userId}/privacy")
    @RequirePermission(menu = USER, action = Action.PRIVACY)
    public ResponseEntity<ApiResponse<UserPrivacy>> privacy(@AuthenticationPrincipal AdminPrincipal principal,
                                                            @PathVariable long userId,
                                                            @Valid @RequestBody PrivacyRequest req,
                                                            HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(userAdminService
                .viewPrivacy(principal.adminId(), request.getRemoteAddr(), userId, req.reasonCd(), req.reasonEtc())));
    }

    @GetMapping("/{userId}/form")
    @RequirePermission(menu = USER, action = Action.UPDATE, also = Action.PRIVACY)
    public ResponseEntity<ApiResponse<UserForm>> form(@AuthenticationPrincipal AdminPrincipal principal,
                                                      @PathVariable long userId, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(userAdminService.getForm(principal.adminId(), request.getRemoteAddr(), userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = USER, action = Action.CREATE, also = Action.PRIVACY)
    public ApiResponse<Created> create(@AuthenticationPrincipal AdminPrincipal principal,
                                       @Valid @RequestBody UserRequest req, HttpServletRequest request) {
        return ApiResponse.ok(userAdminService.create(principal.adminId(), request.getRemoteAddr(),
                new UserAdminService.CreateCommand(req.userTypeCd(), req.loginId(), req.values())));
    }

    @PutMapping("/{userId}")
    @RequirePermission(menu = USER, action = Action.UPDATE, also = Action.PRIVACY)
    public ApiResponse<Void> update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                                    @Valid @RequestBody UserRequest req, HttpServletRequest request) {
        userAdminService.update(principal.adminId(), request.getRemoteAddr(), userId, req.values(), req.modDt());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{userId}/status")
    @RequirePermission(menu = USER, action = Action.UPDATE)
    public ApiResponse<Void> changeStatus(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                                          @Valid @RequestBody StatusRequest req, HttpServletRequest request) {
        userAdminService.changeStatus(principal.adminId(), request.getRemoteAddr(), userId, req.statusCd(),
                req.reason(), req.modDt());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{userId}/password-reset")
    @RequirePermission(menu = USER, action = Action.UPDATE)
    public ApiResponse<Map<String, String>> resetPassword(@AuthenticationPrincipal AdminPrincipal principal,
                                                          @PathVariable long userId, HttpServletRequest request) {
        return ApiResponse.ok(Map.of("tempPassword",
                userAdminService.resetPassword(principal.adminId(), request.getRemoteAddr(), userId)));
    }

    @DeleteMapping("/{userId}")
    @RequirePermission(menu = USER, action = Action.DELETE)
    public ApiResponse<Void> delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                                    @Valid @RequestBody DeleteRequest req, HttpServletRequest request) {
        userAdminService.delete(principal.adminId(), request.getRemoteAddr(), userId, req.reason());
        return ApiResponse.ok(null);
    }

    /** sort는 "필드,방향"이라 List로 받으면 쉼표에서 나뉜다. 원래 값 그대로 읽는다 */
    private static List<String> sort(HttpServletRequest request) {
        String[] sort = request.getParameterValues("sort");
        return sort == null ? List.of() : List.of(sort);
    }
}
