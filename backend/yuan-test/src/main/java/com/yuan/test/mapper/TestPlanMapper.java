package com.yuan.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.test.entity.TestPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestPlanMapper extends BaseMapper<TestPlan> {
}