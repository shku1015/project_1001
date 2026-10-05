package egovframework.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import egovframework.admin.auth.InitialAdminService;
import egovframework.admin.web.support.IntegrationTestWithData;

/**
 * 최초 관리자 admin 생성 (docs/03-initial-data.md 2절).
 */
@IntegrationTestWithData
class InitialAdminTest {

    @Autowired
    private InitialAdminService initialAdminService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 없으면_임시_비밀번호_슈퍼관리자로_만들고_있으면_건너뛴다() {
        jdbc.update("DELETE FROM tb_admin_role WHERE admin_id IN (SELECT admin_id FROM tb_admin WHERE login_id = 'admin')");
        jdbc.update("DELETE FROM tb_admin_login_hist WHERE login_id = 'admin'");
        jdbc.update("DELETE FROM tb_admin WHERE login_id = 'admin'");

        assertThat(initialAdminService.createIfAbsent("initial-pw")).isTrue();
        assertThat(initialAdminService.createIfAbsent("other-pw")).isFalse();

        assertThat(jdbc.queryForObject("SELECT pwd_temp_yn FROM tb_admin WHERE login_id = 'admin'", String.class))
                .isEqualTo("Y");
        assertThat(jdbc.queryForObject("""
                SELECT r.role_cd FROM tb_admin a JOIN tb_admin_role ar USING (admin_id) JOIN tb_role r USING (role_id)
                WHERE a.login_id = 'admin'
                """, String.class)).isEqualTo("SUPER_ADMIN");
    }
}
