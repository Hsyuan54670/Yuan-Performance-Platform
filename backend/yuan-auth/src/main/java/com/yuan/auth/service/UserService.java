package com.yuan.auth.service;

import com.yuan.auth.dto.UserCreateDTO;
import com.yuan.auth.dto.UserStatusUpdateDTO;
import com.yuan.auth.vo.SystemUserVO;
import com.yuan.common.result.R;

import java.util.List;

public interface UserService {

    R<List<SystemUserVO>> listUsers();

    R<Long> createUser(UserCreateDTO request);

    R<Void> updateUserStatus(Long operatorUserId, Long userId, UserStatusUpdateDTO request);
}
