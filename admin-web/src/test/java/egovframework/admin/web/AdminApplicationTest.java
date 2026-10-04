package egovframework.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 골격 동작 확인: 실제 PostgreSQL에서 앱이 뜨고, Flyway·JSP·헬스 체크가 동작하는지.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class AdminApplicationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCreatesPgTrgmExtension() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'pg_trgm'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void healthIsUp() {
        ResponseEntity<String> res = rest.getForEntity("/actuator/health", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void rootPageRendersJsp() {
        ResponseEntity<String> res = rest.getForEntity("/", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("관리자 서비스", "/react/", "/jsp/login", "/ssr/login");
    }
}
