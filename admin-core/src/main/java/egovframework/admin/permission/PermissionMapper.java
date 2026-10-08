package egovframework.admin.permission;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 권한관리용 데이터 접근 (tb_permission, tb_role_permission). 메뉴 기준으로 역할의 권한을 본다.
 * 메뉴 목록·전체 권한·역할 목록은 역할관리의 {@link egovframework.admin.role.RoleMapper}를 함께 쓴다.
 */
@Mapper
public interface PermissionMapper {

    /** 이 메뉴에서 역할에 부여된 권한 한 칸 */
    record GrantRow(long roleId, long permId, String actionCd) {
    }

    /** 관리자의 최종 권한 한 칸과 그 권한을 준 역할 */
    record AdminGrantRow(long menuId, String actionCd, String roleNm) {
    }

    record AdminRow(long adminId, String loginId, String adminNm) {
    }

    List<GrantRow> selectMenuGrants(@Param("menuId") long menuId);

    /** 이 관리자가 가진 역할 ID (사용 여부와 무관) */
    List<Long> selectAdminRoleIds(@Param("adminId") long adminId);

    AdminRow selectAdmin(@Param("adminId") long adminId);

    /** 사용 중인 역할·메뉴만 합친 최종 권한 (AuthMapper.selectPermissions와 같은 조건) */
    List<AdminGrantRow> selectAdminGrants(@Param("adminId") long adminId);
}
