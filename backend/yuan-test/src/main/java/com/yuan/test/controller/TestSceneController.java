package com.yuan.test.controller;

import com.yuan.common.result.R;
import com.yuan.test.service.TestSceneService;
import com.yuan.test.vo.TestSceneVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestSceneController {

    final
    TestSceneService testSceneService;

    public TestSceneController(TestSceneService testSceneService) {
        this.testSceneService = testSceneService;
    }

    @GetMapping("/scenes")
    public R<List<TestSceneVO>> scenes(@RequestHeader("X-User-Id") Long userId) {
        return testSceneService.scenes(userId);
    }

}