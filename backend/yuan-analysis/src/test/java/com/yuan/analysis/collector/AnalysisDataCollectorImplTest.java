package com.yuan.analysis.collector;

import com.yuan.analysis.model.SecondMetricPoint;
import com.yuan.api.monitor.dto.RunSystemMetricPointDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisDataCollectorImplTest {

    @Test
    void mergeMetricSeries_shouldKeepBusinessTimelineWhenSystemTimelineIsShifted() {
        AnalysisDataCollectorImpl collector = new AnalysisDataCollectorImpl();
        LocalDateTime base = LocalDateTime.of(2026, 3, 18, 10, 0, 0);

        List<SecondMetricPoint> businessPoints = new ArrayList<>();
        List<RunSystemMetricPointDTO> systemPoints = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SecondMetricPoint businessPoint = new SecondMetricPoint();
            businessPoint.setTs(base.plusSeconds(i + 1));
            businessPoint.setQps(BigDecimal.valueOf(100 + i));
            businessPoint.setP50(BigDecimal.valueOf(50));
            businessPoint.setP90(BigDecimal.valueOf(90));
            businessPoint.setP99(BigDecimal.valueOf(200 + i));
            businessPoint.setErrorRate(BigDecimal.ZERO);
            businessPoints.add(businessPoint);

            RunSystemMetricPointDTO systemPoint = new RunSystemMetricPointDTO();
            systemPoint.setTs(base.plusSeconds(i));
            systemPoint.setCpu(BigDecimal.valueOf(10 + i));
            systemPoint.setCpuMax(BigDecimal.valueOf(20 + i));
            systemPoint.setMemory(BigDecimal.valueOf(30 + i));
            systemPoint.setMemoryMax(BigDecimal.valueOf(40 + i));
            systemPoint.setSampleCount(4);
            systemPoint.setMissing(false);
            systemPoints.add(systemPoint);
        }

        @SuppressWarnings("unchecked")
        List<SecondMetricPoint> merged = (List<SecondMetricPoint>) ReflectionTestUtils.invokeMethod(
                collector,
                "mergeMetricSeries",
                businessPoints,
                systemPoints
        );

        assertThat(merged).hasSize(10);
        assertThat(merged).extracting(SecondMetricPoint::getTs).containsExactlyElementsOf(
                businessPoints.stream().map(SecondMetricPoint::getTs).toList()
        );
        assertThat(merged).allSatisfy(point -> assertThat(point.getCpu()).isNotNull());
        assertThat(merged.get(0).getCpu()).isEqualByComparingTo("10");
        assertThat(merged.get(9).getCpu()).isEqualByComparingTo("19");
    }

    @Test
    void mergeMetricSeries_shouldFallbackToSystemTimelineWhenBusinessPointsAreMissing() {
        AnalysisDataCollectorImpl collector = new AnalysisDataCollectorImpl();
        LocalDateTime base = LocalDateTime.of(2026, 3, 18, 11, 0, 0);

        List<RunSystemMetricPointDTO> systemPoints = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            RunSystemMetricPointDTO systemPoint = new RunSystemMetricPointDTO();
            systemPoint.setTs(base.plusSeconds(i));
            systemPoint.setCpu(BigDecimal.valueOf(60 + i));
            systemPoint.setMemory(BigDecimal.valueOf(70 + i));
            systemPoint.setSampleCount(4);
            systemPoint.setMissing(false);
            systemPoints.add(systemPoint);
        }

        @SuppressWarnings("unchecked")
        List<SecondMetricPoint> merged = (List<SecondMetricPoint>) ReflectionTestUtils.invokeMethod(
                collector,
                "mergeMetricSeries",
                List.of(),
                systemPoints
        );

        assertThat(merged).hasSize(3);
        assertThat(merged).extracting(SecondMetricPoint::getTs).containsExactly(
                base,
                base.plusSeconds(1),
                base.plusSeconds(2)
        );
        assertThat(merged).extracting(SecondMetricPoint::getCpu).containsExactly(
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(61),
                BigDecimal.valueOf(62)
        );
    }
}
