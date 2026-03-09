package com.yuan.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.test.entity.TestPlan;
import com.yuan.test.entity.TestScene;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestSceneMapper extends BaseMapper<TestScene> {
}