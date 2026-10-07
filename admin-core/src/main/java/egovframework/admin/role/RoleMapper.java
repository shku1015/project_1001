package egovframework.admin.role;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 역할관리용 데이터 접근 (tb_role, tb_role_permission, tb_admin_role).
 */
@Mapper
public interface RoleMapper {

    record RoleRow(long roleId, String roleCd, String roleNm, String description, int adminCnt, String systemYn,
                   String useYn, String regNm, LocalDateTime regDt, String modNm, LocalDateTime modDt) {
    }

    /** 권한 표의 메뉴 한 행 (depth, sort_ord 순) */
    record MenuRow(long menuId, Long parentMenuId, String menuCd, String menuNm, String menuTypeCd, int depth,
                   String useYn) {
    }

    /** 권한 한 칸: 메뉴 × 액션. permId는 tb_permission */
    record PermRow(long permId, long menuId, String menuCd, String menuNm, String actionCd) {
    }

    record AdminRow(long adminId, String loginId, String adminNm, String deptNm, String statusCd, String statusNm,
                    LocalDateTime grantedDt) {
    }

    List<RoleRow> selectRoles(@Param("keyword") String keyword, @Param("useYn") String useYn);

    RoleRow selectRole(@Param("roleId") long roleId);

    boolean existsRoleCd(@Param("roleCd") String roleCd);

    /** 이 관리자가 가진 역할인지 (사용 여부와 무관) */
    boolean hasRole(@Param("adminId") long adminId, @Param("roleId") long roleId);

    long insertRole(@Param("roleCd") String roleCd, @Param("roleNm") String roleNm,
                    @Param("description") String description, @Param("useYn") String useYn,
                    @Param("regId") long regId);

    int updateRole(@Param("roleId") long roleId, @Param("roleNm") String roleNm,
                   @Param("description") String description, @Param("useYn") String useYn,
                   @Param("modId") long modId, @Param("modDt") LocalDateTime modDt);

    /** 권한 설정 시 동시 수정 확인용으로 mod_dt만 갱신한다 */
    int touchRole(@Param("roleId") long roleId, @Param("modId") long modId, @Param("modDt") LocalDateTime modDt);

    void deleteRole(@Param("roleId") long roleId);

    List<MenuRow> selectMenus();

    /** 전체 권한(메뉴 × 사용 액션) */
    List<PermRow> selectAllPermissions();

    /** 이 역할에 부여된 권한 */
    List<PermRow> selectRolePermissions(@Param("roleId") long roleId);

    void insertRolePermissions(@Param("roleId") long roleId, @Param("permIds") List<Long> permIds,
                               @Param("regId") long regId);

    void deleteRolePermissions(@Param("roleId") long roleId, @Param("permIds") List<Long> permIds);

    void deleteAllRolePermissions(@Param("roleId") long roleId);

    List<AdminRow> selectRoleAdmins(@Param("roleId") long roleId);
}
