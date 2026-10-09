package egovframework.admin.web.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
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
import egovframework.admin.common.PageResult;
import egovframework.admin.company.CompanyAdminService;
import egovframework.admin.company.CompanyAdminService.CompanyCommand;
import egovframework.admin.company.CompanyAdminService.CompanyUsers;
import egovframework.admin.company.CompanyMapper;
import egovframework.admin.company.CompanyMapper.DetailRow;
import egovframework.admin.company.CompanyMapper.ListRow;
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
 * 기업정보관리 API (docs/06-api/01-company.md, api/openapi.yaml company 태그). 메뉴 코드 COMPANY.
 */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyApiController {

    private static final String COMPANY = "COMPANY";

    public record CompanyRequest(@NotNull @Size(min = 1, max = 100) String companyNm,
                                 @Pattern(regexp = "^[0-9]{10}$") String bizRegNo,
                                 @NotNull @Size(min = 1, max = 50) String ceoNm,
                                 @Size(max = 100) String bizType, @Size(max = 100) String bizItem,
                                 @Pattern(regexp = "^[0-9]{9,11}$") String telNo,
                                 @Pattern(regexp = "^[0-9]{5}$") String zipCd,
                                 @Size(max = 200) String addr, @Size(max = 200) String addrDtl,
                                 LocalDateTime modDt) {

        CompanyCommand command() {
            return new CompanyCommand(companyNm, ceoNm, bizType, bizItem, telNo, zipCd, addr, addrDtl);
        }
    }

    public record StatusRequest(@NotNull @Pattern(regexp = "^(ACTIVE|SUSPENDED)$") String statusCd,
                                @NotNull @Size(min = 1, max = 500) String reason,
                                @NotNull LocalDateTime modDt) {
    }

    private final CompanyAdminService companyAdminService;

    public CompanyApiController(CompanyAdminService companyAdminService) {
        this.companyAdminService = companyAdminService;
    }

    @GetMapping
    @RequirePermission(menu = COMPANY, action = Action.READ)
    public ApiResponse<PageResult<ListRow>> list(@RequestParam(required = false) String companyNm,
                                                 @RequestParam(required = false) String bizRegNo,
                                                 @RequestParam(required = false) String ceoNm,
                                                 @RequestParam(required = false) String statusCd,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtFrom,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtTo,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size,
                                                 HttpServletRequest request) {
        return ApiResponse.ok(companyAdminService.getCompanies(
                new CompanyMapper.Search(companyNm, bizRegNo, ceoNm, statusCd, regDtFrom, regDtTo), page, size,
                sort(request)));
    }

    @GetMapping("/excel")
    @RequirePermission(menu = COMPANY, action = Action.EXCEL)
    public void excel(@AuthenticationPrincipal AdminPrincipal principal,
                      @RequestParam(required = false) String companyNm, @RequestParam(required = false) String bizRegNo,
                      @RequestParam(required = false) String ceoNm, @RequestParam(required = false) String statusCd,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtFrom,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtTo,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        companyAdminService.writeExcel(principal.adminId(), request.getRemoteAddr(),
                new CompanyMapper.Search(companyNm, bizRegNo, ceoNm, statusCd, regDtFrom, regDtTo), sort(request),
                ExcelResponse.target(response, "기업정보관리"));
    }

    @GetMapping("/check-biz-reg-no")
    @RequirePermission(menu = COMPANY, action = Action.CREATE)
    public ApiResponse<Map<String, Boolean>> checkBizRegNo(@RequestParam String bizRegNo) {
        return ApiResponse.ok(Map.of("available", companyAdminService.isBizRegNoAvailable(bizRegNo)));
    }

    @GetMapping("/{companyId}")
    @RequirePermission(menu = COMPANY, action = Action.READ)
    public ApiResponse<DetailRow> company(@PathVariable long companyId) {
        return ApiResponse.ok(companyAdminService.getCompany(companyId));
    }

    @GetMapping("/{companyId}/users")
    @RequirePermission(menu = COMPANY, action = Action.READ)
    public ApiResponse<CompanyUsers> users(@PathVariable long companyId,
                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(companyAdminService.getUsers(companyId, Math.min(Math.max(size, 1), 50)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(menu = COMPANY, action = Action.CREATE)
    public ApiResponse<Map<String, Long>> create(@AuthenticationPrincipal AdminPrincipal principal,
                                                 @Valid @RequestBody CompanyRequest req, HttpServletRequest request) {
        return ApiResponse.ok(Map.of("companyId", companyAdminService.create(principal.adminId(),
                request.getRemoteAddr(), req.bizRegNo(), req.command())));
    }

    @PutMapping("/{companyId}")
    @RequirePermission(menu = COMPANY, action = Action.UPDATE)
    public ApiResponse<Void> update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long companyId,
                                    @Valid @RequestBody CompanyRequest req, HttpServletRequest request) {
        companyAdminService.update(principal.adminId(), request.getRemoteAddr(), companyId, req.command(),
                req.modDt());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{companyId}/status")
    @RequirePermission(menu = COMPANY, action = Action.UPDATE)
    public ApiResponse<Void> changeStatus(@AuthenticationPrincipal AdminPrincipal principal,
                                          @PathVariable long companyId, @Valid @RequestBody StatusRequest req,
                                          HttpServletRequest request) {
        companyAdminService.changeStatus(principal.adminId(), request.getRemoteAddr(), companyId, req.statusCd(),
                req.reason(), req.modDt());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{companyId}")
    @RequirePermission(menu = COMPANY, action = Action.DELETE)
    public ApiResponse<Void> delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long companyId,
                                    HttpServletRequest request) {
        companyAdminService.delete(principal.adminId(), request.getRemoteAddr(), companyId);
        return ApiResponse.ok(null);
    }

    /** sort는 "필드,방향"이라 List로 받으면 쉼표에서 나뉜다. 원래 값 그대로 읽는다 */
    private static List<String> sort(HttpServletRequest request) {
        String[] sort = request.getParameterValues("sort");
        return sort == null ? List.of() : List.of(sort);
    }
}
