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
import egovframework.admin.code.CodeService;
import egovframework.admin.common.BusinessException;
import egovframework.admin.user.UserAdminMapper;
import egovframework.admin.user.UserAdminMapper.UserValues;
import egovframework.admin.user.UserAdminService;
import egovframework.admin.user.UserAdminService.UserForm;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import egovframework.admin.web.support.ExcelResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ③ JSP SSR 사용자관리 (SCR-USR-01~03). Service를 직접 호출한다 (ADR-0003).
 * 개인정보 원문 보기는 POST 결과 화면에서만 원문을 보여 준다 (세션·Flash에 원문을 남기지 않는다).
 * 임시 비밀번호는 Flash 속성으로 상세 화면에 한 번만 넘긴다.
 */
@Controller
public class SsrUserController {

    private static final String USER = "USER";

    private final UserAdminService userAdminService;
    private final AdminAuthInfoService authInfoService;
    private final CodeService codeService;

    public SsrUserController(UserAdminService userAdminService, AdminAuthInfoService authInfoService,
                             CodeService codeService) {
        this.userAdminService = userAdminService;
        this.authInfoService = authInfoService;
        this.codeService = codeService;
    }

    /** 일시 표시 형식 (docs/05-ia-screens.md 4.1: 목록은 분, 상세는 초까지) */
    @ModelAttribute
    public void formats(Model model) {
        model.addAttribute("dtMin", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        model.addAttribute("dtSec", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** 목록·엑셀 검색 조건 (요청 파라미터 이름 그대로) */
    public record SearchForm(String userTypeCd, String loginId, String userNm, String email, String mobileNo,
                             Long companyId, String companyNm, String statusCd, String joinPath,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDtFrom,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDtTo) {

        UserAdminMapper.Search search() {
            return new UserAdminMapper.Search(userTypeCd, loginId, userNm, email, mobileNo, companyId, companyNm,
                    statusCd, joinPath, joinDtFrom, joinDtTo);
        }
    }

    // ================= 목록 (SCR-USR-01) =================

    @GetMapping("/ssr/users")
    @RequirePermission(menu = USER, action = Action.READ)
    public String list(@AuthenticationPrincipal AdminPrincipal principal, SearchForm form,
                       @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
                       HttpServletRequest request, Model model) {
        String[] sort = request.getParameterValues("sort");
        try {
            model.addAttribute("result", userAdminService.getUsers(form.search(), page, size,
                    sort == null ? List.of() : List.of(sort)));
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("result", userAdminService.getUsers(form.search(), 1, 20, List.of()));
        }
        model.addAttribute("sort", sort == null ? "" : sort[0]);
        addPermissions(principal, model);
        return "ssr/user-list";
    }

    /** 엑셀. 건수 초과 등은 목록으로 돌아가 알린다 (HttpServletResponse를 받으므로 직접 리다이렉트) */
    @GetMapping("/ssr/users/excel")
    @RequirePermission(menu = USER, action = Action.EXCEL)
    public void excel(@AuthenticationPrincipal AdminPrincipal principal, SearchForm form, HttpServletRequest request,
                      HttpServletResponse response) throws IOException {
        String[] sort = request.getParameterValues("sort");
        try {
            userAdminService.writeExcel(principal.adminId(), request.getRemoteAddr(),
                    authInfoService.load(principal.adminId()).has(USER, Action.PRIVACY), form.search(),
                    sort == null ? List.of() : List.of(sort), ExcelResponse.target(response, "사용자관리"));
        } catch (BusinessException e) {
            RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
            String url = request.getContextPath() + "/ssr/users"
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
            RequestContextUtils.saveOutputFlashMap(url, request, response);
            response.sendRedirect(url);
        }
    }

    // ================= 상세 (SCR-USR-02) =================

    @GetMapping("/ssr/users/{userId}")
    @RequirePermission(menu = USER, action = Action.READ)
    public String detail(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId, Model model) {
        renderDetail(principal, userId, model);
        return "ssr/user-detail";
    }

    /**
     * 개인정보 원문 보기 (CMP-08). 원문은 이 응답 화면에만 넣는다.
     * 캐시 금지(Cache-Control no-store)는 Spring Security 기본 헤더가 모든 응답에 붙인다.
     * (화면을 돌려주는 메서드는 HttpServletResponse를 받으면 Spring이 응답을 직접 처리한 것으로 보므로 받지 않는다)
     */
    @PostMapping("/ssr/users/{userId}/privacy")
    @RequirePermission(menu = USER, action = Action.PRIVACY)
    public String privacy(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                          @RequestParam(required = false) String reasonCd,
                          @RequestParam(required = false) String reasonEtc, HttpServletRequest request,
                          Model model) {
        try {
            model.addAttribute("privacy", userAdminService.viewPrivacy(principal.adminId(), request.getRemoteAddr(),
                    userId, reasonCd, reasonEtc));
        } catch (BusinessException e) {
            model.addAttribute("error", message(e));
        }
        renderDetail(principal, userId, model);
        return "ssr/user-detail";
    }

    @PostMapping("/ssr/users/{userId}/status")
    @RequirePermission(menu = USER, action = Action.UPDATE)
    public String changeStatus(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                               @RequestParam String statusCd, @RequestParam(required = false) String reason,
                               @RequestParam String modDt, HttpServletRequest request, RedirectAttributes redirect) {
        try {
            userAdminService.changeStatus(principal.adminId(), request.getRemoteAddr(), userId, statusCd, reason,
                    LocalDateTime.parse(modDt));
            redirect.addFlashAttribute("notice", "상태가 변경되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/ssr/users/" + userId;
    }

    @PostMapping("/ssr/users/{userId}/password-reset")
    @RequirePermission(menu = USER, action = Action.UPDATE)
    public String resetPassword(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                                HttpServletRequest request, RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("tempPassword",
                    userAdminService.resetPassword(principal.adminId(), request.getRemoteAddr(), userId));
            redirect.addFlashAttribute("notice", "비밀번호가 초기화되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/ssr/users/" + userId;
    }

    @PostMapping("/ssr/users/{userId}/delete")
    @RequirePermission(menu = USER, action = Action.DELETE)
    public String delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                         @RequestParam(required = false) String reason, HttpServletRequest request,
                         RedirectAttributes redirect) {
        try {
            userAdminService.delete(principal.adminId(), request.getRemoteAddr(), userId, reason);
            redirect.addFlashAttribute("notice", "삭제되었습니다");
            return "redirect:/ssr/users";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
            return "redirect:/ssr/users/" + userId;
        }
    }

    // ================= 등록·수정 (SCR-USR-03) =================

    @GetMapping("/ssr/users/new")
    @RequirePermission(menu = USER, action = Action.CREATE, also = Action.PRIVACY)
    public String createForm(Model model) {
        model.addAttribute("companies", userAdminService.getCompanyOptions(null));
        return "ssr/user-form";
    }

    @PostMapping("/ssr/users")
    @RequirePermission(menu = USER, action = Action.CREATE, also = Action.PRIVACY)
    public String create(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam(required = false) String userTypeCd,
                         @RequestParam(required = false) String loginId, ValuesForm form, HttpServletRequest request,
                         RedirectAttributes redirect, Model model) {
        try {
            UserAdminService.Created created = userAdminService.create(principal.adminId(), request.getRemoteAddr(),
                    new UserAdminService.CreateCommand(userTypeCd, loginId == null ? null : loginId.trim(),
                            form.values()));
            redirect.addFlashAttribute("notice", "등록되었습니다");
            redirect.addFlashAttribute("tempPassword", created.tempPassword());
            return "redirect:/ssr/users/" + created.userId();
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            model.addAttribute("companies", userAdminService.getCompanyOptions(null));
            return "ssr/user-form";
        }
    }

    /** 수정 화면은 원문을 보여 주므로 들어올 때 감사로그를 남긴다 (Service) */
    @GetMapping("/ssr/users/{userId}/edit")
    @RequirePermission(menu = USER, action = Action.UPDATE, also = Action.PRIVACY)
    public String editForm(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                           HttpServletRequest request, RedirectAttributes redirect, Model model) {
        try {
            model.addAttribute("user", userAdminService.getForm(principal.adminId(), request.getRemoteAddr(), userId));
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
            return "redirect:/ssr/users/" + userId;
        }
        model.addAttribute("companies", userAdminService.getCompanyOptions(null));
        return "ssr/user-form";
    }

    @PostMapping("/ssr/users/{userId}/update")
    @RequirePermission(menu = USER, action = Action.UPDATE, also = Action.PRIVACY)
    public String update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long userId,
                         ValuesForm form, @RequestParam String modDt, HttpServletRequest request,
                         RedirectAttributes redirect, Model model) {
        try {
            userAdminService.update(principal.adminId(), request.getRemoteAddr(), userId, form.values(),
                    LocalDateTime.parse(modDt));
            redirect.addFlashAttribute("notice", "수정되었습니다");
            return "redirect:/ssr/users/" + userId;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            UserForm user = userAdminService.getForm(principal.adminId(), request.getRemoteAddr(), userId);
            model.addAttribute("user", user);
            model.addAttribute("companies", userAdminService.getCompanyOptions(null));
            return "ssr/user-form";
        }
    }

    /** 등록·수정 폼 값 */
    public record ValuesForm(String userNm, String email, String mobileNo,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate, Long companyId,
                             String deptNm, String positionNm) {

        UserValues values() {
            return new UserValues(userNm, email, mobileNo, birthDate, companyId, deptNm, positionNm);
        }
    }

    // ================= 공통 =================

    private void renderDetail(AdminPrincipal principal, long userId, Model model) {
        model.addAttribute("user", userAdminService.getUser(userId));
        model.addAttribute("histories", userAdminService.getStatusHistories(userId));
        model.addAttribute("privacyReasons", codeService.getCodes("PRIVACY_REASON", false));
        addPermissions(principal, model);
    }

    /** 권한이 없는 버튼은 비활성으로 보여 준다 (docs/05-ia-screens.md 4.2) */
    private void addPermissions(AdminPrincipal principal, Model model) {
        AdminAuthInfo auth = authInfoService.load(principal.adminId());
        boolean privacy = auth.has(USER, Action.PRIVACY);
        model.addAttribute("canCreate", auth.has(USER, Action.CREATE) && privacy);
        model.addAttribute("canEdit", auth.has(USER, Action.UPDATE) && privacy);
        model.addAttribute("canUpdate", auth.has(USER, Action.UPDATE));
        model.addAttribute("canDelete", auth.has(USER, Action.DELETE));
        model.addAttribute("canExcel", auth.has(USER, Action.EXCEL));
        model.addAttribute("canPrivacy", privacy);
    }

    private static String message(BusinessException e) {
        return e.getFieldErrors().isEmpty() ? e.getMessage() : e.getFieldErrors().get(0).message();
    }
}
