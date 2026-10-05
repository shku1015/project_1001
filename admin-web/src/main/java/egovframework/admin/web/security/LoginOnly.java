package egovframework.admin.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메뉴 권한 없이 로그인만 필요한 기능 (내 정보, 공통 코드 조회 등).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginOnly {
}
