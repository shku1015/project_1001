package egovframework.admin.web.ssr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ③ JSP SSR 오류 화면 (SCR-ERR-403/404/500). API와 같은 오류 코드를 화면 메시지로 쓴다.
 */
@ControllerAdvice(basePackages = "egovframework.admin.web.ssr")
public class SsrExceptionHandler {

    private static final Logger log = LogManager.getLogger(SsrExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, HttpServletResponse response, Model model) {
        ErrorCode code = e.getErrorCode();
        response.setStatus(code.status());
        model.addAttribute("status", code.status());
        model.addAttribute("message", code == ErrorCode.FORBIDDEN ? "이 화면을 볼 권한이 없습니다" : e.getMessage());
        return "ssr/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception e, HttpServletResponse response, Model model) {
        log.error("처리하지 못한 SSR 오류", e);
        response.setStatus(500);
        model.addAttribute("status", 500);
        model.addAttribute("message", ErrorCode.INTERNAL_ERROR.message());
        return "ssr/error";
    }
}
