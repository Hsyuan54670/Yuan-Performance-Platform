package com.yuan.auth.controller;

import com.yuan.auth.service.MenuService;
import com.yuan.auth.vo.SystemMenuVO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menus")
    public R<List<SystemMenuVO>> listMenus(@RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_MENU_READ)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_MENU_READ);
        }
        return menuService.listMenus();
    }
}
