package com.yuan.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.auth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    UserRole selectByUserId(Long userId);
}
