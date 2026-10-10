package egovframework.admin.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자관리용 데이터 접근 (tb_user, tb_user_status_hist). 삭제된 회원(del_yn = 'Y')은 조회하지 않는다.
 * 개인정보는 원문으로 읽고 마스킹은 Service가 한다.
 */
@Mapper
public interface UserAdminMapper {

    /** 목록 검색 조건. 개인정보 항목도 원문으로 검색한다 (화면 명세 SCR-USR-01) */
    record Search(String userTypeCd, String loginId, String userNm, String email, String mobileNo, Long companyId,
                  String companyNm, String statusCd, String joinPath, LocalDate joinDtFrom, LocalDate joinDtTo) {
    }

    record ListRow(long userId, String userTypeCd, String userTypeNm, String loginId, String userNm, String email,
                   String mobileNo, Long companyId, String companyNm, String statusCd, String statusNm,
                   LocalDateTime joinDt) {
    }

    record DetailRow(long userId, String userTypeCd, String userTypeNm, String loginId, String userNm, String email,
                     String mobileNo, Long companyId, String companyNm, String statusCd, String statusNm,
                     LocalDateTime joinDt, LocalDate birthDate, String deptNm, String positionNm, String joinPath,
                     String pwdTempYn, LocalDateTime lastLoginDt, LocalDateTime withdrawDt, String regNm,
                     LocalDateTime regDt, String modNm, LocalDateTime modDt) {
    }

    record HistoryRow(LocalDateTime regDt, String beforeStatusCd, String beforeStatusNm, String afterStatusCd,
                      String afterStatusNm, String reason, String regNm) {
    }

    record CompanyOption(long companyId, String companyNm, String bizRegNo) {
    }

    /** 등록·수정 값 */
    record UserValues(String userNm, String email, String mobileNo, LocalDate birthDate, Long companyId,
                      String deptNm, String positionNm) {
    }

    long countUsers(@Param("s") Search search);

    /** limit이 null이면 전부 (엑셀) */
    List<ListRow> selectUsers(@Param("s") Search search, @Param("orderBy") String orderBy,
                              @Param("limit") Integer limit, @Param("offset") int offset);

    DetailRow selectUser(@Param("userId") long userId);

    /** 삭제된 회원의 아이디도 중복으로 본다 (BR-01) */
    boolean existsLoginId(@Param("loginId") String loginId);

    /** 소속 기업 상태. 없거나 삭제된 기업이면 null */
    String selectCompanyStatus(@Param("companyId") long companyId);

    List<CompanyOption> selectCompanyOptions(@Param("keyword") String keyword, @Param("limit") int limit);

    long insertUser(@Param("userTypeCd") String userTypeCd, @Param("loginId") String loginId,
                    @Param("password") String password, @Param("v") UserValues values, @Param("regId") long regId);

    int updateUser(@Param("userId") long userId, @Param("v") UserValues values, @Param("modId") long modId,
                   @Param("modDt") LocalDateTime modDt);

    /** 상태를 바꾼다. 탈퇴(WITHDRAWN)면 탈퇴 일시를 넣는다 */
    int updateStatus(@Param("userId") long userId, @Param("statusCd") String statusCd, @Param("modId") long modId,
                     @Param("modDt") LocalDateTime modDt);

    void insertStatusHist(@Param("userId") long userId, @Param("beforeStatusCd") String beforeStatusCd,
                          @Param("afterStatusCd") String afterStatusCd, @Param("reason") String reason,
                          @Param("regId") long regId);

    void updateTempPassword(@Param("userId") long userId, @Param("password") String password,
                            @Param("modId") long modId);

    void deleteUser(@Param("userId") long userId, @Param("modId") long modId);

    List<HistoryRow> selectStatusHistories(@Param("userId") long userId);

    /** 개인정보 원문 보기 사유 코드 이름 (코드 그룹 PRIVACY_REASON, 사용 중인 코드만). 없으면 null */
    String selectPrivacyReasonNm(@Param("reasonCd") String reasonCd);
}
