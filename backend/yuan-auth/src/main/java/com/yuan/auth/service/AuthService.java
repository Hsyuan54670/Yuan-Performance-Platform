package com.yuan.auth.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yuan.auth.dto.LoginRequestDTO;
import com.yuan.auth.dto.LogoutRequestDTO;
import com.yuan.auth.dto.RefreshTokenRequestDTO;
import com.yuan.auth.entity.User;
import com.yuan.auth.vo.LoginResponseVO;
import com.yuan.auth.vo.UserInfoVO;
import com.yuan.common.result.R;
import jakarta.validation.Valid;

public interface AuthService extends IService<User> {

    R<LoginResponseVO> login(LoginRequestDTO request);

    R<UserInfoVO> me(String authorization);

    R<LoginResponseVO> refresh(RefreshTokenRequestDTO request);

    R<Void> logout(String authorization, LogoutRequestDTO request);
}