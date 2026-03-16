package com.yuan.api.test.feign;

import com.yuan.api.test.dto.TestMetricDTO;
import com.yuan.api.test.dto.TestRunBaselineDTO;
import com.yuan.api.test.dto.TestRunContextDTO;
import com.yuan.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "yuan-test")
public interface TestFeignClient {
    @GetMapping("/test/runs/{runId}/metrics")
    R<List<TestMetricDTO>> listRunMetrics(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("runId") Long runId
    );

    @GetMapping("/test/runs/{runId}/context")
    R<TestRunContextDTO> getRunContext(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("runId") Long runId
    );

    @GetMapping("/test/runs/{runId}/baseline")
    R<TestRunBaselineDTO> getRunBaseline(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("runId") Long runId
    );
}
