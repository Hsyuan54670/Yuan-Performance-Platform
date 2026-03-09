package com.yuan.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuan.common.result.R;
import com.yuan.test.entity.TestScene;
import com.yuan.test.vo.TestSceneVO;

import java.util.List;


public interface TestSceneService  extends IService<TestScene> {
    R<List<TestSceneVO>> scenes(Long userId);
}