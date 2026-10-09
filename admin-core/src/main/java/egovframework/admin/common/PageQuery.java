package egovframework.admin.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 목록 페이징·정렬 요청 (docs/06-api-spec.md 5절). page는 1부터, size는 10·20·50.
 * 정렬은 "필드,방향"이고 화면 명세에서 정렬할 수 있다고 한 필드만 받는다. 다른 필드는 INVALID_REQUEST.
 * orderBy는 허용 목록의 칼럼으로만 만들므로 MyBatis에서 ${}로 써도 안전하다.
 */
public record PageQuery(int page, int size, String orderBy) {

    private static final Set<Integer> SIZES = Set.of(10, 20, 50);

    /**
     * @param sortable 정렬할 수 있는 필드 → SQL 칼럼
     * @param defaultOrderBy 정렬 조건이 없을 때의 ORDER BY 절 (칼럼 그대로)
     * @param tieBreaker 같은 값끼리의 순서를 고정할 칼럼 (예: a.admin_id DESC). 항상 뒤에 붙인다
     */
    public static PageQuery of(Integer page, Integer size, List<String> sort, Map<String, String> sortable,
                               String defaultOrderBy, String tieBreaker) {
        int p = page == null ? 1 : page;
        int s = size == null ? 20 : size;
        if (p < 1 || !SIZES.contains(s)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "페이지 번호나 크기가 올바르지 않습니다.");
        }
        List<String> orders = new ArrayList<>();
        if (sort != null) {
            for (String item : sort) {
                String[] parts = item.split(",");
                String column = parts.length == 2 ? sortable.get(parts[0]) : null;
                String direction = parts.length == 2 ? parts[1].toLowerCase() : "";
                if (column == null || !(direction.equals("asc") || direction.equals("desc"))) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "정렬할 수 없는 항목입니다: " + item);
                }
                orders.add(column + " " + direction.toUpperCase() + " NULLS LAST");
            }
        }
        if (orders.isEmpty()) {
            orders.add(defaultOrderBy);
        }
        orders.add(tieBreaker);
        return new PageQuery(p, s, String.join(", ", orders));
    }

    public int offset() {
        return (page - 1) * size;
    }
}
