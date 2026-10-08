package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;
import egovframework.admin.web.support.OpenApiContract;

/**
 * 권한관리 API (docs/06-api/07-permission.md, docs/04-features/07-permission.md BR-01~06). 응답은 api/openapi.yaml과 대조한다.
 * 기존 역할(권한 매트릭스)은 바꾸지 않고 테스트마다 IT_PRM_* 역할을 만들어 쓴다.
 * t_system(시스템관리자)은 PERMISSION READ·UPDATE, CODE 전 권한을 갖고 USER 권한은 없다.
 */
@IntegrationTestWithData
class PermissionApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

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

    private static <T> T read(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    private long id(String table, String codeColumn, String code) {
        String idColumn = table.substring(3) + "_id";
        return jdbc.queryForObject("SELECT " + idColumn + " FROM " + table + " WHERE " + codeColumn + " = ?",
                Long.class, code);
    }

    private long menuId(String menuCd) {
        return id("tb_menu", "menu_cd", menuCd);
    }

    private long roleId(String roleCd) {
        return id("tb_role", "role_cd", roleCd);
    }

    private long createRole() {
        String roleCd = "IT_PRM_" + SEQ.incrementAndGet() + "_" + (System.nanoTime() % 100000);
        jdbc.update("INSERT INTO tb_role (role_cd, role_nm) VALUES (?, '권한테스트역할')", roleCd);
        return roleId(roleCd);
    }

    private MvcResult save(String menuCd, String roles, int expectedStatus) throws Exception {
        return call(put("/api/v1/permissions/menus/" + menuId(menuCd)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\": [" + roles + "]}"), expectedStatus);
    }

    private List<String> actionsInDb(long roleId, String menuCd) {
        return jdbc.queryForList("""
                SELECT p.action_cd FROM tb_role_permission rp JOIN tb_permission p ON p.perm_id = rp.perm_id
                JOIN tb_menu m ON m.menu_id = p.menu_id WHERE rp.role_id = ? AND m.menu_cd = ? ORDER BY p.perm_id
                """, String.class, roleId, menuCd);
    }

    // ------------------------------------------------------------------ 조회

    @Test
    void 메뉴_트리에_사용_액션을_준다() throws Exception {
        MvcResult r = call(get("/api/v1/permissions"), 200);
        assertThat(read(r, "$..[?(@.menuCd == 'CODE')].actions").toString())
                .isEqualTo("[[\"READ\",\"CREATE\",\"UPDATE\",\"DELETE\"]]");
        assertThat(read(r, "$..[?(@.menuCd == 'SYSTEM_ROOT')].actions").toString()).isEqualTo("[[]]");
    }

    @Test
    void 메뉴의_역할별_부여_현황을_준다() throws Exception {
        MvcResult r = call(get("/api/v1/permissions/menus/" + menuId("CODE")), 200);
        // 슈퍼관리자는 사용 액션 전체, 바꿀 수 없다
        assertThat(read(r, "$.data.roles[?(@.roleCd == 'SUPER_ADMIN')].granted").toString())
                .isEqualTo("[[\"READ\",\"CREATE\",\"UPDATE\",\"DELETE\"]]");
        assertThat(read(r, "$.data.roles[?(@.roleCd == 'SUPER_ADMIN')].editable").toString()).isEqualTo("[false]");
        // 내 역할 (BR-04)
        assertThat(read(r, "$.data.roles[?(@.roleCd == 'SYSTEM_ADMIN')].mine").toString()).isEqualTo("[true]");
        assertThat(read(r, "$.data.roles[?(@.roleCd == 'SYSTEM_ADMIN')].editable").toString()).isEqualTo("[false]");
        assertThat(read(r, "$.data.roles[?(@.roleCd == 'MEMBER_OPERATOR')].granted").toString())
                .isEqualTo("[[\"READ\"]]");
        assertThat(read(r, "$.data.grantableActions").toString())
                .isEqualTo("[\"READ\",\"CREATE\",\"UPDATE\",\"DELETE\"]");

        // 내가 갖지 않은 권한의 메뉴는 줄 수 있는 액션이 없다 (BR-05)
        MvcResult user = call(get("/api/v1/permissions/menus/" + menuId("USER")), 200);
        assertThat(read(user, "$.data.grantableActions").toString()).isEqualTo("[]");

        // 폴더 메뉴는 권한이 없다
        assertThat(read(call(get("/api/v1/permissions/menus/" + menuId("SYSTEM_ROOT")), 409), "$.error.code")
                .toString()).isEqualTo("MENU_NOT_PAGE");
    }

    @Test
    void 권한관리_권한이_없으면_403() throws Exception {
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        assertThat(mockMvc.perform(get("/api/v1/permissions").header("Authorization", viewer)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 부여·회수

    @Test
    void 메뉴_기준으로_부여하면_READ가_함께_부여되고_빈_목록이면_모두_회수한다() throws Exception {
        long role = createRole();
        MvcResult r = save("CODE", "{\"roleId\": %d, \"actions\": [\"UPDATE\"]}".formatted(role), 200);
        assertThat(read(r, "$.data.changes[0].added").toString()).isEqualTo("[\"READ\",\"UPDATE\"]");
        assertThat(actionsInDb(role, "CODE")).containsExactly("READ", "UPDATE");

        // 바뀌지 않은 역할은 결과에 없다
        assertThat(read(save("CODE", "{\"roleId\": %d, \"actions\": [\"READ\", \"UPDATE\"]}".formatted(role), 200),
                "$.data.changes").toString()).isEqualTo("[]");

        MvcResult cleared = save("CODE", "{\"roleId\": %d, \"actions\": []}".formatted(role), 200);
        assertThat(read(cleared, "$.data.changes[0].removed").toString()).isEqualTo("[\"READ\",\"UPDATE\"]");
        assertThat(actionsInDb(role, "CODE")).isEmpty();
    }

    @Test
    void 시스템_역할_내_역할_내가_갖지_않은_권한은_바꿀_수_없다() throws Exception {
        assertThat(read(save("CODE", "{\"roleId\": %d, \"actions\": []}".formatted(roleId("SUPER_ADMIN")), 409),
                "$.error.code").toString()).isEqualTo("ROLE_NOT_EDITABLE");
        assertThat(read(save("CODE", "{\"roleId\": %d, \"actions\": []}".formatted(roleId("SYSTEM_ADMIN")), 409),
                "$.error.code").toString()).isEqualTo("ROLE_NOT_EDITABLE");

        long role = createRole();
        assertThat(read(save("USER", "{\"roleId\": %d, \"actions\": [\"READ\"]}".formatted(role), 409),
                "$.error.code").toString()).isEqualTo("PRIVILEGE_ESCALATION");
        // 하나라도 어기면 아무것도 바꾸지 않는다
        long other = createRole();
        save("CODE", "{\"roleId\": %d, \"actions\": [\"READ\"]}, {\"roleId\": %d, \"actions\": []}"
                .formatted(other, roleId("SYSTEM_ADMIN")), 409);
        assertThat(actionsInDb(other, "CODE")).isEmpty();
        // 메뉴에서 쓰지 않는 액션
        save("CODE", "{\"roleId\": %d, \"actions\": [\"EXCEL\"]}".formatted(role), 400);
    }

    // ------------------------------------------------------------------ 관리자별 최종 권한

    @Test
    void 관리자별_최종_권한은_권한을_준_역할_이름과_함께_준다() throws Exception {
        long member = id("tb_admin", "login_id", "t_member");
        MvcResult r = call(get("/api/v1/permissions/admins/" + member), 200);
        assertThat((Boolean) read(r, "$.data.superAdmin")).isFalse();
        assertThat(read(r, "$.data.admin.loginId").toString()).isEqualTo("t_member");
        assertThat(read(r, "$.data.menus[?(@.menuNm == '사용자관리')].granted.PRIVACY").toString())
                .isEqualTo("[[\"회원운영자\"]]");
        // 없는 액션은 키가 없다
        assertThat(read(r, "$.data.menus[?(@.menuNm == '사용자관리')].granted").toString()).doesNotContain("DELETE");

        long superAdmin = id("tb_admin", "login_id", "t_super");
        MvcResult s = call(get("/api/v1/permissions/admins/" + superAdmin), 200);
        assertThat((Boolean) read(s, "$.data.superAdmin")).isTrue();
        assertThat(read(s, "$.data.menus").toString()).isEqualTo("[]");

        call(get("/api/v1/permissions/admins/999999"), 404);
    }
}
