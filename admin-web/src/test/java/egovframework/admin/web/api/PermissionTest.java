package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;

/**
 * 권한 (docs/02-access-model.md).
 * 1. 권한 매트릭스: 테스트 관리자별 최종 권한이 docs/data/permission-matrix.csv의 역할 합집합과 같은지 모든 메뉴·액션 조합을 확인
 * 2. 인터셉터: 기본 거부, 로그인만, 여러 메뉴 중 하나, 슈퍼관리자 예외
 */
@IntegrationTestWithData
class PermissionTest {

    private static final Path MATRIX = Path.of("..", "docs", "data", "permission-matrix.csv");

    /** 테스트 관리자 → 역할 (docs/03-initial-data.md 3.1) */
    private static final Map<String, List<String>> TEST_ADMINS = Map.of(
            "t_system", List.of("SYSTEM_ADMIN"),
            "t_member", List.of("MEMBER_OPERATOR"),
            "t_content", List.of("CONTENT_OPERATOR"),
            "t_viewer", List.of("VIEWER"),
            "t_multi", List.of("MEMBER_OPERATOR", "CONTENT_OPERATOR"),
            "t_norole", List.of());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AdminAuthInfoService authInfoService;

    @Test
    void 관리자별_최종_권한이_권한_매트릭스의_역할_합집합과_같다() throws IOException {
        Map<String, Set<String>> grantsByRole = readMatrix();
        List<String> pageMenus = jdbc.queryForList("SELECT menu_cd FROM tb_menu WHERE menu_type_cd = 'PAGE'", String.class);
        List<String> mismatches = new ArrayList<>();

        for (Map.Entry<String, List<String>> admin : TEST_ADMINS.entrySet()) {
            Set<String> expected = new HashSet<>();
            admin.getValue().forEach(role -> expected.addAll(grantsByRole.getOrDefault(role, Set.of())));
            AdminAuthInfo auth = authInfoService.load(adminId(admin.getKey()));

            for (String menuCd : pageMenus) {
                for (Action action : Action.values()) {
                    boolean shouldHave = expected.contains(menuCd + "/" + action);
                    if (auth.has(menuCd, action) != shouldHave) {
                        mismatches.add(admin.getKey() + " " + menuCd + "/" + action + " 기대=" + shouldHave);
                    }
                }
            }
        }
        assertThat(mismatches).isEmpty();
    }

    @Test
    void 슈퍼관리자는_모든_권한을_가진다() {
        AdminAuthInfo auth = authInfoService.load(adminId("t_super"));
        assertThat(auth.superAdmin()).isTrue();
        assertThat(auth.has("ANY_MENU", Action.DELETE)).isTrue();
    }

    @Test
    void 권한이_있으면_통과하고_없으면_403_FORBIDDEN() throws Exception {
        String member = AuthTestSupport.login(mockMvc, "t_member").bearer();
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        mockMvc.perform(get("/api/v1/_probe/user-privacy").header("Authorization", member))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/_probe/user-privacy").header("Authorization", viewer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void 권한_표시가_없는_API는_슈퍼관리자도_기본_거부() throws Exception {
        String superAdmin = AuthTestSupport.login(mockMvc, "t_super").bearer();
        mockMvc.perform(get("/api/v1/_probe/no-annotation").header("Authorization", superAdmin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/_probe/login-only").header("Authorization", superAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void 역할이_없어도_로그인만_필요한_API는_쓸_수_있다() throws Exception {
        String noRole = AuthTestSupport.login(mockMvc, "t_norole").bearer();
        mockMvc.perform(get("/api/v1/_probe/login-only").header("Authorization", noRole))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/_probe/user-privacy").header("Authorization", noRole))
                .andExpect(status().isForbidden());
    }

    @Test
    void 함께_필요한_액션이_하나라도_없으면_403() throws Exception {
        // 회원 등록은 CREATE와 PRIVACY가 모두 필요하다 (02-user BR-05). CREATE만 가진 역할을 만든다
        jdbc.update("INSERT INTO tb_role (role_cd, role_nm) VALUES ('IT_USER_CREATE_ONLY', '회원등록만') ON CONFLICT DO NOTHING");
        jdbc.update("""
                INSERT INTO tb_role_permission (role_id, perm_id)
                SELECT r.role_id, p.perm_id FROM tb_role r, tb_permission p JOIN tb_menu m USING (menu_id)
                WHERE r.role_cd = 'IT_USER_CREATE_ONLY' AND m.menu_cd = 'USER' AND p.action_cd IN ('READ', 'CREATE')
                ON CONFLICT DO NOTHING
                """);
        String createOnly = AuthTestSupport.login(mockMvc,
                AuthTestSupport.createAdmin(jdbc, encoder, "it_perm", "IT_USER_CREATE_ONLY", false)).bearer();
        mockMvc.perform(get("/api/v1/_probe/user-create").header("Authorization", createOnly))
                .andExpect(status().isForbidden());
        // 회원운영자는 USER의 CREATE·PRIVACY를 모두 가진다
        String member = AuthTestSupport.login(mockMvc, "t_member").bearer();
        mockMvc.perform(get("/api/v1/_probe/user-create").header("Authorization", member))
                .andExpect(status().isOk());
    }

    @Test
    void 게시판별_메뉴_권한만_있으면_그_게시판만_통과한다() throws Exception {
        // 공지사항 관리(POST_NOTICE) 수정 권한만 가진 역할을 만들어 부여한다 (ADR-0010)
        jdbc.update("INSERT INTO tb_role (role_cd, role_nm) VALUES ('IT_NOTICE_ONLY', '공지만') ON CONFLICT DO NOTHING");
        jdbc.update("""
                INSERT INTO tb_role_permission (role_id, perm_id)
                SELECT r.role_id, p.perm_id FROM tb_role r, tb_permission p JOIN tb_menu m USING (menu_id)
                WHERE r.role_cd = 'IT_NOTICE_ONLY' AND m.menu_cd = 'POST_NOTICE' AND p.action_cd IN ('READ', 'UPDATE')
                ON CONFLICT DO NOTHING
                """);
        String loginId = AuthTestSupport.createAdmin(jdbc, encoder, "it_perm", "IT_NOTICE_ONLY", false);
        String token = AuthTestSupport.login(mockMvc, loginId).bearer();

        mockMvc.perform(get("/api/v1/_probe/boards/NOTICE/posts").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/_probe/boards/QNA/posts").header("Authorization", token))
                .andExpect(status().isForbidden());
        // 통합 메뉴(POST) 권한이 있으면 모든 게시판 통과
        String content = AuthTestSupport.login(mockMvc, "t_content").bearer();
        mockMvc.perform(get("/api/v1/_probe/boards/QNA/posts").header("Authorization", content))
                .andExpect(status().isOk());
    }

    private long adminId(String loginId) {
        return jdbc.queryForObject("SELECT admin_id FROM tb_admin WHERE login_id = ?", Long.class, loginId);
    }

    private static Map<String, Set<String>> readMatrix() throws IOException {
        Map<String, Set<String>> grants = new java.util.HashMap<>();
        List<String> lines = Files.readAllLines(MATRIX);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split(",");
            for (String action : cols[2].trim().split(" ")) {
                grants.computeIfAbsent(cols[0], k -> new HashSet<>()).add(cols[1] + "/" + action);
            }
        }
        return grants;
    }
}
