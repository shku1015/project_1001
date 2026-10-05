package egovframework.admin.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import egovframework.admin.auth.AuthTypes.Action;

/**
 * 컨트롤러 메서드에 필요한 권한 (docs/02-access-model.md 4절).
 * 메뉴를 여러 개 주면 그중 하나라도 권한이 있으면 통과한다.
 * 메뉴 코드에 {경로변수}를 쓸 수 있다. 예: {"POST", "POST_{boardCd}"}
 *
 * 컨트롤러(api, ssr 패키지)의 모든 메서드는 @RequirePermission, @LoginOnly, @PublicEndpoint 중 하나가 있어야 한다.
 * 없으면 기본 거부(403)한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    String[] menu();

    Action action();
}
