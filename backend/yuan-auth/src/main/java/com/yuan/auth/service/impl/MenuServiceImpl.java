package com.yuan.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.auth.entity.Menu;
import com.yuan.auth.entity.RoleMenu;
import com.yuan.auth.mapper.MenuMapper;
import com.yuan.auth.mapper.RoleMenuMapper;
import com.yuan.auth.service.MenuService;
import com.yuan.auth.vo.SystemMenuVO;
import com.yuan.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuServiceImpl(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public R<List<SystemMenuVO>> listMenus() {
        return R.success(loadAllMenusSafely());
    }

    @Override
    public List<SystemMenuVO> listMenusByRoleIds(List<Long> roleIds) {
        try {
            List<Menu> allMenus = loadAllMenus();
            if (allMenus.isEmpty()) {
                return buildFallbackMenus();
            }
            if (roleIds == null || roleIds.isEmpty()) {
                return buildTree(allMenus);
            }

            List<RoleMenu> bindings = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                    .in(RoleMenu::getRoleId, roleIds));
            if (bindings == null || bindings.isEmpty()) {
                return buildTree(allMenus);
            }

            Set<Long> allowedIds = new LinkedHashSet<>();
            Map<Long, Menu> menuMap = new LinkedHashMap<>();
            for (Menu menu : allMenus) {
                menuMap.put(menu.getId(), menu);
            }

            for (RoleMenu binding : bindings) {
                Long menuId = binding.getMenuId();
                while (menuId != null && menuId > 0 && allowedIds.add(menuId)) {
                    Menu menu = menuMap.get(menuId);
                    if (menu == null) {
                        break;
                    }
                    menuId = menu.getParentId();
                }
            }

            List<Menu> filteredMenus = allMenus.stream()
                    .filter(menu -> allowedIds.contains(menu.getId()))
                    .toList();
            return filteredMenus.isEmpty() ? buildTree(allMenus) : buildTree(filteredMenus);
        } catch (Exception e) {
            log.warn("Failed to load menus by role ids, fallback to full menu tree, roleIds={}", roleIds, e);
            return loadAllMenusSafely();
        }
    }

    private List<SystemMenuVO> loadAllMenusSafely() {
        try {
            List<Menu> menus = loadAllMenus();
            return menus.isEmpty() ? buildFallbackMenus() : buildTree(menus);
        } catch (Exception e) {
            log.warn("Failed to load menus from auth_menu, fallback to built-in tree", e);
            return buildFallbackMenus();
        }
    }

    private List<Menu> loadAllMenus() {
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .orderByAsc(Menu::getSort)
                .orderByAsc(Menu::getId));
        return menus == null ? List.of() : menus;
    }

    private List<SystemMenuVO> buildTree(List<Menu> menus) {
        Map<Long, SystemMenuVO> nodeMap = new LinkedHashMap<>();
        List<SystemMenuVO> roots = new ArrayList<>();

        for (Menu menu : menus) {
            SystemMenuVO vo = new SystemMenuVO();
            vo.setId(menu.getId());
            vo.setName(menu.getName());
            vo.setPath(menu.getPath());
            nodeMap.put(menu.getId(), vo);
        }

        for (Menu menu : menus) {
            SystemMenuVO current = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(current);
                continue;
            }
            SystemMenuVO parent = nodeMap.get(parentId);
            if (parent == null) {
                roots.add(current);
                continue;
            }
            parent.getChildren().add(current);
        }

        return roots;
    }

    private List<SystemMenuVO> buildFallbackMenus() {
        SystemMenuVO dashboard = createMenu(1L, "Dashboard", "/dashboard");
        SystemMenuVO test = createMenu(2L, "Test", "/test");
        test.getChildren().add(createMenu(21L, "Plan", "/test/plan"));
        test.getChildren().add(createMenu(22L, "Scene", "/test/scene"));
        test.getChildren().add(createMenu(23L, "Task Create", "/test/task/create"));
        test.getChildren().add(createMenu(24L, "Task", "/test/task"));

        SystemMenuVO monitor = createMenu(3L, "Monitor", "/monitor");
        monitor.getChildren().add(createMenu(31L, "Realtime", "/monitor/realtime"));
        monitor.getChildren().add(createMenu(32L, "Alert", "/monitor/alert"));

        SystemMenuVO analysis = createMenu(4L, "Analysis", "/analysis");
        analysis.getChildren().add(createMenu(41L, "Report", "/analysis/report"));
        analysis.getChildren().add(createMenu(42L, "Rule", "/analysis/rule"));

        SystemMenuVO report = createMenu(5L, "Report Center", "/report");

        SystemMenuVO system = createMenu(6L, "System", "/system");
        system.getChildren().add(createMenu(61L, "User", "/system/user"));
        system.getChildren().add(createMenu(62L, "Role", "/system/role"));
        system.getChildren().add(createMenu(63L, "Menu", "/system/menu"));

        return List.of(dashboard, test, monitor, analysis, report, system);
    }

    private SystemMenuVO createMenu(Long id, String name, String path) {
        SystemMenuVO vo = new SystemMenuVO();
        vo.setId(id);
        vo.setName(name);
        vo.setPath(path);
        return vo;
    }
}
