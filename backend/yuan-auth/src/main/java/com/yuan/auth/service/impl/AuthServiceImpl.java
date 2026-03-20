package com.yuan.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.auth.dto.LoginRequestDTO;
import com.yuan.auth.dto.LogoutRequestDTO;
import com.yuan.auth.dto.RefreshTokenRequestDTO;
import com.yuan.auth.entity.Permission;
import com.yuan.auth.entity.Role;
import com.yuan.auth.entity.RolePermission;
import com.yuan.auth.entity.User;
import com.yuan.auth.entity.UserRole;
import com.yuan.auth.mapper.PermissionMapper;
import com.yuan.auth.mapper.RoleMapper;
import com.yuan.auth.mapper.RolePermissionMapper;
import com.yuan.auth.mapper.UserMapper;
import com.yuan.auth.mapper.UserRoleMapper;
import com.yuan.auth.service.AuthService;
import com.yuan.auth.service.MenuService;
import com.yuan.auth.vo.LoginResponseVO;
import com.yuan.auth.vo.UserInfoVO;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yuan.common.constant.CommonConstant.REDIS_BLACKLIST_REFRESH_TOKEN;
import static com.yuan.common.constant.CommonConstant.REDIS_BLACKLIST_TOKEN;
import static com.yuan.common.util.JwtUtil.generateRefreshToken;
import static com.yuan.common.util.JwtUtil.generateToken;

@Slf4j
@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private MenuService menuService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public R<LoginResponseVO> login(LoginRequestDTO request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return R.fail(HttpStatus.FORBIDDEN, "用户账户被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return R.fail(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        Long userId = user.getId();
        claims.put("userId", userId);
        claims.put("username", user.getUsername());

        return R.success(buildLoginResponse(user, claims));
    }

    @Override
    public R<UserInfoVO> me(String authorization) {
        Map<String, Object> claims;
        try {
            claims = checkAuthorization(authorization);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        Long userId = Long.parseLong(claims.get("userId").toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        return R.success(buildUserInfoVO(user));
    }

    @Override
    public R<LoginResponseVO> refresh(RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();
        Map<String, Object> claims;
        try {
            claims = checkToken(refreshToken);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        Long userId = Long.parseLong(claims.get("userId").toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return R.fail(HttpStatus.FORBIDDEN, "用户账户被禁用");
        }

        return R.success(buildLoginResponse(user, claims));
    }

    @Override
    public R<Void> logout(String authorization, LogoutRequestDTO request) {
        try {
            checkAuthorization(authorization);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
        String token = authorization.substring(7);
        String refreshToken = request.getRefreshToken();

        try {
            checkToken(refreshToken);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        long tokenRemainingExpiration;
        long refreshTokenRemainingExpiration;
        try {
            tokenRemainingExpiration = JwtUtil.getRemainingExpiration(token);
            refreshTokenRemainingExpiration = JwtUtil.getRemainingExpiration(refreshToken);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, "无效的 token");
        }

        if (tokenRemainingExpiration > 0) {
            stringRedisTemplate.opsForValue()
                    .set(REDIS_BLACKLIST_TOKEN + token, "unAuth", tokenRemainingExpiration, TimeUnit.MILLISECONDS);
        }
        if (refreshTokenRemainingExpiration > 0) {
            stringRedisTemplate.opsForValue()
                    .set(REDIS_BLACKLIST_REFRESH_TOKEN + refreshToken, "unAuth", refreshTokenRemainingExpiration, TimeUnit.MILLISECONDS);
        }

        return R.success(null);
    }

    private LoginResponseVO buildLoginResponse(User user, Map<String, Object> claims) {
        UserInfoVO userInfo = buildUserInfoVO(user);
        Map<String, Object> tokenClaims = new HashMap<>(claims);
        tokenClaims.put("permissions", String.join(",", userInfo.getPermissions()));

        String token = generateToken(tokenClaims);
        String refreshToken = generateRefreshToken(tokenClaims);

        LoginResponseVO response = new LoginResponseVO();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setUser(userInfo);
        return response;
    }

    private UserInfoVO buildUserInfoVO(User user) {
        Long userId = user.getId();
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(userId);
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());

        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .orderByAsc(UserRole::getRoleId));
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();

        Map<Long, Role> roleMap = roleIds.isEmpty()
                ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, role -> role, (left, right) -> left, LinkedHashMap::new));
        List<String> roleCodes = roleIds.stream()
                .map(roleMap::get)
                .filter(Objects::nonNull)
                .map(this::resolveRoleCode)
                .filter(StringUtils::hasText)
                .toList();

        userInfoVO.setRoles(roleCodes);
        userInfoVO.setRole(roleCodes.isEmpty() ? "UNASSIGNED" : roleCodes.get(0));
        userInfoVO.setPermissions(resolvePermissionCodes(roleIds));
        userInfoVO.setMenus(menuService.listMenusByRoleIds(roleIds));
        return userInfoVO;
    }

    private List<String> resolvePermissionCodes(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        try {
            List<RolePermission> rolePermissions = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                    .in(RolePermission::getRoleId, roleIds));
            if (rolePermissions == null || rolePermissions.isEmpty()) {
                return List.of();
            }

            List<Long> permissionIds = rolePermissions.stream()
                    .map(RolePermission::getPermissionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (permissionIds.isEmpty()) {
                return List.of();
            }

            Map<Long, Permission> permissionMap = permissionMapper.selectBatchIds(permissionIds).stream()
                    .collect(Collectors.toMap(Permission::getId, permission -> permission, (left, right) -> left));
            return permissionIds.stream()
                    .map(permissionMap::get)
                    .filter(Objects::nonNull)
                    .map(Permission::getCode)
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to resolve permission codes for roleIds={}, fallback to empty permissions", roleIds, e);
            return List.of();
        }
    }

    private String resolveRoleCode(Role role) {
        if (StringUtils.hasText(role.getCode())) {
            return role.getCode();
        }
        return role.getName();
    }

    private Map<String, Object> checkAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("无效的token");
        }
        String token = authorization.substring(7);
        return checkToken(token);
    }

    private Map<String, Object> checkToken(String token) {
        Map<String, Object> claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            throw new RuntimeException("无效的 token");
        }
        if (claims == null) {
            throw new RuntimeException("无效的 token");
        }
        return claims;
    }
}

