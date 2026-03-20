package com.yuan.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.auth.dto.RoleMenuBindingDTO;
import com.yuan.auth.dto.RolePermissionBindingDTO;
import com.yuan.auth.dto.RoleUpsertDTO;
import com.yuan.auth.entity.Menu;
import com.yuan.auth.entity.Permission;
import com.yuan.auth.entity.Role;
import com.yuan.auth.entity.RoleMenu;
import com.yuan.auth.entity.RolePermission;
import com.yuan.auth.entity.UserRole;
import com.yuan.auth.mapper.MenuMapper;
import com.yuan.auth.mapper.PermissionMapper;
import com.yuan.auth.mapper.RoleMapper;
import com.yuan.auth.mapper.RoleMenuMapper;
import com.yuan.auth.mapper.RolePermissionMapper;
import com.yuan.auth.mapper.UserRoleMapper;
import com.yuan.auth.service.RoleService;
import com.yuan.auth.vo.SystemRoleVO;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;

    public RoleServiceImpl(
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            RoleMenuMapper roleMenuMapper,
            RolePermissionMapper rolePermissionMapper,
            PermissionMapper permissionMapper,
            MenuMapper menuMapper
    ) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public R<List<SystemRoleVO>> listRoles() {
        List<SystemRoleVO> result = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .orderByAsc(Role::getId))
                .stream()
                .map(this::toRoleVO)
                .toList();
        return R.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createRole(RoleUpsertDTO request) {
        String roleCode = normalizeCode(request.getCode());
        if (existsByCode(roleCode, null)) {
            return R.fail(HttpStatus.CONFLICT, "角色编码已存在");
        }

        Role role = new Role();
        role.setCode(roleCode);
        role.setName(request.getName().trim());
        role.setDescription(request.getDescription());
        role.setCreatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        return R.success(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> updateRole(Long roleId, RoleUpsertDTO request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        String roleCode = normalizeCode(request.getCode());
        if (existsByCode(roleCode, roleId)) {
            return R.fail(HttpStatus.CONFLICT, "角色编码已存在");
        }

        role.setCode(roleCode);
        role.setName(request.getName().trim());
        role.setDescription(request.getDescription());
        roleMapper.updateById(role);
        return R.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        Long bindingCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, roleId));
        if (bindingCount != null && bindingCount > 0) {
            return R.fail(HttpStatus.CONFLICT, "该角色仍被用户使用，无法删除");
        }

        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getRoleId, roleId));
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        roleMapper.deleteById(roleId);
        return R.success();
    }

    @Override
    public R<List<Long>> getRolePermissionIds(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        List<Long> permissionIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId)
                        .orderByAsc(RolePermission::getPermissionId))
                .stream()
                .map(RolePermission::getPermissionId)
                .toList();
        return R.success(permissionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> updateRolePermissionIds(Long roleId, RolePermissionBindingDTO request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        List<Long> permissionIds = sanitizeIds(request == null ? Collections.emptyList() : request.getPermissionIds());
        if (!permissionIds.isEmpty()) {
            Set<Long> existingIds = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                            .in(Permission::getId, permissionIds))
                    .stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
            if (existingIds.size() != permissionIds.size()) {
                return R.fail(HttpStatus.BAD_REQUEST, "存在无效的权限配置");
            }
        }

        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));

        LocalDateTime now = LocalDateTime.now();
        for (Long permissionId : permissionIds) {
            RolePermission binding = new RolePermission();
            binding.setRoleId(roleId);
            binding.setPermissionId(permissionId);
            binding.setCreatedAt(now);
            rolePermissionMapper.insert(binding);
        }
        return R.success();
    }

    @Override
    public R<List<Long>> getRoleMenuIds(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getRoleId, roleId)
                        .orderByAsc(RoleMenu::getMenuId))
                .stream()
                .map(RoleMenu::getMenuId)
                .toList();
        return R.success(menuIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> updateRoleMenuIds(Long roleId, RoleMenuBindingDTO request) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        List<Long> menuIds = sanitizeIds(request == null ? Collections.emptyList() : request.getMenuIds());
        if (!menuIds.isEmpty()) {
            Set<Long> existingIds = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                            .in(Menu::getId, menuIds))
                    .stream()
                    .map(Menu::getId)
                    .collect(Collectors.toSet());
            if (existingIds.size() != menuIds.size()) {
                return R.fail(HttpStatus.BAD_REQUEST, "存在无效的菜单配置");
            }
        }

        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getRoleId, roleId));

        LocalDateTime now = LocalDateTime.now();
        for (Long menuId : menuIds) {
            RoleMenu binding = new RoleMenu();
            binding.setRoleId(roleId);
            binding.setMenuId(menuId);
            binding.setCreatedAt(now);
            roleMenuMapper.insert(binding);
        }
        return R.success();
    }

    private SystemRoleVO toRoleVO(Role role) {
        SystemRoleVO vo = new SystemRoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        return vo;
    }

    private boolean existsByCode(String code, Long excludeRoleId) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, code);
        if (excludeRoleId != null) {
            wrapper.ne(Role::getId, excludeRoleId);
        }
        return roleMapper.selectCount(wrapper) > 0;
    }

    private String normalizeCode(String code) {
        return StringUtils.trimWhitespace(code).toUpperCase();
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        return ids == null ? Collections.emptyList() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
