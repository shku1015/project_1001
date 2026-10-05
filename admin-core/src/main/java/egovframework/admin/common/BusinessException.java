package egovframework.admin.common;

import java.util.List;

/**
 * 업무 규칙 위반. Service가 던지고, API는 실패 JSON으로, SSR은 화면 메시지로 바꾼다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiResponse.FieldError> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public BusinessException(ErrorCode errorCode, String message, List<ApiResponse.FieldError> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** 특정 입력 칸의 검증 오류 (VALIDATION_ERROR) */
    public static BusinessException field(String field, String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.message(),
                List.of(new ApiResponse.FieldError(field, message)));
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
