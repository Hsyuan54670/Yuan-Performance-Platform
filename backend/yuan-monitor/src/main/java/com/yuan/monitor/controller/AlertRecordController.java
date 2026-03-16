package com.yuan.monitor.controller;

import com.yuan.common.result.R;
import com.yuan.monitor.service.AlertRecordService;
import com.yuan.monitor.vo.AlertRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor")
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    public AlertRecordController(AlertRecordService alertRecordService) {
        this.alertRecordService = alertRecordService;
    }

    @GetMapping("/alert-records")
    public R<List<AlertRecordVO>> listAlertRecords(@RequestHeader("X-User-Id") Long userId) {
        return alertRecordService.listAlertRecords(userId);
    }

    @GetMapping("/alert-records/runs/{runId}")
    public R<List<AlertRecordVO>> listAlertRecordsByRunId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long runId) {
        return alertRecordService.listAlertRecordsByRunId(userId, runId);
    }
}
