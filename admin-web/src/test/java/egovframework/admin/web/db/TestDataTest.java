package egovframework.admin.web.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import egovframework.admin.web.TestcontainersConfig;

/**
 * 개발용 테스트 데이터(db/testdata/R__testdata.sql)가 오류 없이 들어가고, docs/03-initial-data.md 3절과 맞는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/testdata",
        "spring.flyway.placeholders.testAdminPassword=" + TestDataTest.PASSWORD
})
@Import(TestcontainersConfig.class)
class TestDataTest {

    static final String PASSWORD = "test-only-password";

    @Autowired
    private JdbcTemplate jdbc;

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void 테스트_관리자와_역할() {
        assertThat(count("SELECT count(*) FROM tb_admin")).isEqualTo(9);
        assertThat(count("SELECT count(*) FROM tb_admin_role ar JOIN tb_admin a USING (admin_id) WHERE a.login_id = 't_multi'"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM tb_admin_role ar JOIN tb_admin a USING (admin_id) WHERE a.login_id = 't_norole'"))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status_cd FROM tb_admin WHERE login_id = 't_locked'", String.class))
                .isEqualTo("LOCKED");
    }

    @Test
    void 테스트_관리자_비밀번호는_BCrypt로_저장된다() {
        String hash = jdbc.queryForObject("SELECT password FROM tb_admin WHERE login_id = 't_super'", String.class);
        assertThat(hash).startsWith("$2a$10$");
        assertThat(jdbc.queryForObject("SELECT password = crypt(?, password) FROM tb_admin WHERE login_id = 't_super'",
                Boolean.class, PASSWORD)).isTrue();
    }

    @Test
    void 기업과_회원() {
        assertThat(count("SELECT count(*) FROM tb_company")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM tb_user WHERE login_id NOT LIKE 'bulk_%'")).isEqualTo(20);
        assertThat(count("SELECT count(*) FROM tb_user WHERE login_id LIKE 'bulk_%'")).isEqualTo(100);
        // 소속 회원 수 (삭제 회원 제외, 탈퇴 회원 포함)
        assertThat(count("""
                SELECT count(*) FROM tb_user u JOIN tb_company c USING (company_id)
                WHERE c.biz_reg_no = '1000000001' AND u.del_yn = 'N'
                """)).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM tb_user_status_hist")).isEqualTo(2);
    }

    @Test
    void 게시판과_게시판별_관리_메뉴() {
        assertThat(count("SELECT count(*) FROM tb_board_master")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM tb_board_master WHERE menu_id IS NOT NULL")).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM tb_menu WHERE depth = 3")).isEqualTo(4);
        assertThat(count("""
                SELECT count(*) FROM tb_permission p JOIN tb_menu m USING (menu_id) WHERE m.menu_cd = 'POST_NOTICE'
                """)).isEqualTo(4);
    }

    @Test
    void QnA_답글_구조() {
        // 원글 5, 답글·재답글 7, 최대 깊이 3
        assertThat(count("""
                SELECT count(*) FROM tb_post p JOIN tb_board_master b USING (board_id)
                WHERE b.board_cd = 'QNA' AND p.depth = 0
                """)).isEqualTo(5);
        assertThat(count("""
                SELECT count(*) FROM tb_post p JOIN tb_board_master b USING (board_id)
                WHERE b.board_cd = 'QNA' AND p.depth > 0
                """)).isEqualTo(7);
        assertThat(count("SELECT max(depth) FROM tb_post")).isEqualTo(3);
        // 모든 글의 root_post_id가 채워져 있다
        assertThat(count("SELECT count(*) FROM tb_post WHERE root_post_id IS NULL")).isZero();
    }
}
