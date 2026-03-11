package com.yuan.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuan.common.result.R;
import com.yuan.test.dto.TestTaskCreateDTO;
import com.yuan.test.entity.TestTask;
import com.yuan.test.vo.TestMetricVO;
import com.yuan.test.vo.TestTaskVO;

import java.util.List;

public interface TestTaskService extends IService<TestTask> {
    R<List<TestTaskVO>> tasks(Long userId);


    R<Void> start(Long id, Long userId);

    R<Void> stop(Long id, Long userId);

    R<Long> create(TestTaskCreateDTO request, Long userId);

    R<List<TestMetricVO>> metrics(Long id, Long userId);
}