package com.yuan.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuan.common.result.R;
import com.yuan.test.dto.TestSceneDTO;
import com.yuan.test.entity.TestScene;
import com.yuan.test.vo.TestSceneVO;

import java.util.List;

public interface TestSceneService extends IService<TestScene> {
    R<List<TestSceneVO>> scenes(Long userId);

    R<Long> create(TestSceneDTO request, Long userId);

    R<Void> update(Long id, TestSceneDTO request, Long userId);

    R<Void> delete(Long id, Long userId);
}
