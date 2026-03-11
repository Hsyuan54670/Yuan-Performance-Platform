package com.yuan.test.controller;

import com.yuan.common.result.R;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/test")
public class PressureTargetController {
    @GetMapping("/mock/ping")
    public R<String> hello(){
        return R.success("pong");
    }
    @PostMapping("/mock/order")
    public R<String> echo() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return R.success("Yuan" );
    }
}
