package egovframework.admin.common.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;

/**
 * 엑셀(.xlsx) 만들기 (docs/06-api-spec.md 8절). SXSSF로 행을 디스크에 흘려 써서 메모리를 적게 쓴다.
 * 일시는 "yyyy-MM-dd HH:mm:ss" 문자열, 숫자는 숫자 칸으로 쓴다.
 */
public final class ExcelWriter {

    /** 한 번에 내려받을 수 있는 최대 건수. 넘으면 EXCEL_LIMIT_EXCEEDED */
    public static final int MAX_ROWS = 10_000;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 파일을 쓸 곳. 건수 확인 등이 끝난 뒤에 연다 (그 전에 오류가 나면 파일 대신 실패 JSON을 준다) */
    @FunctionalInterface
    public interface Target {
        OutputStream open() throws IOException;
    }

    private ExcelWriter() {
    }

    /** 건수를 먼저 확인한다 (데이터를 읽기 전에) */
    public static void checkLimit(long count) {
        if (count > MAX_ROWS) {
            throw new BusinessException(ErrorCode.EXCEL_LIMIT_EXCEEDED);
        }
    }

    public static void write(OutputStream out, String sheetName, List<String> headers, List<List<Object>> rows)
            throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(500)) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
                header.getCell(i).setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            int r = 1;
            for (List<Object> values : rows) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.get(i);
                    if (v instanceof Number n) {
                        row.createCell(i).setCellValue(n.doubleValue());
                    } else if (v instanceof LocalDateTime t) {
                        row.createCell(i).setCellValue(t.format(DATE_TIME));
                    } else if (v instanceof LocalDate d) {
                        row.createCell(i).setCellValue(d.toString());
                    } else {
                        row.createCell(i).setCellValue(v == null ? "" : v.toString());
                    }
                }
            }
            workbook.write(out);
            workbook.dispose();
        }
    }
}
