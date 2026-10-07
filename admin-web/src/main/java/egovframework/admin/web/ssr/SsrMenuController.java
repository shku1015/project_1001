package egovframework.admin.web.ssr;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.admin.auth.AdminAuthInfo;
import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.auth.AuthTypes.Action;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.menu.MenuAdminService;
import egovframework.admin.menu.MenuAdminService.MenuAdminNode;
import egovframework.admin.menu.MenuAdminService.MenuDetail;
import egovframework.admin.menu.MenuAdminService.OrderEntry;
import egovframework.admin.menu.MenuAdminService.RevokedRole;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;

/**
 * ③ JSP SSR 메뉴관리 (SCR-MNU-01). Service를 직접 호출한다 (ADR-0003).
 * 순서 변경은 JavaScript로 트리를 바꾼 뒤 [순서 저장] 때 순서 목록(JSON)을 Form으로 보낸다 (ADR-0015).
 * 액션을 빼서 권한이 회수되면 같은 화면에 회수될 역할을 보여 주고 [회수하고 저장]을 한 번 더 받는다 (BR-07).
 */
@Controller
public class SsrMenuController {

    private static final String MENU = "MENU";

    private final MenuAdminService menuAdminService;
    private final AdminAuthInfoService authInfoService;
    private final ObjectMapper objectMapper;
    private final String sortablejsVersion;

    public SsrMenuController(MenuAdminService menuAdminService, AdminAuthInfoService authInfoService,
                             ObjectMapper objectMapper, @Value("${app.ui.sortablejs-version}") String sortablejsVersion) {
        this.menuAdminService = menuAdminService;
        this.authInfoService = authInfoService;
        this.objectMapper = objectMapper;
        this.sortablejsVersion = sortablejsVersion;
    }

    @GetMapping("/ssr/menus")
    @RequirePermission(menu = MENU, action = Action.READ)
    public String menus(@AuthenticationPrincipal AdminPrincipal principal, @RequestParam(required = false) Long menu,
                        @RequestParam(required = false) String create, Model model) {
        render(principal, model, menu, create);
        return "ssr/menu";
    }

    @PostMapping("/ssr/menus")
    @RequirePermission(menu = MENU, action = Action.CREATE)
    public String create(@AuthenticationPrincipal AdminPrincipal principal,
                         @RequestParam(required = false) Long parentMenuId, @RequestParam String menuCd,
                         @RequestParam String menuNm, @RequestParam String menuTypeCd,
                         @RequestParam(required = false) String menuUrl,
                         @RequestParam(required = false) List<Action> actions,
                         @RequestParam(required = false) String icon, @RequestParam String useYn,
                         HttpServletRequest request, RedirectAttributes redirect, Model model) {
        try {
            long menuId = menuAdminService.create(principal.adminId(), request.getRemoteAddr(),
                    new MenuAdminService.CreateCommand(parentMenuId, menuCd.trim(), menuNm, menuTypeCd, menuUrl,
                            actions == null ? List.of() : actions, icon, useYn));
            redirect.addFlashAttribute("notice", "등록되었습니다");
            return "redirect:/ssr/menus?menu=" + menuId;
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            render(principal, model, null, parentMenuId == null ? "root" : String.valueOf(parentMenuId));
            return "ssr/menu";
        }
    }

    @PostMapping("/ssr/menus/{menuId}/update")
    @RequirePermission(menu = MENU, action = Action.UPDATE)
    public String update(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId,
                         @RequestParam String menuNm, @RequestParam(required = false) String menuUrl,
                         @RequestParam(required = false) List<Action> actions,
                         @RequestParam(required = false) String icon, @RequestParam String useYn,
                         @RequestParam String modDt, @RequestParam(required = false) String confirmRevoke,
                         HttpServletRequest request, RedirectAttributes redirect, Model model) {
        MenuAdminService.UpdateCommand cmd = new MenuAdminService.UpdateCommand(menuNm, menuUrl,
                actions == null ? List.of() : actions, icon, useYn, LocalDateTime.parse(modDt));
        try {
            if (!"Y".equals(confirmRevoke)) {
                List<RevokedRole> revoked = menuAdminService.update(principal.adminId(), request.getRemoteAddr(),
                        menuId, cmd, true);
                if (!revoked.isEmpty()) {
                    // 확인을 받기 위해 입력한 값 그대로 다시 보여 준다
                    model.addAttribute("pendingRevoke", revoked);
                    render(principal, model, menuId, null);
                    return "ssr/menu";
                }
            }
            menuAdminService.update(principal.adminId(), request.getRemoteAddr(), menuId, cmd, false);
            redirect.addFlashAttribute("notice", "수정되었습니다");
        } catch (BusinessException e) {
            model.addAttribute("formError", message(e));
            render(principal, model, menuId, null);
            return "ssr/menu";
        }
        return "redirect:/ssr/menus?menu=" + menuId;
    }

