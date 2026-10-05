package egovframework.admin.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AuthTypes.Action;

/**
 * 메뉴 트리. 로그인한 관리자에게 보일 메뉴만 고른다 (docs/02-access-model.md 3.2).
 */
@Service
@Transactional(readOnly = true)
public class MenuService {

    public record MenuNode(long menuId, String menuCd, String menuNm, String menuTypeCd, String menuUrl, String icon,
                           List<MenuNode> children) {
    }

    private final MenuMapper menuMapper;

    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * - 화면 메뉴: 사용 중이고 READ 권한이 있으면 보인다.
     * - 폴더 메뉴: 사용 중이고 보이는 하위 메뉴가 하나라도 있으면 보인다.
     * - 사용 안 함 메뉴와 그 아래 메뉴는 보이지 않는다.
     */
    public List<MenuNode> getMyMenuTree(AdminAuthInfo auth) {
        Map<Long, List<MenuMapper.MenuRow>> childrenByParent = new LinkedHashMap<>();
        List<MenuMapper.MenuRow> roots = new ArrayList<>();
        for (MenuMapper.MenuRow row : menuMapper.selectAllMenus()) {
            if (row.parentMenuId() == null) {
                roots.add(row);
            } else {
                childrenByParent.computeIfAbsent(row.parentMenuId(), k -> new ArrayList<>()).add(row);
            }
        }
        return visible(roots, childrenByParent, auth);
    }

    private List<MenuNode> visible(List<MenuMapper.MenuRow> rows, Map<Long, List<MenuMapper.MenuRow>> childrenByParent,
                                   AdminAuthInfo auth) {
        List<MenuNode> result = new ArrayList<>();
        for (MenuMapper.MenuRow row : rows) {
            if (!"Y".equals(row.useYn())) {
                continue;
            }
            if ("PAGE".equals(row.menuTypeCd())) {
                if (auth.has(row.menuCd(), Action.READ)) {
                    result.add(toNode(row, List.of()));
                }
            } else {
                List<MenuNode> children = visible(childrenByParent.getOrDefault(row.menuId(), List.of()),
                        childrenByParent, auth);
                if (!children.isEmpty()) {
                    result.add(toNode(row, children));
                }
            }
        }
        return result;
    }

    private static MenuNode toNode(MenuMapper.MenuRow row, List<MenuNode> children) {
        return new MenuNode(row.menuId(), row.menuCd(), row.menuNm(), row.menuTypeCd(), row.menuUrl(), row.icon(),
                children);
    }
}
