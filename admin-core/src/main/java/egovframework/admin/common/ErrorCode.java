package egovframework.admin.common;

/**
 * 오류 코드 목록 (docs/06-api-spec.md 6절, docs/06-api/*.md "업무 오류 코드").
 * api/openapi.yaml의 ErrorCode enum과 같아야 한다 (ErrorCodeSpecTest가 확인).
 * API는 JSON 오류 응답으로, JSP SSR은 화면 메시지로 같은 코드를 쓴다 (ADR-0003).
 */
public enum ErrorCode {

    // 공통
    VALIDATION_ERROR(400, "입력값을 확인하세요."),
    INVALID_REQUEST(400, "잘못된 요청입니다."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    TOKEN_EXPIRED(401, "로그인이 만료되었습니다."),
    REFRESH_FAILED(401, "다시 로그인하세요."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(403, "권한이 없습니다."),
    PASSWORD_CHANGE_REQUIRED(403, "비밀번호를 변경해야 합니다."),
    ACCOUNT_LOCKED(403, "잠긴 계정입니다. 관리자에게 잠금 해제를 요청하세요."),
    ACCOUNT_DISABLED(403, "사용이 중지된 계정입니다."),
    NOT_FOUND(404, "대상을 찾을 수 없습니다."),
    DUPLICATE(409, "이미 사용 중인 값입니다."),
    CONFLICT_MODIFIED(409, "다른 관리자가 먼저 수정했습니다. 다시 조회하세요."),
    EXCEL_LIMIT_EXCEEDED(409, "엑셀은 한 번에 10,000건까지 내려받을 수 있습니다. 검색 조건을 좁히세요."),
    FILE_TOO_LARGE(413, "파일 크기가 허용 범위를 넘었습니다."),
    INTERNAL_ERROR(500, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요."),

    // 상태 변경 공통
    INVALID_STATUS_CHANGE(409, "바꿀 수 없는 상태입니다."),
    REASON_REQUIRED(409, "사유를 입력하세요."),

    // 기업·회원
    COMPANY_HAS_MEMBERS(409, "소속 회원이 있어 삭제할 수 없습니다."),
    COMPANY_NOT_ACTIVE(409, "정상 상태인 기업만 고를 수 있습니다."),
    COMPANY_REQUIRED(409, "기업 회원은 소속 기업이 필요합니다."),
    USER_WITHDRAWN(409, "탈퇴한 회원은 수정하거나 상태를 바꿀 수 없습니다."),

    // 메뉴
    MENU_DEPTH_EXCEEDED(409, "메뉴는 3단계까지만 만들 수 있습니다."),
    MENU_PARENT_NOT_FOLDER(409, "폴더 메뉴 아래에만 하위 메뉴를 둘 수 있습니다."),
    MENU_HAS_CHILDREN(409, "하위 메뉴가 있어 삭제할 수 없습니다."),
    MENU_SYSTEM_PROTECTED(409, "시스템 메뉴는 삭제하거나 사용 안 함으로 바꿀 수 없습니다."),
    MENU_BOARD_MANAGED(409, "게시판별 메뉴는 게시판 관리에서 바꿉니다."),
    MENU_ORDER_MISMATCH(409, "메뉴 구성이 바뀌었습니다. 트리를 다시 불러오세요."),
    MENU_NOT_PAGE(409, "화면 메뉴만 권한을 가집니다."),

    // 코드
    CODE_SYSTEM_PROTECTED(409, "시스템 코드는 바꿀 수 없습니다."),
    CODE_GROUP_HAS_CODES(409, "상세코드가 있어 삭제할 수 없습니다."),

    // 게시판
    BOARD_MENU_EXISTS(409, "이미 관리 메뉴가 있습니다."),
    BOARD_MENU_NOT_FOUND(409, "관리 메뉴가 없습니다."),
    REPLY_NOT_ALLOWED(409, "답글을 쓸 수 없는 게시판입니다."),
    COMMENT_NOT_ALLOWED(409, "댓글을 쓸 수 없는 게시판입니다."),
    COMMENT_DEPTH_EXCEEDED(409, "대댓글에는 댓글을 달 수 없습니다."),
    ATTACH_NOT_ALLOWED(409, "첨부할 수 없는 게시판입니다."),
    ATTACH_COUNT_EXCEEDED(409, "첨부 개수를 넘었습니다."),
    FILE_TYPE_NOT_ALLOWED(409, "허용되지 않은 파일 형식입니다."),

    // 관리자·역할·권한
    ROLE_REQUIRED(409, "역할이 하나 이상 필요합니다."),
    SELF_ROLE_CHANGE(409, "본인 역할은 바꿀 수 없습니다."),
    SELF_DISABLE(409, "본인 계정은 사용중지할 수 없습니다."),
    PRIVILEGE_ESCALATION(409, "본인이 갖지 않은 권한은 부여할 수 없습니다."),
    LAST_SUPER_ADMIN(409, "마지막 슈퍼관리자는 사용중지하거나 역할을 회수할 수 없습니다."),
    ROLE_NOT_USABLE(409, "사용 안 함 역할은 부여할 수 없습니다."),
    ROLE_NOT_EDITABLE(409, "시스템 역할이나 본인 역할은 바꿀 수 없습니다."),
    ROLE_IN_USE(409, "관리자에게 부여된 역할은 삭제할 수 없습니다."),
    ROLE_SYSTEM_PROTECTED(409, "시스템 역할은 삭제할 수 없습니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }
}
