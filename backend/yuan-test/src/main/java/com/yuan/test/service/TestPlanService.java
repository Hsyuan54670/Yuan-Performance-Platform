package com.yuan.test.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yuan.common.result.R;
import com.yuan.test.entity.TestPlan;
import com.yuan.test.vo.TestPlanVO;

import java.util.List;

public interface TestPlanService extends IService<TestPlan> {
    R<List<TestPlanVO>> plans(Long userId);
}