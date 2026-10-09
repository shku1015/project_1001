package egovframework.admin.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import egovframework.admin.common.PageQuery;

/**
 * 관리자관리용 데이터 접근 (tb_admin, tb_admin_role, tb_admin_login_hist).
 */
@Mapper
public interface AdminManageMapper {

    /** 목록 검색 조건 (keyword: 로그인 아이디·이름) */
    record Search(String keyword, String loginId, String adminNm, String deptNm, Long roleId, String statusCd) {
    }

    record ListRow(long adminId, String loginId, String adminNm, String deptNm, String statusCd, String statusNm,
                   LocalDateTime lastLoginDt, LocalDateTime regDt) {
    }

    record RoleOfAdmin(long adminId, long roleId, String roleNm) {
    }

    record DetailRow(long adminId, String loginId, String adminNm, String email, String mobileNo, String deptNm,
                     String statusCd, String statusNm, int loginFailCnt, String pwdTempYn,
                     LocalDateTime pwdChangedDt, LocalDateTime lastLoginDt, String regNm, LocalDateTime regDt,
                     String modNm, LocalDateTime modDt) {
    }

    record GrantedRoleRow(long roleId, String roleCd, String roleNm, String useYn, String systemYn, String regNm,
                          LocalDateTime regDt) {
    }

    record RoleRow(long roleId, String roleCd, String roleNm, String useYn, String systemYn) {
    }

    /** 역할의 권한 한 칸 (R5 확인용) */
    record RolePermRow(long roleId, String menuCd, String actionCd) {
    }

    record LoginHistRow(LocalDateTime regDt, String resultCd, String resultNm, String authTypeCd, String ipAddr) {
    }

    long countAdmins(@Param("s") Search search);

    List<ListRow> selectAdmins(@Param("s") Search search, @Param("q") PageQuery query, @Param("offset") int offset);

    List<RoleOfAdmin> selectRolesOf(@Param("adminIds") List<Long> adminIds);

    DetailRow selectAdmin(@Param("adminId") long adminId);

    List<GrantedRoleRow> selectGrantedRoles(@Param("adminId") long adminId);

    boolean existsLoginId(@Param("loginId") String loginId);

    long insertAdmin(@Param("loginId") String loginId, @Param("password") String password,
                     @Param("adminNm") String adminNm, @Param("email") String email,
                     @Param("mobileNo") String mobileNo, @Param("deptNm") String deptNm, @Param("regId") long regId);

    int updateInfo(@Param("adminId") long adminId, @Param("adminNm") String adminNm, @Param("email") String email,
                   @Param("mobileNo") String mobileNo, @Param("deptNm") String deptNm, @Param("modId") long modId,
                   @Param("modDt") LocalDateTime modDt);

    int insertAdminRole(@Param("adminId") long adminId, @Param("roleId") long roleId, @Param("regId") long regId);

    int deleteAdminRole(@Param("adminId") long adminId, @Param("roleId") long roleId);

    /** 상태를 바꾼다. modDt가 있으면 동시 수정을 확인한다. resetFail이면 실패 횟수를 0으로 */
    int updateStatus(@Param("adminId") long adminId, @Param("statusCd") String statusCd,
                     @Param("resetFail") boolean resetFail, @Param("modId") long modId,
                     @Param("modDt") LocalDateTime modDt);

    void updateTempPassword(@Param("adminId") long adminId, @Param("password") String password,
                            @Param("modId") long modId);

    /** 사용 중(ACTIVE)이고 사용 중인 슈퍼관리자 역할을 가진 관리자 수 (R3) */
    int countActiveSuperAdmins();

    List<RoleRow> selectRoles();

    List<RolePermRow> selectRolePermissions();

    List<LoginHistRow> selectLoginHistories(@Param("adminId") long adminId, @Param("limit") int limit);
}
