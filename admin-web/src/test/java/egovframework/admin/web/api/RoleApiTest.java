package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * 역할관리 API (docs/06-api/08-role.md, docs/04-features/08-role.md BR-01~09). 응답은 api/openapi.yaml과 대조한다.
 * 기존 역할(권한 매트릭스)은 바꾸지 않고 테스트마다 IT_ROLE_* 역할을 만들어 쓴다.
 * t_system(시스템관리자)은 ROLE 전 권한, CODE 전 권한을 갖고 USER 권한은 없다.
 */
@IntegrationTestWithData
class RoleApiTest {

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

    private MvcResult call(String token, MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(req.header("Authorization", token)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        OpenApiContract.assertValid(result);
        return result;
    }

    private MvcResult call(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        return call(system, req, expectedStatus);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String code() {
        return "IT_ROLE_" + SEQ.incrementAndGet() + "_" + (System.nanoTime() % 100000);
    }

    private static <T> T read(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    private long roleId(String roleCd) {
        return jdbc.queryForObject("SELECT role_id FROM tb_role WHERE role_cd = ?", Long.class, roleCd);
    }

    private long menuId(String menuCd) {
        return jdbc.queryForObject("SELECT menu_id FROM tb_menu WHERE menu_cd = ?", Long.class, menuCd);
    }

    private long createRole(String roleCd) throws Exception {
        MvcResult r = call(json(post("/api/v1/roles"), """
                {"roleCd": "%s", "roleNm": "테스트 역할", "description": "설명", "useYn": "Y"}
                """.formatted(roleCd)), 201);
        return ((Number) read(r, "$.data.roleId")).longValue();
    }

    private String modDt(long roleId) throws Exception {
        return read(call(get("/api/v1/roles/" + roleId), 200), "$.data.modDt");
    }

    private MvcResult savePermissions(long roleId, String permissions, int expectedStatus) throws Exception {
        return call(json(put("/api/v1/roles/" + roleId + "/permissions"), """
                {"permissions": [%s], "modDt": "%s"}
                """.formatted(permissions, modDt(roleId))), expectedStatus);
    }

    private List<String> grantsInDb(long roleId) {
        return jdbc.queryForList("""
                SELECT m.menu_cd || '/' || p.action_cd FROM tb_role_permission rp
                JOIN tb_permission p ON p.perm_id = rp.perm_id JOIN tb_menu m ON m.menu_id = p.menu_id
                WHERE rp.role_id = ? ORDER BY 1
                """, String.class, roleId);
    }

    // ------------------------------------------------------------------ 조회

    @Test
    void 목록은_등록순이고_관리자_수와_시스템_역할을_준다() throws Exception {
        MvcResult r = call(get("/api/v1/roles"), 200);
        List<String> codes = read(r, "$.data[*].roleCd");
        assertThat(codes).startsWith("SUPER_ADMIN", "SYSTEM_ADMIN", "MEMBER_OPERATOR", "CONTENT_OPERATOR", "VIEWER");
        assertThat(read(r, "$.data[0].systemYn").toString()).isEqualTo("Y");
        assertThat((Integer) read(r, "$.data[1].adminCnt")).isPositive();

        MvcResult search = call(get("/api/v1/roles?keyword={k}", "조회전용"), 200);
        assertThat(read(search, "$.data[*].roleCd").toString()).isEqualTo("[\"VIEWER\"]");
    }

    @Test
    void 역할_권한이_없으면_403() throws Exception {
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        assertThat(mockMvc.perform(get("/api/v1/roles").header("Authorization", viewer)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void 내_역할과_시스템_역할은_수정할_수_없다() throws Exception {
        // t_system은 SYSTEM_ADMIN 역할을 가진다 (BR-04)
        long mine = roleId("SYSTEM_ADMIN");
        MvcResult detail = call(get("/api/v1/roles/" + mine), 200);
        assertThat((Boolean) read(detail, "$.data.mine")).isTrue();
        assertThat((Boolean) read(detail, "$.data.editable")).isFalse();
        MvcResult r = call(json(put("/api/v1/roles/" + mine), """
                {"roleNm": "바꿈", "description": null, "useYn": "Y", "modDt": "%s"}
                """.formatted(modDt(mine))), 409);
        assertThat(read(r, "$.error.code").toString()).isEqualTo("ROLE_NOT_EDITABLE");
        assertThat(read(savePermissions(mine, "", 409), "$.error.code").toString()).isEqualTo("ROLE_NOT_EDITABLE");

        // 시스템 역할 (R2)
        long superRole = roleId("SUPER_ADMIN");
        assertThat((Boolean) read(call(get("/api/v1/roles/" + superRole), 200), "$.data.editable")).isFalse();
        assertThat(read(call(delete("/api/v1/roles/" + superRole), 409), "$.error.code").toString())
                .isEqualTo("ROLE_SYSTEM_PROTECTED");
        // 관리자에게 부여된 역할은 삭제할 수 없다 (R6)
        assertThat(read(call(delete("/api/v1/roles/" + roleId("VIEWER")), 409), "$.error.code").toString())
                .isEqualTo("ROLE_IN_USE");
    }

    // ------------------------------------------------------------------ 등록·수정·삭제

    @Test
    void 역할을_등록_수정_삭제한다() throws Exception {
        String roleCd = code();
        assertThat((Boolean) read(call(get("/api/v1/roles/check-role-cd?roleCd={cd}", roleCd), 200),
                "$.data.available")).isTrue();
        long id = createRole(roleCd);

        MvcResult detail = call(get("/api/v1/roles/" + id), 200);
        assertThat((Boolean) read(detail, "$.data.editable")).isTrue();
        assertThat(read(detail, "$.data.regNm").toString()).isEqualTo("테스트시스템");

        // 중복·형식 오류 (BR-01)
        assertThat(read(call(json(post("/api/v1/roles"), """
                {"roleCd": "%s", "roleNm": "중복", "useYn": "Y"}""".formatted(roleCd)), 409), "$.error.code")
                .toString()).isEqualTo("DUPLICATE");
        // 명세를 어기는 요청이므로 계약 검사 없이 상태만 본다
        assertThat(mockMvc.perform(json(post("/api/v1/roles"), """
                {"roleCd": "lower", "roleNm": "형식", "useYn": "Y"}""").header("Authorization", system)).andReturn()
                .getResponse().getStatus()).isEqualTo(400);

        call(json(put("/api/v1/roles/" + id), """
                {"roleNm": "바뀐 이름", "description": null, "useYn": "N", "modDt": "%s"}
                """.formatted(modDt(id))), 200);
        assertThat(jdbc.queryForObject("SELECT role_nm || '/' || use_yn FROM tb_role WHERE role_id = ?",
                String.class, id)).isEqualTo("바뀐 이름/N");

        // 오래된 수정일시면 동시 수정 오류
        assertThat(read(call(json(put("/api/v1/roles/" + id), """
                {"roleNm": "다시", "useYn": "Y", "modDt": "2000-01-01T00:00:00"}"""), 409), "$.error.code")
                .toString()).isEqualTo("CONFLICT_MODIFIED");

        call(delete("/api/v1/roles/" + id), 200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_role WHERE role_id = ?", Integer.class, id)).isZero();
    }

    // ------------------------------------------------------------------ 권한 설정

    @Test
    void 권한을_설정하면_READ가_함께_부여되고_추가_제거_목록을_준다() throws Exception {
        long id = createRole(code());
        long codeMenu = menuId("CODE");

        MvcResult r = savePermissions(id, """
                {"menuId": %d, "actions": ["UPDATE"]}""".formatted(codeMenu), 200);
        assertThat(read(r, "$.data.added[*].action").toString()).isEqualTo("[\"READ\",\"UPDATE\"]");
        assertThat(grantsInDb(id)).containsExactly("CODE/READ", "CODE/UPDATE");

        // 권한 탭: 부여된 액션과 내가 줄 수 있는 액션
        MvcResult tree = call(get("/api/v1/roles/" + id + "/permissions"), 200);
        assertThat(read(tree, "$..[?(@.menuCd == 'CODE')].granted").toString())
                .isEqualTo("[[\"READ\",\"UPDATE\"]]");
        assertThat(read(tree, "$..[?(@.menuCd == 'USER')].grantableActions").toString()).isEqualTo("[[]]");

        // 목록에 없는 메뉴는 모두 회수
        MvcResult cleared = savePermissions(id, "", 200);
        assertThat(read(cleared, "$.data.removed[*].action").toString()).isEqualTo("[\"READ\",\"UPDATE\"]");
        assertThat(grantsInDb(id)).isEmpty();
    }

    @Test
    void 내가_갖지_않은_권한이나_메뉴에_없는_액션은_줄_수_없다() throws Exception {
        long id = createRole(code());
        // t_system은 USER 권한이 없다 (BR-05)
        MvcResult escalation = savePermissions(id, """
                {"menuId": %d, "actions": ["READ"]}""".formatted(menuId("USER")), 409);
        assertThat(read(escalation, "$.error.code").toString()).isEqualTo("PRIVILEGE_ESCALATION");
        // 코드관리는 EXCEL 액션을 쓰지 않는다
        savePermissions(id, """
                {"menuId": %d, "actions": ["EXCEL"]}""".formatted(menuId("CODE")), 400);
        assertThat(grantsInDb(id)).isEmpty();
    }

    @Test
    void 역할을_복사하면_권한이_같은_새_역할이_생긴다() throws Exception {
        long source = createRole(code());
        savePermissions(source, """
                {"menuId": %d, "actions": ["READ", "CREATE"]}""".formatted(menuId("CODE")), 200);

        String copyCd = code();
        MvcResult r = call(json(post("/api/v1/roles/" + source + "/copy"), """
                {"roleCd": "%s", "roleNm": "복사본"}""".formatted(copyCd)), 201);
        long copy = ((Number) read(r, "$.data.roleId")).longValue();
        assertThat(grantsInDb(copy)).isEqualTo(grantsInDb(source));
        assertThat(jdbc.queryForObject("SELECT description || '/' || use_yn FROM tb_role WHERE role_id = ?",
                String.class, copy)).isEqualTo("설명/Y");

        // 내가 갖지 않은 권한이 든 역할(회원운영자: USER)은 복사할 수 없다 (R5)
        assertThat(read(call(json(post("/api/v1/roles/" + roleId("MEMBER_OPERATOR") + "/copy"), """
                {"roleCd": "%s", "roleNm": "복사"}""".formatted(code())), 409), "$.error.code").toString())
                .isEqualTo("PRIVILEGE_ESCALATION");
    }

    @Test
    void 권한과_사용_여부_변경은_그_역할을_가진_관리자의_다음_요청부터_반영된다() throws Exception {
        String roleCd = code();
        long id = createRole(roleCd);
        String loginId = AuthTestSupport.createAdmin(jdbc, encoder, "it_role", roleCd, false);
        String token = AuthTestSupport.login(mockMvc, loginId).bearer();
        assertThat(mockMvc.perform(get("/api/v1/code-groups").header("Authorization", token)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);

        savePermissions(id, """
                {"menuId": %d, "actions": ["READ"]}""".formatted(menuId("CODE")), 200);
        assertThat(mockMvc.perform(get("/api/v1/code-groups").header("Authorization", token)).andReturn()
                .getResponse().getStatus()).isEqualTo(200);

        // 사용 안 함 역할의 권한은 최종 권한에서 빠진다 (BR-07)
        call(json(put("/api/v1/roles/" + id), """
                {"roleNm": "테스트 역할", "useYn": "N", "modDt": "%s"}""".formatted(modDt(id))), 200);
        assertThat(mockMvc.perform(get("/api/v1/code-groups").header("Authorization", token)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);

        // 관리자 탭
        MvcResult admins = call(get("/api/v1/roles/" + id + "/admins"), 200);
        assertThat(read(admins, "$.data[*].loginId").toString()).isEqualTo("[\"" + loginId + "\"]");
    }
}
