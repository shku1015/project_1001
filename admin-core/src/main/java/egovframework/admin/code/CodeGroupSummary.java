package egovframework.admin.code;

/** 그룹코드 목록 한 줄 (SCR-COD-01 왼쪽) */
public record CodeGroupSummary(String groupCd, String groupNm, int codeCnt, String systemYn, String useYn) {
}
