package egovframework.admin.menu;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MenuMapper {

    record MenuRow(long menuId, Long parentMenuId, String menuCd, String menuNm, String menuTypeCd, String menuUrl,
                   String icon, int depth, int sortOrd, String useYn) {
    }

    List<MenuRow> selectAllMenus();
}
