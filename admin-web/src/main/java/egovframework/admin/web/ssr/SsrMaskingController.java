package egovframework.admin.web.ssr;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.masking.MaskingAdminService;
import egovframework.admin.masking.MaskingAdminService.PolicyChange;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 마스킹 설정 (SCR-MSK-01). 바뀐 항목만 보내야 하므로 JavaScript가 처음 값과 비교해 JSON으로 모아 보낸다.
 */
@Controller
public class SsrMaskingController {

    private static final String MASKING = "MASKING";

    private final MaskingAdminService maskingAdminService;
    private final AdminAuthInfoService authInfoService;
    private final ObjectMapper objectMapper;

    public SsrMaskingController(MaskingAdminService maskingAdminService, AdminAuthInfoService authInfoService,
                                ObjectMapper objectMapper) {
        this.maskingAdminService = maskingAdminService;
        this.authInfoService = authInfoService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/ssr/masking")
    @RequirePermission(menu = MASKING, action = Action.READ)
    public String masking(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("policies", maskingAdminService.getPolicies());
        model.addAttribute("canUpdate", authInfoService.load(principal.adminId()).has(MASKING, Action.UPDATE));
        model.addAttribute("dtMin", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        // JSON으로 다시 보내는 modDt는 API 일시 형식(초 단위, 소수 없음)으로 넣는다 (toString은 소수 초가 붙는다)
        model.addAttribute("dtApi", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        return "ssr/masking";
    }

    @PostMapping("/ssr/masking")
    @RequirePermission(menu = MASKING, action = Action.UPDATE)
    public String save(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam String policies,
                       HttpServletRequest request, RedirectAttributes redirect) {
        try {
            List<PolicyChange> changes = objectMapper.readValue(policies, new TypeReference<List<PolicyChange>>() { });
            maskingAdminService.save(principal.adminId(), request.getRemoteAddr(), changes);
            redirect.addFlashAttribute("notice", "저장되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (JsonProcessingException e) {
            redirect.addFlashAttribute("error", ErrorCode.INVALID_REQUEST.message());
        }
        return "redirect:/ssr/masking";
    }
}
