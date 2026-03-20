package com.yuan.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.auth.dto.UserCreateDTO;
import com.yuan.auth.dto.UserStatusUpdateDTO;
import com.yuan.auth.entity.Role;
import com.yuan.auth.entity.User;
import com.yuan.auth.entity.UserRole;
import com.yuan.auth.mapper.RoleMapper;
import com.yuan.auth.mapper.UserMapper;
import com.yuan.auth.mapper.UserRoleMapper;
import com.yuan.auth.service.UserService;
import com.yuan.auth.vo.SystemUserVO;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "DISABLED");

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public R<List<SystemUserVO>> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
        Map<Long, UserRole> userRoleMap = userRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(UserRole::getUserId, Function.identity(), (left, right) -> left));
        Map<Long, Role> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity(), (left, right) -> left));

        List<SystemUserVO> result = users.stream().map(user -> {
            SystemUserVO vo = new SystemUserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setStatus(user.getStatus());

            UserRole userRole = userRoleMap.get(user.getId());
            Role role = userRole == null ? null : roleMap.get(userRole.getRoleId());
            if (role == null) {
                vo.setRole("UNASSIGNED");
            } else {
                vo.setRole(StringUtils.hasText(role.getCode()) ? role.getCode() : role.getName());
            }
            return vo;
        }).toList();

        return R.success(result);
    }

    @Override
    @Transactional
    public R<Long> createUser(UserCreateDTO request) {
        String username = request.getUsername().trim();
        if (userMapper.selectByUsername(username) != null) {
            return R.fail(HttpStatus.CONFLICT, "用户名已存在");
        }

        Role role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            return R.fail(HttpStatus.NOT_FOUND, "角色不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setNickname(request.getNickname().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setCreatedAt(now);
        userRoleMapper.insert(userRole);

        return R.success(user.getId());
    }

    @Override
    public R<Void> updateUserStatus(Long operatorUserId, Long userId, UserStatusUpdateDTO request) {
        if (operatorUserId != null && operatorUserId.equals(userId)) {
            return R.fail(HttpStatus.FORBIDDEN, "不能修改自己的状态");
        }

        String nextStatus = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(nextStatus)) {
            return R.fail(HttpStatus.BAD_REQUEST, "不支持的用户状态");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return R.fail(HttpStatus.NOT_FOUND, "用户不存在");
        }

        user.setStatus(nextStatus);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return R.success();
    }
}
