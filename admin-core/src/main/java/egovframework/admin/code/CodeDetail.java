package egovframework.admin.code;

import java.time.LocalDateTime;

/** 상세코드 (docs/06-api/04-code.md) */
public record CodeDetail(String groupCd, String code, String codeNm, int sortOrd, String description, String useYn,
                         LocalDateTime modDt) {
}
