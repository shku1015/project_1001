package egovframework.admin.web.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import egovframework.admin.web.TestcontainersConfig;

/**
 * 통합 테스트 + 개발용 테스트 데이터(db/testdata): 테스트 관리자 t_* 9명, 기업, 회원, 게시판 등.
 * 테스트 관리자 비밀번호는 {@link AuthTestSupport#TEST_PASSWORD}.
 * 데이터를 바꾸는 테스트(잠금, 비밀번호 변경 등)는 t_* 계정을 쓰지 말고 AuthTestSupport.createAdmin으로 만든 계정을 쓴다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/testdata",
        "spring.flyway.placeholders.testAdminPassword=" + AuthTestSupport.TEST_PASSWORD
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public @interface IntegrationTestWithData {
}
