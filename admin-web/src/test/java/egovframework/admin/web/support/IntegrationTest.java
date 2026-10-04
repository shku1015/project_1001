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
 * 통합 테스트 공통 설정: 실제 PostgreSQL(Testcontainers) + 운영 필수 초기 데이터 + MockMvc.
 * 같은 설정을 쓰는 테스트는 Spring 컨텍스트와 DB 컨테이너를 함께 쓴다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public @interface IntegrationTest {
}
