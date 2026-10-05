package egovframework.admin.web.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * 인증 테스트 도우미: MockMvc 로그인, 테스트 전용 관리자 생성.
 */
public final class AuthTestSupport {

    /** 테스트 데이터(t_*)의 비밀번호. 테스트에서만 쓰는 값이다 */
    public static final String TEST_PASSWORD = "test-only-password";

    private static final AtomicInteger SEQ = new AtomicInteger();

    private AuthTestSupport() {
    }

    public record Login(String accessToken, Cookie refreshCookie, MvcResult result) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    public static MvcResult loginRaw(MockMvc mockMvc, String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    /** 로그인해서 Access Token과 Refresh Token 쿠키를 받는다 (성공해야 한다) */
    public static Login login(MockMvc mockMvc, String loginId, String password) throws Exception {
        MvcResult result = loginRaw(mockMvc, loginId, password);
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("로그인 실패: " + loginId + " " + result.getResponse().getContentAsString());
        }
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
        return new Login(token, result.getResponse().getCookie("refreshToken"), result);
    }

    public static Login login(MockMvc mockMvc, String loginId) throws Exception {
        return login(mockMvc, loginId, TEST_PASSWORD);
    }

    /**
     * 테스트 전용 관리자를 만든다. 이름이 겹치지 않도록 접두어 뒤에 번호를 붙인다.
     * @param roleCd 역할 코드. null이면 역할 없음
     * @return 만든 로그인 아이디
     */
    public static String createAdmin(JdbcTemplate jdbc, PasswordEncoder encoder, String prefix, String roleCd,
                                     boolean pwdTemp) {
        String loginId = prefix + "_" + SEQ.incrementAndGet() + "_" + System.nanoTime() % 100000;
        jdbc.update("""
                INSERT INTO tb_admin (login_id, password, admin_nm, email, status_cd, pwd_temp_yn, pwd_changed_dt)
                VALUES (?, ?, '테스트전용', ? , 'ACTIVE', ?, now())
                """, loginId, encoder.encode(TEST_PASSWORD), loginId + "@example.com", pwdTemp ? "Y" : "N");
        if (roleCd != null) {
            jdbc.update("""
                    INSERT INTO tb_admin_role (admin_id, role_id)
                    SELECT a.admin_id, r.role_id FROM tb_admin a, tb_role r WHERE a.login_id = ? AND r.role_cd = ?
                    """, loginId, roleCd);
        }
        return loginId;
    }
}
