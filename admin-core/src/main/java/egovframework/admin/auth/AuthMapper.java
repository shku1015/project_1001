package egovframework.admin.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 인증·내 정보용 데이터 접근 (tb_admin, tb_admin_login_hist, 권한 조회).
 */
@Mapper
public interface AuthMapper {

    record LoginAdmin(long adminId, String loginId, String password, String statusCd, String pwdTempYn) {
    }

    record AdminProfile(long adminId, String loginId, String adminNm, String email, String mobileNo, String deptNm,
                        String statusCd, String pwdTempYn, LocalDateTime pwdChangedDt, LocalDateTime modDt) {
    }

    record RoleRow(String roleCd, String roleNm, String useYn) {
    }

    record PermissionRow(String menuCd, String actionCd) {
    }

    record LoginHistRow(LocalDateTime regDt, String ipAddr) {
    }

    LoginAdmin selectLoginAdmin(@Param("loginId") String loginId);

    /** 실패 횟수를 1 올리고, 기준에 이르면 잠금 상태로 바꾼다 */
    void increaseLoginFail(@Param("adminId") long adminId, @Param("lockThreshold") int lockThreshold);

    void updateLoginSuccess(@Param("adminId") long adminId);

    void insertLoginHist(@Param("adminId") Long adminId, @Param("loginId") String loginId,
                         @Param("resultCd") String resultCd, @Param("authTypeCd") String authTypeCd,
                         @Param("ipAddr") String ipAddr, @Param("userAgent") String userAgent);

    AdminProfile selectAdminProfile(@Param("adminId") long adminId);

    List<RoleRow> selectRoles(@Param("adminId") long adminId);

    /** 사용 중인 역할을 거친 권한 (사용 안 함 메뉴 제외) */
    List<PermissionRow> selectPermissions(@Param("adminId") long adminId);

    /** 이번 로그인 직전의 성공 로그인 */
    LoginHistRow selectPreviousLogin(@Param("adminId") long adminId);

    String selectPasswordHash(@Param("adminId") long adminId);

    int updateMyInfo(@Param("adminId") long adminId, @Param("adminNm") String adminNm, @Param("email") String email,
                     @Param("mobileNo") String mobileNo, @Param("deptNm") String deptNm,
                     @Param("modDt") LocalDateTime modDt);

    void updatePassword(@Param("adminId") long adminId, @Param("password") String password);

    boolean existsLoginId(@Param("loginId") String loginId);

    void insertInitialAdmin(@Param("loginId") String loginId, @Param("password") String password,
                            @Param("adminNm") String adminNm, @Param("email") String email,
                            @Param("roleCd") String roleCd);
}
