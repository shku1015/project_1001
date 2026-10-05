package egovframework.admin.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 임시 비밀번호 상태에서도 쓸 수 있는 기능 (비밀번호 변경, 로그아웃, 내 정보 조회).
 * 나머지 기능은 API는 403 PASSWORD_CHANGE_REQUIRED, SSR은 비밀번호 변경 화면으로 보낸다 (AUTH-03).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowTempPassword {
}
