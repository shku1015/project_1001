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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;
import egovframework.admin.web.support.OpenApiContract;

/**
 * 메뉴관리 API (docs/06-api/03-menu.md, docs/04-features/03-menu.md BR-01~12). 응답은 api/openapi.yaml과 대조한다.
 * 다른 테스트(권한 매트릭스 등)에 영향을 주지 않도록 기존 역할에는 권한을 주지 않고 전용 역할을 쓰며, 3단계 메뉴는 만들지 않는다.
 */
@IntegrationTestWithData
class MenuApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String system;    // MENU 전 권한
    private String viewer;    // MENU 권한 없음

    @BeforeEach
    void login() throws Exception {
        system = AuthTestSupport.login(mockMvc, "t_system").bearer();
        viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
    }

    // ------------------------------------------------------------------ 도우미

    private MvcResult call(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(req.header("Authorization", system)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        OpenApiContract.assertValid(result);
        return result;
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String code() {
        return "IT_MENU_" + SEQ.incrementAndGet() + "_" + (System.nanoTime() % 100000);
    }

    private long menuId(String menuCd) {
        return jdbc.queryForObject("SELECT menu_id FROM tb_menu WHERE menu_cd = ?", Long.class, menuCd);
    }

    private String errorCode(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.error.code");
    }

    /** 회원관리(MEMBER_ROOT) 아래에 화면 메뉴를 만든다 */
    private long createPage(String menuCd, String actions) throws Exception {
        MvcResult r = call(json(post("/api/v1/menus"), """
                {"parentMenuId": %d, "menuCd": "%s", "menuNm": "테스트 화면", "menuTypeCd": "PAGE",
                 "menuUrl": "/it-test", "actions": [%s], "icon": null, "useYn": "Y"}
                """.formatted(menuId("MEMBER_ROOT"), menuCd, actions)), 201);
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.data.menuId")).longValue();
    }

    private String modDt(long menuId) throws Exception {
        MvcResult r = call(get("/api/v1/menus/" + menuId), 200);
        return JsonPath.read(r.getResponse().getContentAsString(), "$.data.modDt");
    }

    private List<String> actionsInDb(long menuId) {
        return jdbc.queryForList("SELECT action_cd FROM tb_permission WHERE menu_id = ? ORDER BY action_cd",
                String.class, menuId);
    }

    // ------------------------------------------------------------------ 조회

    @Test
    void 트리와_상세를_준다() throws Exception {
        MvcResult tree = call(get("/api/v1/menus/tree"), 200);
        String body = tree.getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$.data[*].menuCd"))
                .containsExactly("MEMBER_ROOT", "BOARD_ROOT", "SYSTEM_ROOT", "ADMIN_ROOT");
        // 게시판별 자동 메뉴는 boardAutoYn=Y
        assertThat(JsonPath.<List<String>>read(body, "$..children[?(@.menuCd == 'POST_NOTICE')].boardAutoYn"))
                .containsExactly("Y");

        MvcResult detail = call(get("/api/v1/menus/" + menuId("CODE")), 200);
        assertThat(JsonPath.<List<String>>read(detail.getResponse().getContentAsString(), "$.data.actions"))
                .containsExactly("READ", "CREATE", "UPDATE", "DELETE");
    }

    @Test
    void 메뉴_권한이_없으면_403() throws Exception {
        assertThat(mockMvc.perform(get("/api/v1/menus/tree").header("Authorization", viewer)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 등록

    @Test
    void 화면_메뉴를_등록하면_맨_뒤_순서가_되고_READ를_포함한_권한이_생긴다() throws Exception {
        String menuCd = code();
        long id = createPage(menuCd, "\"UPDATE\"");
        assertThat(actionsInDb(id)).containsExactly("READ", "UPDATE");    // BR-05: READ 자동 포함
        Integer sort = jdbc.queryForObject("SELECT sort_ord FROM tb_menu WHERE menu_id = ?", Integer.class, id);
        Integer max = jdbc.queryForObject("SELECT max(sort_ord) FROM tb_menu WHERE parent_menu_id = ?", Integer.class,
                menuId("MEMBER_ROOT"));
        assertThat(sort).isEqualTo(max);
        // BR-06: 기존 역할에는 부여하지 않는다
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_role_permission rp JOIN tb_permission p USING (perm_id) WHERE p.menu_id = ?
                """, Integer.class, id)).isZero();

        MvcResult check = call(get("/api/v1/menus/check-menu-cd").queryParam("menuCd", menuCd), 200);
        assertThat(JsonPath.<Boolean>read(check.getResponse().getContentAsString(), "$.data.available")).isFalse();
    }

    @Test
    void 메뉴_코드가_겹치면_DUPLICATE() throws Exception {
        MvcResult r = call(json(post("/api/v1/menus"), """
                {"parentMenuId": null, "menuCd": "CODE", "menuNm": "중복", "menuTypeCd": "FOLDER", "menuUrl": null,
                 "actions": [], "icon": null, "useYn": "Y"}
                """), 409);
        assertThat(errorCode(r)).isEqualTo("DUPLICATE");
    }

    @Test
    void 화면_메뉴_아래에는_만들_수_없고_3단계에는_폴더를_둘_수_없다() throws Exception {
        MvcResult underPage = call(json(post("/api/v1/menus"), """
                {"parentMenuId": %d, "menuCd": "%s", "menuNm": "x", "menuTypeCd": "PAGE", "menuUrl": "/x",
                 "actions": ["READ"], "icon": null, "useYn": "Y"}
                """.formatted(menuId("CODE"), code())), 409);
        assertThat(errorCode(underPage)).isEqualTo("MENU_PARENT_NOT_FOLDER");    // BR-02

        MvcResult folderAtDepth3 = call(json(post("/api/v1/menus"), """
                {"parentMenuId": %d, "menuCd": "%s", "menuNm": "x", "menuTypeCd": "FOLDER", "menuUrl": null,
                 "actions": [], "icon": null, "useYn": "Y"}
                """.formatted(menuId("POST_BY_BOARD"), code())), 409);
        assertThat(errorCode(folderAtDepth3)).isEqualTo("MENU_DEPTH_EXCEEDED");    // BR-01
    }

    // ------------------------------------------------------------------ 수정

    @Test
    void 액션을_빼면_역할_매핑이_회수되고_dryRun은_미리_보여만_준다() throws Exception {
        long id = createPage(code(), "\"UPDATE\", \"DELETE\"");
        jdbc.update("INSERT INTO tb_role (role_cd, role_nm) VALUES ('IT_MENU_ROLE', '메뉴테스트역할') ON CONFLICT DO NOTHING");
        jdbc.update("""
                INSERT INTO tb_role_permission (role_id, perm_id)
                SELECT r.role_id, p.perm_id FROM tb_role r, tb_permission p
                WHERE r.role_cd = 'IT_MENU_ROLE' AND p.menu_id = ? AND p.action_cd = 'DELETE'
                """, id);
        String body = """
                {"menuNm": "테스트 화면", "menuUrl": "/it-test", "actions": ["READ", "UPDATE"], "icon": null,
                 "useYn": "Y", "modDt": "%s"}
                """.formatted(modDt(id));

        MvcResult dry = call(json(put("/api/v1/menus/" + id).queryParam("dryRun", "Y"), body), 200);
        assertThat(JsonPath.<List<String>>read(dry.getResponse().getContentAsString(), "$.data.revokedRoles[*].roleNm"))
                .containsExactly("메뉴테스트역할");
        assertThat(actionsInDb(id)).contains("DELETE");    // dryRun은 저장하지 않는다

        MvcResult saved = call(json(put("/api/v1/menus/" + id), body), 200);
        assertThat(JsonPath.<List<String>>read(saved.getResponse().getContentAsString(), "$.data.revokedRoles[0].actions"))
                .containsExactly("DELETE");
        assertThat(actionsInDb(id)).containsExactly("READ", "UPDATE");    // BR-07
    }

    @Test
    void 시스템_메뉴는_이름만_바꿀_수_있고_사용_안_함으로_바꿀_수_없다() throws Exception {
        long id = menuId("MENU");
        MvcResult r = call(json(put("/api/v1/menus/" + id), """
                {"menuNm": "메뉴관리", "menuUrl": "/menus", "actions": ["READ", "CREATE", "UPDATE", "DELETE"],
                 "icon": null, "useYn": "N", "modDt": "%s"}
                """.formatted(modDt(id))), 409);
        assertThat(errorCode(r)).isEqualTo("MENU_SYSTEM_PROTECTED");    // BR-10

        call(json(put("/api/v1/menus/" + id), """
                {"menuNm": "메뉴관리", "menuUrl": "/menus", "actions": ["READ", "CREATE", "UPDATE", "DELETE"],
                 "icon": null, "useYn": "Y", "modDt": "%s"}
                """.formatted(modDt(id))), 200);
    }

    @Test
    void 오래된_modDt로_수정하면_CONFLICT_MODIFIED() throws Exception {
        long id = createPage(code(), "");
        MvcResult r = call(json(put("/api/v1/menus/" + id), """
                {"menuNm": "바뀜", "menuUrl": "/it-test", "actions": ["READ"], "icon": null, "useYn": "Y",
                 "modDt": "2000-01-01T00:00:00"}
                """), 409);
        assertThat(errorCode(r)).isEqualTo("CONFLICT_MODIFIED");
    }

    // ------------------------------------------------------------------ 상위 메뉴 변경

    @Test
    void 다른_폴더로_옮기면_맨_뒤_순서가_되고_화면_메뉴_아래로는_못_옮긴다() throws Exception {
        long id = createPage(code(), "");
        long systemRoot = menuId("SYSTEM_ROOT");
        call(json(patch("/api/v1/menus/" + id + "/parent"), """
                {"parentMenuId": %d, "modDt": "%s"}""".formatted(systemRoot, modDt(id))), 200);
        assertThat(jdbc.queryForObject("SELECT parent_menu_id FROM tb_menu WHERE menu_id = ?", Long.class, id))
                .isEqualTo(systemRoot);

        MvcResult r = call(json(patch("/api/v1/menus/" + id + "/parent"), """
                {"parentMenuId": %d, "modDt": "%s"}""".formatted(menuId("CODE"), modDt(id))), 409);
        assertThat(errorCode(r)).isEqualTo("MENU_PARENT_NOT_FOLDER");
    }

    @Test
    void 시스템_메뉴와_게시판_자동_메뉴는_옮길_수_없다() throws Exception {
        long menu = menuId("MENU");
        MvcResult r1 = call(json(patch("/api/v1/menus/" + menu + "/parent"), """
                {"parentMenuId": %d, "modDt": "%s"}""".formatted(menuId("MEMBER_ROOT"), modDt(menu))), 409);
        assertThat(errorCode(r1)).isEqualTo("MENU_SYSTEM_PROTECTED");

        long notice = menuId("POST_NOTICE");
        MvcResult r2 = call(json(patch("/api/v1/menus/" + notice + "/parent"), """
                {"parentMenuId": %d, "modDt": "%s"}""".formatted(menuId("MEMBER_ROOT"), modDt(notice))), 409);
        assertThat(errorCode(r2)).isEqualTo("MENU_BOARD_MANAGED");    // BR-12
    }

    // ------------------------------------------------------------------ 순서 저장

    @Test
    void 순서를_저장하고_하위_메뉴가_빠지면_MENU_ORDER_MISMATCH() throws Exception {
        long parent = menuId("SYSTEM_ROOT");
        List<Long> children = jdbc.queryForList(
                "SELECT menu_id FROM tb_menu WHERE parent_menu_id = ? ORDER BY sort_ord", Long.class, parent);
        List<Long> reversed = new java.util.ArrayList<>(children);
        java.util.Collections.reverse(reversed);

        call(json(put("/api/v1/menus/order"), """
                {"orders": [{"parentMenuId": %d, "menuIds": %s}]}""".formatted(parent, reversed)), 200);
        assertThat(jdbc.queryForList("SELECT menu_id FROM tb_menu WHERE parent_menu_id = ? ORDER BY sort_ord",
                Long.class, parent)).isEqualTo(reversed);

        MvcResult mismatch = call(json(put("/api/v1/menus/order"), """
                {"orders": [{"parentMenuId": %d, "menuIds": %s}]}""".formatted(parent, children.subList(1, children.size()))), 409);
        assertThat(errorCode(mismatch)).isEqualTo("MENU_ORDER_MISMATCH");

        // 원래 순서로 되돌린다
        call(json(put("/api/v1/menus/order"), """
                {"orders": [{"parentMenuId": %d, "menuIds": %s}]}""".formatted(parent, children)), 200);
    }

    // ------------------------------------------------------------------ 삭제

    @Test
    void 삭제하면_권한까지_지우고_시스템_메뉴와_하위가_있는_메뉴는_지울_수_없다() throws Exception {
        long id = createPage(code(), "\"UPDATE\"");
        call(delete("/api/v1/menus/" + id), 200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_menu WHERE menu_id = ?", Integer.class, id)).isZero();
        assertThat(actionsInDb(id)).isEmpty();

        assertThat(errorCode(call(delete("/api/v1/menus/" + menuId("MENU")), 409))).isEqualTo("MENU_SYSTEM_PROTECTED");
        assertThat(errorCode(call(delete("/api/v1/menus/" + menuId("MEMBER_ROOT")), 409))).isEqualTo("MENU_HAS_CHILDREN");
        assertThat(errorCode(call(delete("/api/v1/menus/" + menuId("POST_NOTICE")), 409))).isEqualTo("MENU_BOARD_MANAGED");
    }
}