    @PostMapping("/ssr/menus/{menuId}/move")
    @RequirePermission(menu = MENU, action = Action.UPDATE)
    public String move(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId,
                       @RequestParam(required = false) String parentMenuId, @RequestParam String modDt,
                       HttpServletRequest request, RedirectAttributes redirect) {
        try {
            menuAdminService.move(principal.adminId(), request.getRemoteAddr(), menuId,
                    parentMenuId == null || parentMenuId.isBlank() ? null : Long.valueOf(parentMenuId),
                    LocalDateTime.parse(modDt));
            redirect.addFlashAttribute("notice", "이동했습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/ssr/menus?menu=" + menuId;
    }

    @PostMapping("/ssr/menus/order")
    @RequirePermission(menu = MENU, action = Action.UPDATE)
    public String saveOrder(@RequestParam String orders, @RequestParam(required = false) Long selected,
                            RedirectAttributes redirect) {
        try {
            menuAdminService.saveOrder(objectMapper.readValue(orders, new TypeReference<List<OrderEntry>>() { }));
            redirect.addFlashAttribute("notice", "순서가 저장되었습니다");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
        } catch (JsonProcessingException e) {
            redirect.addFlashAttribute("error", ErrorCode.INVALID_REQUEST.message());
        }
        return "redirect:/ssr/menus" + (selected == null ? "" : "?menu=" + selected);
    }

    @PostMapping("/ssr/menus/{menuId}/delete")
    @RequirePermission(menu = MENU, action = Action.DELETE)
    public String delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable long menuId,
                         HttpServletRequest request, RedirectAttributes redirect) {
        try {
            menuAdminService.delete(principal.adminId(), request.getRemoteAddr(), menuId);
            redirect.addFlashAttribute("notice", "삭제되었습니다");
            return "redirect:/ssr/menus";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", message(e));
            return "redirect:/ssr/menus?menu=" + menuId;
        }
    }

    /**
     * 화면 데이터: 트리, 선택한 메뉴(수정) 또는 등록할 위치(create=root|상위 메뉴 ID), 버튼 권한, 옮길 수 있는 폴더.
     */
    private void render(AdminPrincipal principal, Model model, Long menuId, String create) {
        List<MenuAdminNode> tree = menuAdminService.getTree();
        AdminAuthInfo auth = authInfoService.load(principal.adminId());
        model.addAttribute("tree", tree);
        model.addAttribute("sortablejsVersion", sortablejsVersion);
        model.addAttribute("canCreate", auth.has(MENU, Action.CREATE));
        model.addAttribute("canUpdate", auth.has(MENU, Action.UPDATE));
        model.addAttribute("canDelete", auth.has(MENU, Action.DELETE));
        model.addAttribute("allActions", Arrays.asList(Action.values()));

        if (create != null) {
            model.addAttribute("mode", "create");
            if (!"root".equals(create)) {
                model.addAttribute("createParent", menuAdminService.getMenu(Long.parseLong(create)));
            }
        } else if (menuId != null) {
            MenuDetail selected = menuAdminService.getMenu(menuId);
            model.addAttribute("mode", "edit");
            model.addAttribute("selected", selected);
            MenuAdminNode node = find(tree, menuId);
            model.addAttribute("hasChildren", node != null && !node.children().isEmpty());
            model.addAttribute("moveTargets", moveTargets(tree, menuId));
        }
    }

    private static MenuAdminNode find(List<MenuAdminNode> nodes, long menuId) {
        for (MenuAdminNode n : nodes) {
            if (n.menuId() == menuId) {
                return n;
            }
            MenuAdminNode found = find(n.children(), menuId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 상위 메뉴 변경 대상: 자기 자신과 그 하위를 뺀 폴더 메뉴 (깊이 규칙은 서버가 다시 확인한다) */
    static List<MenuAdminNode> moveTargets(List<MenuAdminNode> tree, long menuId) {
        List<MenuAdminNode> result = new ArrayList<>();
        collectFolders(tree, menuId, result);
        return result;
    }

    private static void collectFolders(List<MenuAdminNode> nodes, long excludeId, List<MenuAdminNode> result) {
        for (MenuAdminNode n : nodes) {
            if (n.menuId() == excludeId) {
                continue;
            }
            if ("FOLDER".equals(n.menuTypeCd()) && n.depth() < MenuAdminService.MAX_DEPTH) {
                result.add(n);
            }
            collectFolders(n.children(), excludeId, result);
        }
    }

    private static String message(BusinessException e) {
        return e.getFieldErrors().isEmpty() ? e.getMessage() : e.getFieldErrors().get(0).message();
    }
}
