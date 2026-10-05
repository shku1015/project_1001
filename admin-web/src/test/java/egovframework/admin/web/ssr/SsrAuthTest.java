package egovframework.admin.web.ssr;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;

/**
 * ③ JSP SSR 세션 로그인 (실제 HTTP 요청: 세션 쿠키, CSRF, Spring Session JDBC).
 */
@IntegrationTestWithData
class SsrAuthTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    /** 쿠키(SESSION)를 기억하고 리다이렉트는 따라가지 않는 브라우저 흉내 */
    private static final class Browser {
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new java.net.CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        private final String base;

        Browser(int port) {
            this.base = "http://localhost:" + port;
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> postForm(String path, String form) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(base + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        }

        String csrf(String html) {
            Matcher m = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
            assertThat(m.find()).as("CSRF 토큰이 화면에 있어야 한다").isTrue();
            return m.group(1);
        }

        HttpResponse<String> login(String loginId, String password) throws Exception {
            String csrf = csrf(get("/ssr/login").body());
            return postForm("/ssr/login", "_csrf=" + enc(csrf) + "&loginId=" + enc(loginId) + "&password=" + enc(password));
        }

        static String enc(String v) {
            return URLEncoder.encode(v, StandardCharsets.UTF_8);
        }
    }

    @Test
    void 로그인하지_않으면_로그인_화면으로_보낸다() throws Exception {
        HttpResponse<String> res = new Browser(port).get("/ssr/");
        assertThat(res.statusCode()).isEqualTo(302);
        assertThat(res.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/login");
    }

    @Test
    void CSRF_토큰_없는_로그인_요청은_거부한다() throws Exception {
        Browser browser = new Browser(port);
        browser.get("/ssr/login");
        HttpResponse<String> res = browser.postForm("/ssr/login",
                "loginId=t_system&password=" + Browser.enc(AuthTestSupport.TEST_PASSWORD));
        assertThat(res.statusCode()).isEqualTo(403);
    }

    @Test
    void 로그인하면_홈으로_가고_세션이_DB에_저장된다() throws Exception {
        String loginId = AuthTestSupport.createAdmin(jdbc, encoder, "it_ssr", "SYSTEM_ADMIN", false);
        Browser browser = new Browser(port);
        HttpResponse<String> login = browser.login(loginId, AuthTestSupport.TEST_PASSWORD);
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/");

        HttpResponse<String> home = browser.get("/ssr/");
        assertThat(home.statusCode()).isEqualTo(200);
        // 홈: 내 이름(테스트전용), 왼쪽 메뉴(시스템관리자는 시스템관리·관리자관리 폴더), 바로가기
        assertThat(home.body()).contains("테스트전용", "시스템관리", "관리자관리", "id=\"home-shortcuts\"");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session WHERE principal_name = ?",
                Integer.class, loginId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT auth_type_cd FROM tb_admin_login_hist WHERE login_id = ? ORDER BY hist_id DESC LIMIT 1
                """, String.class, loginId)).isEqualTo("SESSION");
    }

    @Test
    void 잘못된_비밀번호는_로그인_화면에_같은_메시지를_보여_준다() throws Exception {
        Browser browser = new Browser(port);
        HttpResponse<String> res = browser.login("t_system", "wrong");
        assertThat(res.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/login?error");
        assertThat(browser.get("/ssr/login?error").body()).contains("아이디 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    void 다른_곳에서_API로_로그인하면_기존_세션이_끊긴다() throws Exception {
        String loginId = AuthTestSupport.createAdmin(jdbc, encoder, "it_ssr", "VIEWER", false);
        Browser browser = new Browser(port);
        browser.login(loginId, AuthTestSupport.TEST_PASSWORD);
        assertThat(browser.get("/ssr/").statusCode()).isEqualTo(200);

        AuthTestSupport.login(mockMvc, loginId);    // NF-LG-02

        HttpResponse<String> after = browser.get("/ssr/");
        assertThat(after.statusCode()).isEqualTo(302);
        assertThat(after.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/login");
    }

    @Test
    void 임시_비밀번호면_비밀번호_변경_화면으로_보내고_바꾸면_홈으로_간다() throws Exception {
        String loginId = AuthTestSupport.createAdmin(jdbc, encoder, "it_ssr", "VIEWER", true);
        Browser browser = new Browser(port);
        HttpResponse<String> login = browser.login(loginId, AuthTestSupport.TEST_PASSWORD);
        assertThat(login.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/password");

        // 비밀번호를 바꾸기 전에는 홈으로 갈 수 없다
        HttpResponse<String> home = browser.get("/ssr/");
        assertThat(home.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/password");

        String csrf = browser.csrf(browser.get("/ssr/password").body());
        HttpResponse<String> changed = browser.postForm("/ssr/password", "_csrf=" + Browser.enc(csrf)
                + "&currentPassword=" + Browser.enc(AuthTestSupport.TEST_PASSWORD)
                + "&newPassword=new-pw&newPasswordConfirm=new-pw");
        assertThat(changed.statusCode()).isEqualTo(302);
        assertThat(changed.headers().firstValue("Location").orElseThrow()).endsWith("/ssr/");
        // 현재 세션은 유지된다
        assertThat(browser.get("/ssr/").statusCode()).isEqualTo(200);
    }
}
