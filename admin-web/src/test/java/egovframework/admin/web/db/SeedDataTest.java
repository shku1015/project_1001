package egovframework.admin.web.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import egovframework.admin.web.support.IntegrationTest;

/**
 * 운영 필수 초기 데이터(V3__seed.sql)가 명세와 맞는지 확인한다.
 */
@IntegrationTest
class SeedDataTest {

    /** docs/02-access-model.md 6절 권한 매트릭스의 기계 판독용 사본 */
    private static final Path MATRIX = Path.of("..", "docs", "data", "permission-matrix.csv");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 역할별_권한이_권한_매트릭스와_같다() throws IOException {
        Set<String> expected = new TreeSet<>();
        List<String> lines = Files.readAllLines(MATRIX);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split(",");
            Arrays.stream(cols[2].trim().split(" "))
                    .forEach(action -> expected.add(cols[0] + "/" + cols[1] + "/" + action));
        }

        Set<String> actual = new TreeSet<>(jdbc.queryForList("""
                SELECT r.role_cd || '/' || m.menu_cd || '/' || p.action_cd
                FROM tb_role_permission rp
                JOIN tb_role r ON r.role_id = rp.role_id
                JOIN tb_permission p ON p.perm_id = rp.perm_id
                JOIN tb_menu m ON m.menu_id = p.menu_id
                """, String.class));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void 슈퍼관리자는_권한_매핑이_없는_시스템_역할이다() {
        assertThat(jdbc.queryForObject("SELECT system_yn FROM tb_role WHERE role_cd = 'SUPER_ADMIN'", String.class))
                .isEqualTo("Y");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_role_permission rp JOIN tb_role r ON r.role_id = rp.role_id
                WHERE r.role_cd = 'SUPER_ADMIN'
                """, Integer.class)).isZero();
    }

    @Test
    void 메뉴_트리는_화면명세의_구성과_같다() {
        // docs/05-ia-screens.md 1절: 폴더 5개(최상위 4 + 게시판별 게시글), 화면 11개
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_menu WHERE menu_type_cd = 'FOLDER'", Integer.class))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_menu WHERE menu_type_cd = 'PAGE'", Integer.class))
                .isEqualTo(11);
        // 시스템 메뉴 (docs/04-features/03-menu.md BR-10)
        assertThat(jdbc.queryForList("SELECT menu_cd FROM tb_menu WHERE system_yn = 'Y' ORDER BY menu_cd", String.class))
                .containsExactly("ADMIN", "ADMIN_ROOT", "AUDIT_LOG", "MASKING", "MENU", "PERMISSION", "POST_BY_BOARD",
                        "ROLE", "SYSTEM_ROOT");
    }

    @Test
    void 화면_메뉴는_모두_READ_권한을_가진다() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_menu m
                WHERE m.menu_type_cd = 'PAGE'
                  AND NOT EXISTS (SELECT 1 FROM tb_permission p WHERE p.menu_id = m.menu_id AND p.action_cd = 'READ')
                """, Integer.class)).isZero();
    }

    @Test
    void 개인정보_마스킹_기본값은_모두_마스킹이다() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_masking_policy WHERE screen_mask_yn = 'Y' AND excel_mask_yn = 'Y'
                """, Integer.class)).isEqualTo(4);
    }

    @Test
    void 테스트_데이터는_기본_프로필에_들어가지_않는다() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_admin", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_user", Integer.class)).isZero();
    }
}
