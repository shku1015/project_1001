package egovframework.admin.common;

import java.util.List;

/**
 * 목록 응답의 data (docs/06-api-spec.md 4절): items와 페이지 정보.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalCount, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, PageQuery query, long totalCount) {
        return new PageResult<>(items, query.page(), query.size(), totalCount,
                (int) ((totalCount + query.size() - 1) / query.size()));
    }
}
