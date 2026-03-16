package com.yuan.api.monitor.feign;

import com.yuan.api.monitor.dto.AlertRecordDTO;
import com.yuan.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "yuan-monitor")
public interface MonitorFeignClient {
    @GetMapping("/monitor/alert-records/runs/{runId}")
    R<List<AlertRecordDTO>> listAlertRecordsByRunId(@RequestHeader("X-User-Id") Long userId, @PathVariable("runId") Long runId);
}
