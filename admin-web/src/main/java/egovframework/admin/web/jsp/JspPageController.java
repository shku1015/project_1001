package egovframework.admin.web.jsp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ② JSP + API 화면 껍데기. 데이터가 없는 페이지만 내려주고, 인증·데이터·권한은 모두 API(토큰)에서 처리한다
 * (docs/04-features/09-auth.md 1.1). 그래서 이 경로는 서버에서 인증하지 않는다.
 */
@Controller
public class JspPageController {

    private final String tablerVersion;

    public JspPageController(@Value("${app.ui.tabler-version}") String tablerVersion) {
        this.tablerVersion = tablerVersion;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("prefix", "/jsp");
        model.addAttribute("tablerVersion", tablerVersion);
    }

    @GetMapping("/jsp/login")
    public String login() {
        return "jsp/login";
    }

    @GetMapping({"/jsp", "/jsp/"})
    public String home() {
        return "jsp/home";
    }

    @GetMapping("/jsp/password")
    public String password() {
        return "jsp/password";
    }

    @GetMapping("/jsp/me")
    public String me() {
        return "jsp/me";
    }

    @GetMapping("/jsp/codes")
    public String codes() {
        return "jsp/code";
    }
}
