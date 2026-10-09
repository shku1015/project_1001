package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;
import egovframework.admin.web.support.OpenApiContract;

/**
 * 관리자관리 API (docs/06-api/06-admin.md, docs/04-features/06-admin.md BR-01~10). 응답은 api/openapi.yaml과 대조한다.
 * 데이터를 바꾸는 시나리오는 t_* 대신 AuthTestSupport.createAdmin으로 만든 관리자를 대상으로 한다.
 * t_system(시스템관리자)은 ADMIN 전 권한을 갖는다.
 */
@IntegrationTestWithData
class AdminApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    private String system;

    @BeforeEach
    void login() throws Exception {
        system = AuthTestSupport.login(mockMvc, "t_system").bearer();
    }

    // ------------------------------------------------------------------ 도우미

    private MvcResult call(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(req.header("Authorization", system)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        OpenApiContract.assertValid(result);
        return result;
    }

    /** 명세를 어기는 요청: 계약 검사 없이 상태만 본다 */
    private int status(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", system)).andReturn().getResponse().getStatus();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static <T> T read(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    private static String error(MvcResult r) throws Exception {
        return read(r, "$.error.code");
    }

    private long adminId(String loginId) {
        return jdbc.queryForObject("SELECT admin_id FROM tb_admin WHERE login_id = ?", Long.class, loginId);
    }

    private long roleId(String roleCd) {
        return jdbc.queryForObject("SELECT role_id FROM tb_role WHERE role_cd = ?", Long.class, roleCd);
    }

    /** 데이터를 바꿀 대상 관리자 (VIEWER 역할) */
    private long target() {
        return adminId(AuthTestSupport.createAdmin(jdbc, encoder, "it_adm", "VIEWER", false));
    }

    private String modDt(long adminId) throws Exception {
        return read(call(get("/api/v1/admins/" + adminId), 200), "$.data.modDt");
    }

    private String statusOf(long adminId) {
        return jdbc.queryForObject("SELECT status_cd FROM tb_admin WHERE admin_id = ?", String.class, adminId);
    }

    // ------------------------------------------------------------------ 조회

    @Test
    void 목록은_페이징_정렬_검색을_한다() throws Exception {
        MvcResult r = call(get("/api/v1/admins?size=10&sort=loginId,asc"), 200);
        assertThat((Integer) read(r, "$.data.page")).isEqualTo(1);
        assertThat((Integer) read(r, "$.data.size")).isEqualTo(10);
        assertThat(((Number) read(r, "$.data.totalCount")).longValue()).isGreaterThanOrEqualTo(9);
        List<String> ids = read(r, "$.data.items[*].loginId");
        assertThat(ids).hasSizeLessThanOrEqualTo(10).isSorted();

        MvcResult byRole = call(get("/api/v1/admins?roleId=" + roleId("MEMBER_OPERATOR")), 200);
        assertThat(read(byRole, "$.data.items[*].loginId").toString()).contains("t_member", "t_multi")
                .doesNotContain("t_viewer");
        MvcResult multi = call(get("/api/v1/admins?loginId=t_multi"), 200);
        assertThat(read(multi, "$.data.items[0].roles[*].roleNm").toString()).isEqualTo("[\"회원운영자\",\"콘텐츠운영자\"]");
        MvcResult keyword = call(get("/api/v1/admins?keyword={k}", "테스트슈퍼"), 200);
        assertThat(read(keyword, "$.data.items[*].loginId").toString()).isEqualTo("[\"t_super\"]");
        MvcResult locked = call(get("/api/v1/admins?statusCd=LOCKED"), 200);
        assertThat(read(locked, "$.data.items[*].statusNm").toString()).doesNotContain("사용중지");

        // 정렬할 수 없는 항목, 허용하지 않는 페이지 크기
        assertThat(status(get("/api/v1/admins?sort=email,asc"))).isEqualTo(400);
        assertThat(status(get("/api/v1/admins?size=15"))).isEqualTo(400);
    }

    @Test
    void 상세는_본인_여부와_부여된_역할을_준다() throws Exception {
        MvcResult self = call(get("/api/v1/admins/" + adminId("t_system")), 200);
        assertThat((Boolean) read(self, "$.data.self")).isTrue();
        assertThat(read(self, "$.data.roles[*].roleCd").toString()).isEqualTo("[\"SYSTEM_ADMIN\"]");
        assertThat((Boolean) read(call(get("/api/v1/admins/" + adminId("t_member")), 200), "$.data.self")).isFalse();

        // t_system은 로그인했으므로 이력이 있다
        MvcResult hist = call(get("/api/v1/admins/" + adminId("t_system") + "/login-histories"), 200);
        assertThat(read(hist, "$.data[0].resultCd").toString()).isEqualTo("SUCCESS");
        call(get("/api/v1/admins/999999"), 404);
    }

    @Test
    void 역할_선택_목록은_내_권한_범위_안의_사용_중인_역할만_부여_가능하다() throws Exception {
        MvcResult r = call(get("/api/v1/admins/role-options"), 200);
        assertThat(read(r, "$.data[?(@.roleCd == 'SYSTEM_ADMIN')].assignable").toString()).isEqualTo("[true]");
        // 슈퍼관리자 역할, 내가 갖지 않은 권한(USER)이 든 역할은 줄 수 없다 (R5)
        assertThat(read(r, "$.data[?(@.roleCd == 'SUPER_ADMIN')].assignable").toString()).isEqualTo("[false]");
        assertThat(read(r, "$.data[?(@.roleCd == 'MEMBER_OPERATOR')].assignable").toString()).isEqualTo("[false]");
    }

    @Test
    void 관리자_권한이_없으면_403() throws Exception {
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        assertThat(mockMvc.perform(get("/api/v1/admins").header("Authorization", viewer)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 등록·수정

    @Test
    void 등록하면_임시_비밀번호를_한_번만_준다() throws Exception {
        String loginId = "itnew" + SEQ.incrementAndGet() + System.nanoTime() % 100000;
        assertThat((Boolean) read(call(get("/api/v1/admins/check-login-id?loginId=" + loginId), 200),
                "$.data.available")).isTrue();
        MvcResult r = call(json(post("/api/v1/admins"), """
                {"loginId": "%s", "adminNm": "신규", "email": "%s@example.com", "mobileNo": "01012345678",
                 "deptNm": "운영팀", "roleIds": [%d]}""".formatted(loginId, loginId, roleId("SYSTEM_ADMIN"))), 201);
        String temp = read(r, "$.data.tempPassword");
        long id = ((Number) read(r, "$.data.adminId")).longValue();
        assertThat(encoder.matches(temp, jdbc.queryForObject("SELECT password FROM tb_admin WHERE admin_id = ?",
                String.class, id))).isTrue();
        assertThat(jdbc.queryForObject("SELECT pwd_temp_yn FROM tb_admin WHERE admin_id = ?", String.class, id))
                .isEqualTo("Y");
        // 임시 비밀번호로 로그인하면 비밀번호를 바꿔야 한다 (BR-03)
        MvcResult login = AuthTestSupport.loginRaw(mockMvc, loginId, temp);
        assertThat(login.getResponse().getStatus()).isEqualTo(200);

        // 중복 아이디 (BR-01), 내 권한을 넘는 역할 (R5)
        assertThat(error(call(json(post("/api/v1/admins"), """
                {"loginId": "%s", "adminNm": "중복", "email": "d@example.com", "roleIds": [%d]}"""
                .formatted(loginId, roleId("SYSTEM_ADMIN"))), 409))).isEqualTo("DUPLICATE");
        assertThat(error(call(json(post("/api/v1/admins"), """
                {"loginId": "itesc%d", "adminNm": "상향", "email": "e@example.com", "roleIds": [%d]}"""
                .formatted(SEQ.incrementAndGet(), roleId("MEMBER_OPERATOR"))), 409))).isEqualTo("PRIVILEGE_ESCALATION");
        // 역할 0개
        assertThat(status(json(post("/api/v1/admins"), """
                {"loginId": "itnorole", "adminNm": "역할없음", "email": "n@example.com", "roleIds": []}"""))).isEqualTo(400);
    }

    @Test
    void 정보를_수정하고_오래된_수정일시면_거부한다() throws Exception {
        long id = target();
        call(json(put("/api/v1/admins/" + id), """
                {"adminNm": "바뀐이름", "email": "changed@example.com", "mobileNo": null, "deptNm": "새부서", "modDt": "%s"}
                """.formatted(modDt(id))), 200);
        assertThat(jdbc.queryForObject("SELECT admin_nm || '/' || dept_nm FROM tb_admin WHERE admin_id = ?",
                String.class, id)).isEqualTo("바뀐이름/새부서");
        assertThat(error(call(json(put("/api/v1/admins/" + id), """
                {"adminNm": "다시", "email": "x@example.com", "modDt": "2000-01-01T00:00:00"}"""), 409)))
                .isEqualTo("CONFLICT_MODIFIED");
        assertThat(status(json(put("/api/v1/admins/" + id), """
                {"adminNm": "형식", "email": "not-email", "modDt": "%s"}""".formatted(modDt(id))))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ 역할 부여·회수

    @Test
    void 역할을_부여하고_회수한다() throws Exception {
        long id = target();
        call(json(post("/api/v1/admins/" + id + "/roles"), "{\"roleId\": %d}".formatted(roleId("SYSTEM_ADMIN"))), 200);
        assertThat(error(call(json(post("/api/v1/admins/" + id + "/roles"),
                "{\"roleId\": %d}".formatted(roleId("SYSTEM_ADMIN"))), 409))).isEqualTo("DUPLICATE");
        call(delete("/api/v1/admins/" + id + "/roles/" + roleId("VIEWER")), 200);
        // 마지막 역할은 회수할 수 없다 (BR-04)
        assertThat(error(call(delete("/api/v1/admins/" + id + "/roles/" + roleId("SYSTEM_ADMIN")), 409)))
                .isEqualTo("ROLE_REQUIRED");

        // 본인 역할 (R4)
        long me = adminId("t_system");
        assertThat(error(call(json(post("/api/v1/admins/" + me + "/roles"),
                "{\"roleId\": %d}".formatted(roleId("VIEWER"))), 409))).isEqualTo("SELF_ROLE_CHANGE");
        // 사용 안 함 역할
        jdbc.update("INSERT INTO tb_role (role_cd, role_nm, use_yn) VALUES ('IT_ADM_UNUSED', '안씀', 'N') ON CONFLICT DO NOTHING");
        assertThat(error(call(json(post("/api/v1/admins/" + id + "/roles"),
                "{\"roleId\": %d}".formatted(roleId("IT_ADM_UNUSED"))), 409))).isEqualTo("ROLE_NOT_USABLE");
    }

    // ------------------------------------------------------------------ 상태

    @Test
    void 잠금을_해제하면_실패_횟수가_0이_된다() throws Exception {
        long id = target();
        jdbc.update("UPDATE tb_admin SET status_cd = 'LOCKED', login_fail_cnt = 5 WHERE admin_id = ?", id);
        call(post("/api/v1/admins/" + id + "/unlock"), 200);
        assertThat(jdbc.queryForObject("SELECT status_cd || '/' || login_fail_cnt FROM tb_admin WHERE admin_id = ?",
                String.class, id)).isEqualTo("ACTIVE/0");
        assertThat(error(call(post("/api/v1/admins/" + id + "/unlock"), 409))).isEqualTo("INVALID_STATUS_CHANGE");
    }

    @Test
    void 비밀번호를_초기화하면_예전_비밀번호와_로그인이_끊긴다() throws Exception {
        long id = target();
        String loginId = jdbc.queryForObject("SELECT login_id FROM tb_admin WHERE admin_id = ?", String.class, id);
        AuthTestSupport.login(mockMvc, loginId);
        MvcResult r = call(post("/api/v1/admins/" + id + "/password-reset"), 200);
        String temp = read(r, "$.data.tempPassword");
        assertThat(AuthTestSupport.loginRaw(mockMvc, loginId, AuthTestSupport.TEST_PASSWORD).getResponse().getStatus())
                .isEqualTo(401);
        assertThat(AuthTestSupport.loginRaw(mockMvc, loginId, temp).getResponse().getStatus()).isEqualTo(200);
        // 예전 로그인의 Refresh Token은 모두 폐기됐다 (BR-09). 새 로그인 1건만 남는다
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_admin_refresh_token WHERE admin_id = ? AND revoked_yn = 'N'""",
                Integer.class, id)).isEqualTo(1);
    }

    @Test
    void 사용중지하고_다시_사용한다() throws Exception {
        long id = target();
        call(json(patch("/api/v1/admins/" + id + "/status"), """
                {"statusCd": "DISABLED", "modDt": "%s"}""".formatted(modDt(id))), 200);
        assertThat(statusOf(id)).isEqualTo("DISABLED");
        // 사용중지 상태는 비밀번호 초기화 불가
        assertThat(error(call(post("/api/v1/admins/" + id + "/password-reset"), 409))).isEqualTo("INVALID_STATUS_CHANGE");
        call(json(patch("/api/v1/admins/" + id + "/status"), """
                {"statusCd": "ACTIVE", "modDt": "%s"}""".formatted(modDt(id))), 200);
        assertThat(statusOf(id)).isEqualTo("ACTIVE");
        // 사용 중에 재사용은 허용하지 않는다
        assertThat(error(call(json(patch("/api/v1/admins/" + id + "/status"), """
                {"statusCd": "ACTIVE", "modDt": "%s"}""".formatted(modDt(id))), 409))).isEqualTo("INVALID_STATUS_CHANGE");

        // 본인 사용중지 불가 (BR-05)
        long me = adminId("t_system");
        assertThat(error(call(json(patch("/api/v1/admins/" + me + "/status"), """
                {"statusCd": "DISABLED", "modDt": "%s"}""".formatted(modDt(me))), 409))).isEqualTo("SELF_DISABLE");
    }

    @Test
    void 마지막_슈퍼관리자는_사용중지할_수_없다() throws Exception {
        long superId = adminId("t_super");
        // t_super 말고 사용 중인 슈퍼관리자가 있으면 잠시 사용중지했다가 되돌린다
        List<Long> others = jdbc.queryForList("""
                SELECT a.admin_id FROM tb_admin a JOIN tb_admin_role ar ON ar.admin_id = a.admin_id
                JOIN tb_role r ON r.role_id = ar.role_id
                WHERE r.role_cd = 'SUPER_ADMIN' AND a.status_cd = 'ACTIVE' AND a.admin_id <> ?""", Long.class, superId);
        others.forEach(o -> jdbc.update("UPDATE tb_admin SET status_cd = 'DISABLED' WHERE admin_id = ?", o));
        try {
            assertThat(error(call(json(patch("/api/v1/admins/" + superId + "/status"), """
                    {"statusCd": "DISABLED", "modDt": "%s"}""".formatted(modDt(superId))), 409)))
                    .isEqualTo("LAST_SUPER_ADMIN");
            assertThat(statusOf(superId)).isEqualTo("ACTIVE");
        } finally {
            others.forEach(o -> jdbc.update("UPDATE tb_admin SET status_cd = 'ACTIVE' WHERE admin_id = ?", o));
        }
    }
}
