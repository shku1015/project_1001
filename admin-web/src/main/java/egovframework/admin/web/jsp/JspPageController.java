package egovframework.admin.web.jsp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * ② JSP + API 화면 껍데기. 데이터가 없는 페이지만 내려주고, 인증·데이터·권한은 모두 API(토큰)에서 처리한다
 * (docs/04-features/09-auth.md 1.1). 그래서 이 경로는 서버에서 인증하지 않는다.
 */
@Controller
public class JspPageController {

    private final String tablerVersion;
    private final String sortablejsVersion;

    public JspPageController(@Value("${app.ui.tabler-version}") String tablerVersion,
                             @Value("${app.ui.sortablejs-version}") String sortablejsVersion) {
        this.tablerVersion = tablerVersion;
        this.sortablejsVersion = sortablejsVersion;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("prefix", "/jsp");
        model.addAttribute("tablerVersion", tablerVersion);
        model.addAttribute("sortablejsVersion", sortablejsVersion);
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

    @GetMapping("/jsp/menus")
    public String menus() {
        return "jsp/menu";
    }

    @GetMapping("/jsp/codes")
    public String codes() {
        return "jsp/code";
    }

    @GetMapping("/jsp/permissions")
    public String permissions() {
        return "jsp/permission";
    }

    @GetMapping("/jsp/permissions/admins")
    public String permissionAdmins() {
        return "jsp/permission-admin";
    }

    @GetMapping("/jsp/admins")
    public String admins() {
        return "jsp/admin-list";
    }

    @GetMapping("/jsp/admins/new")
    public String adminNew() {
        return "jsp/admin-form";
    }

    @GetMapping("/jsp/admins/{adminId}")
    public String admin(@PathVariable long adminId, Model model) {
        model.addAttribute("adminId", adminId);
        return "jsp/admin-detail";
    }

    @GetMapping("/jsp/admins/{adminId}/edit")
    public String adminEdit(@PathVariable long adminId, Model model) {
        model.addAttribute("adminId", adminId);
        return "jsp/admin-form";
    }

    @GetMapping("/jsp/roles")
    public String roles() {
        return "jsp/role-list";
    }

    @GetMapping("/jsp/roles/new")
    public String roleNew() {
        return "jsp/role-form";
    }

    @GetMapping("/jsp/roles/{roleId}")
    public String role(@PathVariable long roleId, Model model) {
        model.addAttribute("roleId", roleId);
        return "jsp/role-detail";
    }

    @GetMapping("/jsp/roles/{roleId}/edit")
    public String roleEdit(@PathVariable long roleId, Model model) {
        model.addAttribute("roleId", roleId);
        return "jsp/role-form";
    }
}
