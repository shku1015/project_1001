package egovframework.admin.web.api.probe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.ApiResponse;
import egovframework.admin.web.security.LoginOnly;
import egovframework.admin.web.security.RequirePermission;

/**
 * 테스트 전용: 권한 인터셉터 동작을 확인하는 API. 운영 코드에는 없다 (src/test).
 */
@RestController
@RequestMapping("/api/v1/_probe")
public class PermissionProbeController {

    @GetMapping("/user-privacy")
    @RequirePermission(menu = "USER", action = Action.PRIVACY)
    public ApiResponse<String> userPrivacy() {
        return ApiResponse.ok("ok");
    }

    /** 회원 등록처럼 두 액션(CREATE, PRIVACY)이 모두 있어야 통과 */
    @GetMapping("/user-create")
    @RequirePermission(menu = "USER", action = Action.CREATE, also = Action.PRIVACY)
    public ApiResponse<String> userCreate() {
        return ApiResponse.ok("ok");
    }

    /** 게시글처럼 통합 메뉴 또는 게시판별 메뉴 중 하나의 권한이면 통과 */
    @GetMapping("/boards/{boardCd}/posts")
    @RequirePermission(menu = {"POST", "POST_{boardCd}"}, action = Action.UPDATE)
    public ApiResponse<String> boardPost(@PathVariable String boardCd) {
        return ApiResponse.ok(boardCd);
    }

    @GetMapping("/login-only")
    @LoginOnly
    public ApiResponse<String> loginOnly() {
        return ApiResponse.ok("ok");
    }

    /** 권한 표시가 없는 메서드는 기본 거부 */
    @GetMapping("/no-annotation")
    public ApiResponse<String> noAnnotation() {
        return ApiResponse.ok("should not reach");
    }
}
