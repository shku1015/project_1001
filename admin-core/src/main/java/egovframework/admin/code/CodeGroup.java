package egovframework.admin.code;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** 그룹코드 (docs/06-api/04-code.md) */
public record CodeGroup(String groupCd, String groupNm, String description, String systemYn, String useYn,
                        LocalDateTime modDt) {

    @JsonIgnore    // JSON 응답에 system 필드로 나가지 않게 (api/openapi.yaml CodeGroup)
    public boolean isSystem() {
        return "Y".equals(systemYn);
    }
}
