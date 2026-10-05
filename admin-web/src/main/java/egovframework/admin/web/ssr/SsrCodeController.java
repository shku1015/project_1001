package egovframework.admin.web.ssr;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.code.CodeAdminService;
import egovframework.admin.code.CodeGroup;
import egovframework.admin.code.CodeMapper;
import egovframework.admin.common.BusinessException;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 코드관리 (SCR-COD-01). Service를 직접 호출한다 (ADR-0003).
 * 등록·수정·삭제는 폼 전송 후 /ssr/codes로 리다이렉트하고 1회성 메시지를 보여 준다.
 */
@Controller
public class SsrCodeController {

    private final CodeAdminService codeAdminService;

    public SsrCodeController(CodeAdminService codeAdminService) {
        this.codeAdminService = codeAdminService;
    }

    @GetMapping("/ssr/codes")
    @RequirePermission(menu = "CODE", action = Action.READ)
    public String codes(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String useYn,
                        @RequestParam(required = false) String group, Model model) {
        model.addAttribute("groups", codeAdminService.getGroups(keyword, useYn));
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchUseYn", useYn);
        if (group != null && !group.isBlank()) {
            CodeGroup selected = codeAdminService.getGroup(group);
            model.addAttribute("selectedGroup", selected);
            model.addAttribute("details", codeAdminService.getDetails(group));
            model.addAttribute("nextSortOrd", codeAdminService.nextSortOrd(group));
        }
        return "ssr/code";
    }

    // ===== 그룹코드 =====

    @PostMapping("/ssr/codes/groups")
    @RequirePermission(menu = "CODE", action = Action.CREATE)
    public String createGroup(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam String groupCd,
                              @RequestParam String groupNm, @RequestParam(required = false) String description,
                              @RequestParam String useYn, HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, group(groupCd), () -> codeAdminService.createGroup(principal.adminId(),
                request.getRemoteAddr(),
                new CodeMapper.CodeGroupRequest(groupCd, groupNm, blank(description), useYn)), "등록되었습니다");
    }

    @PostMapping("/ssr/codes/groups/{groupCd}/update")
    @RequirePermission(menu = "CODE", action = Action.UPDATE)
    public String updateGroup(@AuthenticationPrincipal AdminPrincipal principal,
                              @PathVariable String groupCd,
                              @RequestParam String groupNm, @RequestParam(required = false) String description,
                              @RequestParam String useYn, @RequestParam String modDt,
                              HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, group(groupCd), () -> codeAdminService.updateGroup(principal.adminId(),
                request.getRemoteAddr(), groupCd, groupNm, blank(description), useYn, LocalDateTime.parse(modDt)),
                "수정되었습니다");
    }

    @PostMapping("/ssr/codes/groups/{groupCd}/delete")
    @RequirePermission(menu = "CODE", action = Action.DELETE)
    public String deleteGroup(@AuthenticationPrincipal AdminPrincipal principal,
                              @PathVariable String groupCd,
                              HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, "/ssr/codes", () -> codeAdminService.deleteGroup(principal.adminId(),
                request.getRemoteAddr(), groupCd), "삭제되었습니다");
    }

    // ===== 상세코드 =====

    @PostMapping("/ssr/codes/groups/{groupCd}/codes")
    @RequirePermission(menu = "CODE", action = Action.CREATE)
    public String createDetail(@AuthenticationPrincipal AdminPrincipal principal,
                               @PathVariable String groupCd,
                               @RequestParam String code, @RequestParam String codeNm, @RequestParam int sortOrd,
                               @RequestParam(required = false) String description, @RequestParam String useYn,
                               HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, group(groupCd), () -> codeAdminService.createDetail(principal.adminId(),
                request.getRemoteAddr(), groupCd,
                new CodeMapper.CodeDetailRequest(code, codeNm, sortOrd, blank(description), useYn)), "등록되었습니다");
    }

    @PostMapping("/ssr/codes/groups/{groupCd}/codes/{code}/update")
    @RequirePermission(menu = "CODE", action = Action.UPDATE)
    public String updateDetail(@AuthenticationPrincipal AdminPrincipal principal,
                               @PathVariable String groupCd,
                               @PathVariable String code,
                               @RequestParam String codeNm, @RequestParam int sortOrd,
                               @RequestParam(required = false) String description, @RequestParam String useYn,
                               @RequestParam String modDt, HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, group(groupCd), () -> codeAdminService.updateDetail(principal.adminId(),
                request.getRemoteAddr(), groupCd, code, codeNm, sortOrd, blank(description), useYn,
                LocalDateTime.parse(modDt)), "수정되었습니다");
    }

    @PostMapping("/ssr/codes/groups/{groupCd}/codes/{code}/delete")
    @RequirePermission(menu = "CODE", action = Action.DELETE)
    public String deleteDetail(@AuthenticationPrincipal AdminPrincipal principal,
                               @PathVariable String groupCd,
                               @PathVariable String code,
                               HttpServletRequest request, RedirectAttributes redirect) {
        return run(redirect, group(groupCd), () -> codeAdminService.deleteDetail(principal.adminId(),
                request.getRemoteAddr(), groupCd, code), "삭제되었습니다");
    }

    /** 업무 예외는 메시지를 띄우고 같은 화면으로 돌아간다 */
    private String run(RedirectAttributes redirect, String target, Runnable action, String successMessage) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", successMessage);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getFieldErrors().isEmpty()
                    ? e.getMessage() : e.getFieldErrors().get(0).message());
        }
        return "redirect:" + target;
    }

    private static String group(String groupCd) {
        return "/ssr/codes?group=" + groupCd;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
