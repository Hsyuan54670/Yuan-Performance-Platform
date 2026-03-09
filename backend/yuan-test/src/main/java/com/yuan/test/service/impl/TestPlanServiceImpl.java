package com.yuan.test.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuan.common.result.R;
import com.yuan.test.entity.TestPlan;
import com.yuan.test.mapper.TestPlanMapper;
import com.yuan.test.service.TestPlanService;
import com.yuan.test.vo.TestPlanVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TestPlanServiceImpl extends ServiceImpl<TestPlanMapper,TestPlan> implements TestPlanService {
    @Override
    public R<List<TestPlanVO>> plans(Long userId) {
        log.info(userId.toString());
        List<TestPlan> list = this.lambdaQuery()
                .eq(TestPlan::getUserId,userId)
                .orderByDesc(TestPlan::getCreatedAt)
                .list();
        return R.success(list.stream()
                .map(TestPlanVO::fromEntity)
                .toList()
        );
    }
}
