package com.yuan.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.auth.dto.LoginRequestDTO;
import com.yuan.auth.dto.LogoutRequestDTO;
import com.yuan.auth.dto.RefreshTokenRequestDTO;
import com.yuan.auth.entity.User;
import com.yuan.auth.entity.UserRole;
import com.yuan.auth.mapper.RoleMapper;
import com.yuan.auth.mapper.UserMapper;
import com.yuan.auth.mapper.UserRoleMapper;
import com.yuan.auth.service.AuthService;
import com.yuan.auth.vo.LoginResponseVO;
import com.yuan.auth.vo.UserInfoVO;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public R<LoginResponseVO> login(LoginRequestDTO request) {
        // 1. 验证用户名和密码
        User user = userMapper.selectByUsername(request.getUsername());

        // 1.0 验证用户是否存在
        if (user == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        //1.1 验证用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            return R.fail(HttpStatus.FORBIDDEN, "用户账户被禁用");
        }
        //1.2 验证账号密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return R.fail(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        // 2. 生成 JWT token
        Map<String, Object> claims = new HashMap<>();
        Long userId = user.getId();
        claims.put("userId", userId);
        claims.put("username", user.getUsername());


        return R.success(buildLoginResponse(user, claims));
    }

    @Override
    public R<UserInfoVO> me(String authorization){

        Map<String, Object> claims = null;
        try {
            claims = checkAuthorization(authorization);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        Long userId = Long.parseLong(claims.get("userId").toString());
        User user = userMapper.selectById(userId);
        if(user == null){
            return R.fail(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        return R.success(buildUserInfoVO(user));
    }

    @Override
    public R<LoginResponseVO> refresh(RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();
        // 1. 验证刷新令牌
        Map<String, Object> claims = null;
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
            return  R.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        long tokenRemainingExpiration;
        long refreshTokenRemainingExpiration;

        try {
            tokenRemainingExpiration=JwtUtil.getRemainingExpiration(token);
            refreshTokenRemainingExpiration= JwtUtil.getRemainingExpiration(refreshToken);
        } catch (Exception e) {
            return R.fail(HttpStatus.UNAUTHORIZED, "无效的 token");
        }

        if(tokenRemainingExpiration > 0){
            // 将访问令牌加入黑名单
            stringRedisTemplate.opsForValue()
                    .set(REDIS_BLACKLIST_TOKEN +token,"unAuth" , tokenRemainingExpiration, TimeUnit.MILLISECONDS);
        }
        if(refreshTokenRemainingExpiration > 0){
            // 将刷新令牌加入黑名单
            stringRedisTemplate.opsForValue()
                    .set(REDIS_BLACKLIST_REFRESH_TOKEN + refreshToken ,"unAuth" , refreshTokenRemainingExpiration,TimeUnit.MILLISECONDS);
        }

        return R.success(null);
    }

    /*
    * 代码封装
    * */
    private LoginResponseVO buildLoginResponse(User user,Map<String, Object> claims) {

        // 生成访问令牌
        String token = generateToken(claims);
        // 生成刷新令牌
        String refreshToken = generateRefreshToken(claims);

        // 返回登录响应
        LoginResponseVO response = new LoginResponseVO();

        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setUser(buildUserInfoVO(user));

        return response;
    }

    private UserInfoVO buildUserInfoVO(User user) {

        Long userId = user.getId();
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(userId);
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());

        UserRole userRole = userRoleMapper.selectByUserId(userId);
        if(userRole == null){
            throw new RuntimeException("用户角色未知");
        }
        userInfoVO.setRole(roleMapper.selectById(userRole.getRoleId()).getCode());

        return userInfoVO;
    }

    private Map<String, Object> checkAuthorization(String authorization){
        //1.验证是否以 Bearer 开头
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("无效的token");
        }
        //2.提取 token
        String token = authorization.substring(7);

        //3.解析 token 获取用户信息
        return checkToken(token);
    }

    private Map<String, Object> checkToken(String token){
        Map<String, Object> claims = null;
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
