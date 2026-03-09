package com.yuan.test.controller;

import com.yuan.common.result.R;
import com.yuan.test.service.TestPlanService;
import com.yuan.test.vo.TestTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestTaskController {

    @Autowired
    TestPlanService testPlanService;

//    @GetMapping("/tasks")
//    public R<List<TestTaskVO>> plans(@RequestHeader("X-User-Id") Long userId) {
//        return testPlanService.tasks(userId);
//    }


}