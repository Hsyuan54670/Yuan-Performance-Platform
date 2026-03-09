package com.yuan.auth.mapper;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.awt.*;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User selectByUsername(String username);
}