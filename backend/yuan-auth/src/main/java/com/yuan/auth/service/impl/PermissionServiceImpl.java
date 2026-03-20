package com.yuan.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.auth.entity.Permission;
import com.yuan.auth.mapper.PermissionMapper;
import com.yuan.auth.service.PermissionService;
import com.yuan.auth.vo.SystemPermissionVO;
import com.yuan.common.result.R;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public R<List<SystemPermissionVO>> listPermissions() {
        List<SystemPermissionVO> result = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .orderByAsc(Permission::getModule)
                        .orderByAsc(Permission::getCode))
                .stream()
                .map(permission -> {
                    SystemPermissionVO vo = new SystemPermissionVO();
                    vo.setId(permission.getId());
                    vo.setCode(permission.getCode());
                    vo.setName(permission.getName());
                    vo.setModule(permission.getModule());
                    vo.setDescription(permission.getDescription());
                    return vo;
                })
                .toList();
        return R.success(result);
    }
}
