package egovframework.admin.common;

import java.util.List;

/**
 * 공통 응답 틀 { success, data, error } (docs/06-api-spec.md 4절).
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(ErrorCode code, String message, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, null, new ErrorBody(code.name(), message, fieldErrors));
    }

    public static ApiResponse<Void> fail(ErrorCode code) {
        return fail(code, code.message(), List.of());
    }

    public record ErrorBody(String code, String message, List<FieldError> fieldErrors) {
    }

    public record FieldError(String field, String message) {
    }
}
