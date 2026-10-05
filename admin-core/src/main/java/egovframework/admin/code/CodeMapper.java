package egovframework.admin.code;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CodeMapper {

    // ---- 코드 콤보 (공통) ----
    boolean existsGroup(@Param("groupCd") String groupCd);

    List<CodeItem> selectCodes(@Param("groupCd") String groupCd, @Param("includeUnused") boolean includeUnused);

    // ---- 그룹코드 ----
    List<CodeGroupSummary> selectGroups(@Param("keyword") String keyword, @Param("useYn") String useYn);

    CodeGroup selectGroup(@Param("groupCd") String groupCd);

    void insertGroup(@Param("g") CodeGroupRequest request);

    int updateGroup(@Param("groupCd") String groupCd, @Param("groupNm") String groupNm,
                    @Param("description") String description, @Param("useYn") String useYn,
                    @Param("modDt") LocalDateTime modDt);

    void deleteGroup(@Param("groupCd") String groupCd);

    int countCodes(@Param("groupCd") String groupCd);

    // ---- 상세코드 ----
    List<CodeDetail> selectDetails(@Param("groupCd") String groupCd);

    CodeDetail selectDetail(@Param("groupCd") String groupCd, @Param("code") String code);

    boolean existsDetail(@Param("groupCd") String groupCd, @Param("code") String code);

    void insertDetail(@Param("groupCd") String groupCd, @Param("d") CodeDetailRequest request);

    int updateDetail(@Param("groupCd") String groupCd, @Param("code") String code, @Param("codeNm") String codeNm,
                     @Param("sortOrd") int sortOrd, @Param("description") String description,
                     @Param("useYn") String useYn, @Param("modDt") LocalDateTime modDt);

    void deleteDetail(@Param("groupCd") String groupCd, @Param("code") String code);

    /** 등록 기본 정렬 순서: 현재 최댓값 + 1 */
    Integer selectMaxSortOrd(@Param("groupCd") String groupCd);

    /** 그룹코드 요청 (등록) */
    record CodeGroupRequest(String groupCd, String groupNm, String description, String useYn) {
    }

    /** 상세코드 요청 (등록) */
    record CodeDetailRequest(String code, String codeNm, int sortOrd, String description, String useYn) {
    }
}
