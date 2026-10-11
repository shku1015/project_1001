package egovframework.admin.masking;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 마스킹 설정 관리용 데이터 접근 (tb_masking_policy). 적용은 {@link egovframework.admin.common.masking.MaskingService}.
 */
@Mapper
public interface MaskingAdminMapper {

    record PolicyRow(String fieldCd, String fieldNm, String screenMaskYn, String excelMaskYn, String modNm,
                     LocalDateTime modDt) {
    }

    /** 개인정보 항목 코드(PRIVACY_FIELD) 순서 */
    List<PolicyRow> selectPolicies();

    int updatePolicy(@Param("fieldCd") String fieldCd, @Param("screenMaskYn") String screenMaskYn,
                     @Param("excelMaskYn") String excelMaskYn, @Param("modId") long modId,
                     @Param("modDt") LocalDateTime modDt);
}
