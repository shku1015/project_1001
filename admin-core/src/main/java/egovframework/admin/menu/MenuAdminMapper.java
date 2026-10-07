package egovframework.admin.menu;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 메뉴관리용 데이터 접근 (tb_menu, tb_permission, tb_role_permission). 내 메뉴 트리는 {@link MenuMapper}.
 */
@Mapper
public interface MenuAdminMapper {

    /** 메뉴 한 행. boardAutoYn: 게시판별 자동 메뉴(tb_board_master.menu_id로 연결)인지 */
    record MenuRow(long menuId, Long parentMenuId, String parentMenuNm, String menuCd, String menuNm,
                   String menuTypeCd, String menuUrl, String icon, int depth, int sortOrd, String useYn,
                   String systemYn, String boardAutoYn, LocalDateTime modDt) {
    }

    record GrantRow(long roleId, String roleNm, String actionCd) {
    }

    List<MenuRow> selectAllMenus();

    MenuRow selectMenu(@Param("menuId") long menuId);

    List<String> selectActions(@Param("menuId") long menuId);

    boolean existsMenuCd(@Param("menuCd") String menuCd);

    Integer selectMaxSortOrd(@Param("parentMenuId") Long parentMenuId);

    /** 등록하고 새 메뉴 ID를 돌려준다 (INSERT ... RETURNING) */
    long insertMenu(@Param("parentMenuId") Long parentMenuId, @Param("menuCd") String menuCd,
                    @Param("menuNm") String menuNm, @Param("menuTypeCd") String menuTypeCd,
                    @Param("menuUrl") String menuUrl, @Param("icon") String icon, @Param("depth") int depth,
                    @Param("sortOrd") int sortOrd, @Param("useYn") String useYn, @Param("regId") long regId);

    void insertPermissions(@Param("menuId") long menuId, @Param("actions") List<String> actions);

    /** 이 메뉴의 해당 액션 권한을 가진 역할 (액션 목록이 null이면 전부) */
    List<GrantRow> selectGrants(@Param("menuId") long menuId, @Param("actions") List<String> actions);

    void deleteRolePermissions(@Param("menuId") long menuId, @Param("actions") List<String> actions);

    void deletePermissions(@Param("menuId") long menuId, @Param("actions") List<String> actions);

    int updateMenu(@Param("menuId") long menuId, @Param("menuNm") String menuNm, @Param("menuUrl") String menuUrl,
                   @Param("icon") String icon, @Param("useYn") String useYn, @Param("modId") long modId,
                   @Param("modDt") LocalDateTime modDt);

    int updateParent(@Param("menuId") long menuId, @Param("parentMenuId") Long parentMenuId,
                     @Param("depth") int depth, @Param("sortOrd") int sortOrd, @Param("modId") long modId,
                     @Param("modDt") LocalDateTime modDt);

    /** 하위 메뉴 전체(자기 자신 제외)의 depth를 delta만큼 바꾼다 */
    void shiftDescendantDepth(@Param("menuId") long menuId, @Param("delta") int delta);

    /** 자기 자신과 하위 메뉴 전체의 ID */
    List<Long> selectSubtreeIds(@Param("menuId") long menuId);

    /** 자기 자신을 포함한 하위 트리의 가장 깊은 depth */
    int selectSubtreeMaxDepth(@Param("menuId") long menuId);

    List<Long> selectChildIds(@Param("parentMenuId") Long parentMenuId);

    void updateSortOrd(@Param("menuId") long menuId, @Param("sortOrd") int sortOrd);

    int countChildren(@Param("menuId") long menuId);

    void deleteMenu(@Param("menuId") long menuId);
}
