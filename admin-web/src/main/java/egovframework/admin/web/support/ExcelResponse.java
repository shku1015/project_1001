package egovframework.admin.web.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import egovframework.admin.common.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 엑셀 응답 (docs/06-api-spec.md 8절). 파일명은 "{메뉴명}_{yyyyMMddHHmm}.xlsx"이고 UTF-8로 인코딩한다.
 * 헤더는 파일을 쓰기 직전에 붙인다. 그 전에 업무 오류가 나면 공통 오류 처리기가 실패 JSON을 준다.
 */
public final class ExcelResponse {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelResponse() {
    }

    public static ExcelWriter.Target target(HttpServletResponse response, String menuNm) {
        return () -> {
            String fileName = menuNm + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                    + ".xlsx";
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType(CONTENT_TYPE);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"excel.xlsx\"; filename*=UTF-8''" + encoded);
            return response.getOutputStream();
        };
    }
}
