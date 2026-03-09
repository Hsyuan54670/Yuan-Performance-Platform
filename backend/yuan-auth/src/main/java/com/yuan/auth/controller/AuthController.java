package com.yuan.auth.controller;

import com.yuan.auth.dto.LoginRequestDTO;
import com.yuan.auth.dto.LogoutRequestDTO;
import com.yuan.auth.dto.RefreshTokenRequestDTO;
import com.yuan.auth.service.AuthService;
import com.yuan.auth.vo.LoginResponseVO;
import com.yuan.auth.vo.UserInfoVO;
import com.yuan.common.result.R;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /*
    * 登录接口
    * */
    @PostMapping("/login")
    public R<LoginResponseVO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login request received, username={}", request.getUsername());
        return authService.login(request);
    }
    /*
    *
    * */
    @GetMapping("/me")
    public R<UserInfoVO> me(@RequestHeader("Authorization") String authorization) {
        return authService.me(authorization);
    }

    @PostMapping("/refresh")
    public R<LoginResponseVO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authorization,@Valid @RequestBody LogoutRequestDTO request) {
        return authService.logout(authorization,request);
    }

}