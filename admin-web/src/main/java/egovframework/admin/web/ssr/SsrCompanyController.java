package egovframework.admin.web.ssr;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.company.CompanyAdminService;
import egovframework.admin.company.CompanyAdminService.CompanyCommand;
import egovframework.admin.company.CompanyMapper;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import egovframework.admin.web.support.ExcelResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ③ JSP SSR 기업정보관리 (SCR-COM-01~03). Service를 직접 호출한다 (ADR-0003).
 */
@Controller
public class SsrCompanyController {

    private static final String COMPANY = "COMPANY";

    private final CompanyAdminService companyAdminService;
    private final AdminAuthInfoService authInfoService;

    public SsrCompanyController(CompanyAdminService companyAdminService, AdminAuthInfoService authInfoService) {
        this.companyAdminService = companyAdminService;
        this.authInfoService = authInfoService;
    }

    /** 일시 표시 형식 (docs/05-ia-screens.md 4.1: 목록은 분, 상세는 초까지) */
    @ModelAttribute
    public void formats(Model model) {
        model.addAttribute("dtMin", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        model.addAttribute("dtSec", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ================= 목록 (SCR-COM-01) =================

    @GetMapping("/ssr/companies")
    @RequirePermission(menu = COMPANY, action = Action.READ)
    public String list(@AuthenticationPrincipal AdminPrincipal principal,
                       @RequestParam(required = false) String companyNm, @RequestParam(required = false) String bizRegNo,
                       @RequestParam(required = false) String ceoNm, @RequestParam(required = false) String statusCd,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtFrom,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtTo,
                       @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
                       HttpServletRequest request, Model model) {
        String[] sort = request.getParameterValues("sort");
        CompanyMapper.Search search = new CompanyMapper.Search(companyNm, bizRegNo, ceoNm, statusCd, regDtFrom, regDtTo);
        try {
            model.addAttribute("result", companyAdminService.getCompanies(search, page, size,
                    sort == null ? List.of() : List.of(sort)));
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("result", companyAdminService.getCompanies(search, 1, 20, List.of()));
        }
        model.addAttribute("sort", sort == null ? "" : sort[0]);
        addPermissions(principal, model);
        return "ssr/company-list";
    }

    /**
     * 엑셀은 목록과 같은 검색 조건으로 내려받는다. 건수 초과 등은 목록으로 돌아가 알린다.
     * HttpServletResponse를 받으면 Spring이 응답을 직접 처리한 것으로 보므로 "redirect:" 대신 직접 리다이렉트한다.
     */
    @GetMapping("/ssr/companies/excel")
    @RequirePermission(menu = COMPANY, action = Action.EXCEL)
    public void excel(@AuthenticationPrincipal AdminPrincipal principal,
                        @RequestParam(required = false) String companyNm, @RequestParam(required = false) String bizRegNo,
                        @RequestParam(required = false) String ceoNm, @RequestParam(required = false) String statusCd,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtFrom,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regDtTo,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] sort = request.getParameterValues("sort");
        try {
            companyAdminService.writeExcel(principal.adminId(), request.getRemoteAddr(),
                    new CompanyMapper.Search(companyNm, bizRegNo, ceoNm, statusCd, regDtFrom, regDtTo),
                    sort == null ? List.of() : List.of(sort), ExcelResponse.target(response, "기업정보관리"));
        } catch (BusinessException e) {
            RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
            String url = request.getContextPath() + "/ssr/companies"
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
            RequestContextUtils.saveOutputFlashMap(url, request, response);
            response.sendRedirect(url);
        }
    }

    // ================= 상세 (SCR-COM-02) =================

    @GetMapping("/ssr/companies/{companyId}")
    @RequirePermission(menu = COMPANY, action = Action.READ)
    public String detail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long companyId,
                         Model model) {
        model.addAttribute("company", companyAdminService.getCompany(companyId));
        model.addAttribute("users", companyAdminService.getUsers(companyId, 10));
        addPermissions(principal, model);
        return "ssr/company-detail";
    }

    @PostMapping("/ssr/companies/{companyId}/status")
    @RequirePermission(menu = COMPANY, action = Action.UPDATE)
    public String changeStatus(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long companyId,
                               @RequestParam String statusCd, @RequestParam(required = false) String reason,
                               @RequestParam String modDt, HttpServletRequest request, RedirectAttributes redirect) {
        try {
            companyAdminService.changeStatus(principal.adminId(), request.getRemoteAddr(), companyId, statusCd,
                    reason, LocalDateTime.parse(modDt));
            redirect.addFlashAttribute("notice", "SUSPENDED".equals(statusCd) ? "정지되었습니다" : "정지 해제되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/ssr/companies/" + companyId;
    }

    @PostMapping("/ssr/companies/{companyId}/delete")
    @RequirePermission(menu = COMPANY, action = Action.DELETE)
    public String delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long companyId,
                         HttpServletRequest request, RedirectAttributes redirect) {
        try {
            companyAdminService.delete(principal.adminId(), request.getRemoteAddr(), companyId);
            redirect.addFlashAttribute("notice", "삭제되었습니다");
            return "redirect:/ssr/companies";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
            return "redirect:/ssr/companies/" + companyId;
        }
    }

    // ================= 등록·수정 (SCR-COM-03) =================

    @GetMapping("/ssr/companies/new")
    @RequirePermission(menu = COMPANY, action = Action.CREATE)
    public String createForm() {
        return "ssr/company-form";
    }

    @PostMapping("/ssr/companies")
    @RequirePermission(menu = COMPANY, action = Action.CREATE)
    public String create(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam(required = false) String bizRegNo,
                         CompanyForm form, HttpServletRequest request, RedirectAttributes redirect, Model model) {
        try {
            long id = companyAdminService.create(principal.adminId(), request.getRemoteAddr(),
                    bizRegNo == null ? null : bizRegNo.trim(), form.command());
            redirect.addFlashAttribute("notice", "등록되었습니다");
            return "redirect:/ssr/companies/" + id;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            return "ssr/company-form";
        }
    }

    @GetMapping("/ssr/companies/{companyId}/edit")
    @RequirePermission(menu = COMPANY, action = Action.UPDATE)
    public String editForm(@PathVariable long companyId, Model model) {
        model.addAttribute("company", companyAdminService.getCompany(companyId));
        return "ssr/company-form";
    }

    @PostMapping("/ssr/companies/{companyId}/update")
    @RequirePermission(menu = COMPANY, action = Action.UPDATE)
    public String update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long companyId,
                         CompanyForm form, @RequestParam String modDt, HttpServletRequest request,
                         RedirectAttributes redirect, Model model) {
        try {
            companyAdminService.update(principal.adminId(), request.getRemoteAddr(), companyId, form.command(),
                    LocalDateTime.parse(modDt));
            redirect.addFlashAttribute("notice", "수정되었습니다");
            return "redirect:/ssr/companies/" + companyId;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            model.addAttribute("company", companyAdminService.getCompany(companyId));
            return "ssr/company-form";
        }
    }

    /** 등록·수정 폼 값 (사업자등록번호는 등록 때만 따로 받는다) */
    public record CompanyForm(String companyNm, String ceoNm, String bizType, String bizItem, String telNo,
                              String zipCd, String addr, String addrDtl) {

        CompanyCommand command() {
            return new CompanyCommand(companyNm, ceoNm, bizType, bizItem, telNo, zipCd, addr, addrDtl);
        }
    }

    // ================= 공통 =================

    /** 권한이 없는 버튼은 비활성으로 보여 준다 (docs/05-ia-screens.md 4.2) */
    private void addPermissions(AdminPrincipal principal, Model model) {
        AdminAuthInfo auth = authInfoService.load(principal.adminId());
        model.addAttribute("canCreate", auth.has(COMPANY, Action.CREATE));
        model.addAttribute("canUpdate", auth.has(COMPANY, Action.UPDATE));
        model.addAttribute("canDelete", auth.has(COMPANY, Action.DELETE));
        model.addAttribute("canExcel", auth.has(COMPANY, Action.EXCEL));
    }

    private static String message(BusinessException e) {
        return e.getFieldErrors().isEmpty() ? e.getMessage() : e.getFieldErrors().get(0).message();
    }
}
