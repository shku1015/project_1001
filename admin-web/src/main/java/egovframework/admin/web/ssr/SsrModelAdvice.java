package egovframework.admin.web.ssr;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import egovframework.admin.auth.MeService;
import egovframework.admin.menu.MenuService.MenuNode;
import egovframework.admin.web.security.AdminPrincipal;

/**
 * ③ SSR 화면 공통 데이터: 레이아웃(헤더·왼쪽 메뉴)에 쓰는 내 정보와 화면 접두어.
 */
@ControllerAdvice(basePackages = "egovframework.admin.web.ssr")
public class SsrModelAdvice {

    private final MeService meService;
    private final String tablerVersion;

    public SsrModelAdvice(MeService meService, @Value("${app.ui.tabler-version}") String tablerVersion) {
        this.meService = meService;
        this.tablerVersion = tablerVersion;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("prefix", "/ssr");
        model.addAttribute("tablerVersion", tablerVersion);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal principal && !model.containsAttribute("me")) {
            MeService.Me me = meService.getMe(principal.adminId());
            model.addAttribute("me", me);
            model.addAttribute("shortcuts", shortcuts(me.menus()));
        }
    }

    /** 홈 바로가기: 볼 수 있는 화면 메뉴 중 앞쪽 6개 (docs/05-screens/00-common.md SCR-HOME) */
    static List<MenuNode> shortcuts(List<MenuNode> menus) {
        List<MenuNode> pages = new ArrayList<>();
        collect(menus, pages);
        return pages.size() > 6 ? pages.subList(0, 6) : pages;
    }

    private static void collect(List<MenuNode> menus, List<MenuNode> pages) {
        for (MenuNode m : menus) {
            if ("PAGE".equals(m.menuTypeCd())) {
                pages.add(m);
            } else {
                collect(m.children(), pages);
            }
        }
    }
}
