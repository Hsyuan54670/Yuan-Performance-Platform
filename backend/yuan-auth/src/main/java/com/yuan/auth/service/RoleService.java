package com.yuan.auth.service;

import com.yuan.auth.dto.RoleMenuBindingDTO;
import com.yuan.auth.dto.RolePermissionBindingDTO;
import com.yuan.auth.dto.RoleUpsertDTO;
import com.yuan.auth.vo.SystemRoleVO;
import com.yuan.common.result.R;

import java.util.List;

public interface RoleService {

    R<List<SystemRoleVO>> listRoles();

    R<Long> createRole(RoleUpsertDTO request);

    R<Void> updateRole(Long roleId, RoleUpsertDTO request);

    R<Void> deleteRole(Long roleId);

    R<List<Long>> getRolePermissionIds(Long roleId);

    R<Void> updateRolePermissionIds(Long roleId, RolePermissionBindingDTO request);

    R<List<Long>> getRoleMenuIds(Long roleId);

    R<Void> updateRoleMenuIds(Long roleId, RoleMenuBindingDTO request);
}
