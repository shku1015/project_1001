package egovframework.admin.code;

import java.time.LocalDateTime;

/** 그룹코드 (docs/06-api/04-code.md) */
public record CodeGroup(String groupCd, String groupNm, String description, String systemYn, String useYn,
                        LocalDateTime modDt) {

    public boolean isSystem() {
        return "Y".equals(systemYn);
    }
}
