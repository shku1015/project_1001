package egovframework.admin.web.api;

import static egovframework.admin.web.support.AuthTestSupport.TEST_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import egovframework.admin.web.config.AuthProperties;
import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.AuthTestSupport.Login;
import egovframework.admin.web.support.IntegrationTestWithData;
import egovframework.admin.web.support.OpenApiContract;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;

/**
 * 인증 API (docs/04-features/09-auth.md, docs/06-api/00-auth.md). 응답은 api/openapi.yaml과 대조한다.
 */
@IntegrationTestWithData
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthProperties authProperties;

    private String newAdmin(String roleCd, boolean pwdTemp) {
        return AuthTestSupport.createAdmin(jdbc, encoder, "it_auth", roleCd, pwdTemp);
    }

    private String loginResult(String loginId) {
        return jdbc.queryForObject("""
                SELECT result_cd FROM tb_admin_login_hist WHERE login_id = ? ORDER BY hist_id DESC LIMIT 1
                """, String.class, loginId);
    }

    // ---------------------------------------------------------------- 로그인

    @Test
    void 로그인하면_AccessToken과_RefreshToken_쿠키를_준다() throws Exception {
        MvcResult result = AuthTestSupport.loginRaw(mockMvc, "t_system", TEST_PASSWORD);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        OpenApiContract.assertValid(result);

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=", "HttpOnly", "SameSite=Strict", "Path=/api/v1/auth/token");
        assertThat(JsonPath.<Integer>read(result.getResponse().getContentAsString(), "$.data.expiresIn"))
                .isEqualTo(1800);
        assertThat(loginResult("t_system")).isEqualTo("SUCCESS");
    }

    @Test
    void 아이디가_없어도_비밀번호가_틀려도_같은_오류() throws Exception {
        MvcResult noId = AuthTestSupport.loginRaw(mockMvc, "no_such_admin", "x");
        assertThat(noId.getResponse().getStatus()).isEqualTo(401);
        OpenApiContract.assertValid(noId);
        assertThat(loginResult("no_such_admin")).isEqualTo("FAIL_NO_ID");

        String loginId = newAdmin("VIEWER", false);
        MvcResult wrongPwd = AuthTestSupport.loginRaw(mockMvc, loginId, "wrong");
        assertThat(wrongPwd.getResponse().getStatus()).isEqualTo(401);
        assertThat(wrongPwd.getResponse().getContentAsString())
                .isEqualTo(noId.getResponse().getContentAsString());    // BR-01
        assertThat(loginResult(loginId)).isEqualTo("FAIL_PWD");
    }

    @Test
    void 비밀번호를_5번_틀리면_잠기고_맞는_비밀번호로도_로그인할_수_없다() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        for (int i = 0; i < 5; i++) {
            assertThat(AuthTestSupport.loginRaw(mockMvc, loginId, "wrong").getResponse().getStatus()).isEqualTo(401);
        }
        MvcResult locked = AuthTestSupport.loginRaw(mockMvc, loginId, TEST_PASSWORD);
        assertThat(locked.getResponse().getStatus()).isEqualTo(403);
        assertThat(JsonPath.<String>read(locked.getResponse().getContentAsString(), "$.error.code"))
                .isEqualTo("ACCOUNT_LOCKED");
        OpenApiContract.assertValid(locked);
        assertThat(jdbc.queryForObject("SELECT status_cd FROM tb_admin WHERE login_id = ?", String.class, loginId))
                .isEqualTo("LOCKED");
    }

    @Test
    void 로그인에_성공하면_실패_횟수가_0이_된다() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        AuthTestSupport.loginRaw(mockMvc, loginId, "wrong");
        AuthTestSupport.login(mockMvc, loginId);
        assertThat(jdbc.queryForObject("SELECT login_fail_cnt FROM tb_admin WHERE login_id = ?", Integer.class, loginId))
                .isZero();
    }

    @Test
    void 사용중지_계정은_로그인할_수_없다() throws Exception {
        MvcResult result = AuthTestSupport.loginRaw(mockMvc, "t_disabled", TEST_PASSWORD);
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.error.code"))
                .isEqualTo("ACCOUNT_DISABLED");
        assertThat(loginResult("t_disabled")).isEqualTo("FAIL_DISABLED");
    }

    // ---------------------------------------------------------------- 토큰

    @Test
    void 토큰이_없거나_잘못되면_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 만료된_토큰은_401_TOKEN_EXPIRED() throws Exception {
        String expired = Jwts.builder().subject("1").claim("loginId", "t_super")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(authProperties.jwtSecret().getBytes()))
                .compact();
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void RefreshToken은_한_번_쓰면_교체되고_다시_쓰면_모든_토큰이_폐기된다() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        Login login = AuthTestSupport.login(mockMvc, loginId);

        MvcResult first = mockMvc.perform(post("/api/v1/auth/token/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isOk()).andReturn();
        OpenApiContract.assertValid(first);
        Cookie rotated = first.getResponse().getCookie("refreshToken");
        assertThat(rotated.getValue()).isNotEqualTo(login.refreshCookie().getValue());

        // 이미 쓴 토큰을 다시 쓰면 재사용으로 보고 모두 폐기한다
        mockMvc.perform(post("/api/v1/auth/token/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_FAILED"));
        // 교체받은 새 토큰도 함께 폐기되었다
        mockMvc.perform(post("/api/v1/auth/token/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 다른_곳에서_로그인하면_기존_RefreshToken은_폐기된다() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        Login first = AuthTestSupport.login(mockMvc, loginId);
        AuthTestSupport.login(mockMvc, loginId);    // NF-LG-02
        mockMvc.perform(post("/api/v1/auth/token/refresh").cookie(first.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃하면_RefreshToken이_폐기되고_쿠키가_지워진다() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        Login login = AuthTestSupport.login(mockMvc, loginId);
        MvcResult result = mockMvc.perform(delete("/api/v1/auth/token")
                        .header("Authorization", login.bearer()).cookie(login.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andReturn();
        OpenApiContract.assertValid(result);
        mockMvc.perform(post("/api/v1/auth/token/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 허용되지_않은_Origin의_토큰_요청은_막는다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token").header("Origin", "https://evil.example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"t_system\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- 내 정보

    @Test
    void 내_정보에_메뉴_트리와_권한이_들어_있다() throws Exception {
        Login login = AuthTestSupport.login(mockMvc, "t_member");
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", login.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.superAdmin").value(false))
                .andExpect(jsonPath("$.data.roles[0].roleCd").value("MEMBER_OPERATOR"))
                .andExpect(jsonPath("$.data.permissions.USER.length()").value(5))
                .andExpect(jsonPath("$.data.menus[0].menuCd").value("MEMBER_ROOT"))
                .andReturn();
        OpenApiContract.assertValid(result);
        // 회원운영자는 회원관리, 시스템관리(코드) 폴더만 보인다
        String body = result.getResponse().getContentAsString();
        assertThat(JsonPath.<java.util.List<String>>read(body, "$.data.menus[*].menuCd"))
                .containsExactly("MEMBER_ROOT", "SYSTEM_ROOT");
        assertThat(JsonPath.<java.util.List<String>>read(body, "$.data.menus[1].children[*].menuCd"))
                .containsExactly("CODE");
    }

    @Test
    void 슈퍼관리자는_모든_메뉴가_보이고_권한_목록은_비어_있다() throws Exception {
        Login login = AuthTestSupport.login(mockMvc, "t_super");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", login.bearer()))
                .andExpect(jsonPath("$.data.superAdmin").value(true))
                .andExpect(jsonPath("$.data.permissions").isEmpty())
                .andExpect(jsonPath("$.data.menus.length()").value(4));
    }

    @Test
    void 내_정보를_수정하고_오래된_modDt로_수정하면_CONFLICT_MODIFIED() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        Login login = AuthTestSupport.login(mockMvc, loginId);
        String modDt = JsonPath.read(mockMvc.perform(get("/api/v1/auth/me").header("Authorization", login.bearer()))
                .andReturn().getResponse().getContentAsString(), "$.data.modDt");
        String body = "{\"adminNm\":\"바뀐이름\",\"email\":\"changed@example.com\",\"mobileNo\":\"01012345678\","
                + "\"deptNm\":\"운영팀\",\"modDt\":\"" + modDt + "\"}";

        Thread.sleep(1100);    // mod_dt는 초 단위로 비교한다
        MvcResult ok = mockMvc.perform(put("/api/v1/me").header("Authorization", login.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        OpenApiContract.assertValid(ok);
        assertThat(jdbc.queryForObject("SELECT admin_nm FROM tb_admin WHERE login_id = ?", String.class, loginId))
                .isEqualTo("바뀐이름");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_audit_log l JOIN tb_admin a ON a.admin_id = l.admin_id
                WHERE a.login_id = ? AND l.summary = '내 정보 수정'
                """, Integer.class, loginId)).isEqualTo(1);

        MvcResult conflict = mockMvc.perform(put("/api/v1/me").header("Authorization", login.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT_MODIFIED"))
                .andReturn();
        OpenApiContract.assertValid(conflict);
    }

    // ---------------------------------------------------------------- 임시 비밀번호·비밀번호 변경

    @Test
    void 임시_비밀번호_상태에서는_비밀번호를_바꾸기_전에_다른_API를_쓸_수_없다() throws Exception {
        String loginId = newAdmin("SYSTEM_ADMIN", true);
        Login login = AuthTestSupport.login(mockMvc, loginId);
        assertThat(JsonPath.<Boolean>read(login.result().getResponse().getContentAsString(), "$.data.pwdChangeRequired"))
                .isTrue();

        mockMvc.perform(get("/api/v1/common/codes/USER_STATUS").header("Authorization", login.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_CHANGE_REQUIRED"));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", login.bearer()))
                .andExpect(status().isOk());    // 내 정보 조회는 된다

        MvcResult changed = mockMvc.perform(put("/api/v1/me/password").header("Authorization", login.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TEST_PASSWORD + "\",\"newPassword\":\"new-pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pwdChangeRequired").value(false))
                .andReturn();
        OpenApiContract.assertValid(changed);

        String newToken = JsonPath.read(changed.getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(get("/api/v1/common/codes/USER_STATUS").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
        // 이전 로그인의 Refresh Token은 폐기되었다
        mockMvc.perform(post("/api/v1/auth/token/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호_변경_입력_오류는_칸별로_알려_준다() throws Exception {
        String loginId = newAdmin("VIEWER", false);
        Login login = AuthTestSupport.login(mockMvc, loginId);

        MvcResult wrongCurrent = mockMvc.perform(put("/api/v1/me/password").header("Authorization", login.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-pw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("currentPassword"))
                .andReturn();
        OpenApiContract.assertValid(wrongCurrent);

        mockMvc.perform(put("/api/v1/me/password").header("Authorization", login.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TEST_PASSWORD + "\",\"newPassword\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("newPassword"));
    }
}
