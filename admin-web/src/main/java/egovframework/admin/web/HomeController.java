package egovframework.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 안내 페이지. 세 프론트(/react, /jsp, /ssr) 중 하나를 고른다 (docs/08-architecture.md 1.1).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
