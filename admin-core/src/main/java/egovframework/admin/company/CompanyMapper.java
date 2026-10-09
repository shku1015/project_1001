package egovframework.admin.company;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import egovframework.admin.common.PageQuery;

/**
 * 기업정보관리용 데이터 접근 (tb_company, tb_user). 삭제된 기업(del_yn = 'Y')은 조회하지 않는다.
 */
@Mapper
public interface CompanyMapper {

    record Search(String companyNm, String bizRegNo, String ceoNm, String statusCd, LocalDate regDtFrom,
                  LocalDate regDtTo) {
    }

    record ListRow(long companyId, String companyNm, String bizRegNo, String ceoNm, int memberCnt, String statusCd,
                   String statusNm, LocalDateTime regDt) {
    }

    record DetailRow(long companyId, String companyNm, String bizRegNo, String ceoNm, String bizType, String bizItem,
                     String telNo, String zipCd, String addr, String addrDtl, String statusCd, String statusNm,
                     int memberCnt, String regNm, LocalDateTime regDt, String modNm, LocalDateTime modDt) {
    }

    record UserRow(long userId, String loginId, String userNm, String deptNm, String positionNm, String statusCd,
                   String statusNm, LocalDateTime joinDt) {
    }

    long countCompanies(@Param("s") Search search);

    /** limit이 null이면 전부 (엑셀) */
    List<ListRow> selectCompanies(@Param("s") Search search, @Param("orderBy") String orderBy,
                                  @Param("limit") Integer limit, @Param("offset") int offset);

    DetailRow selectCompany(@Param("companyId") long companyId);

    /** 삭제된 기업의 번호도 중복으로 본다 (BR-01) */
    boolean existsBizRegNo(@Param("bizRegNo") String bizRegNo);

    long insertCompany(@Param("c") CompanyAdminService.CompanyCommand command, @Param("bizRegNo") String bizRegNo,
                       @Param("regId") long regId);

    int updateCompany(@Param("companyId") long companyId, @Param("c") CompanyAdminService.CompanyCommand command,
                      @Param("modId") long modId, @Param("modDt") LocalDateTime modDt);

    int updateStatus(@Param("companyId") long companyId, @Param("statusCd") String statusCd,
                     @Param("modId") long modId, @Param("modDt") LocalDateTime modDt);

    void deleteCompany(@Param("companyId") long companyId, @Param("modId") long modId);

    long countUsers(@Param("companyId") long companyId);

    List<UserRow> selectUsers(@Param("companyId") long companyId, @Param("limit") int limit);
}
