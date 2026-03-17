package com.yuan.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.analysis.ai.service.AiAnalysisEnhancer;
import com.yuan.analysis.collector.AnalysisDataCollector;
import com.yuan.analysis.entity.AnalysisReport;
import com.yuan.analysis.entity.AnalysisSuggestion;
import com.yuan.analysis.entity.BottleneckRecord;
import com.yuan.analysis.mapper.AnalysisReportMapper;
import com.yuan.analysis.mapper.AnalysisSuggestionMapper;
import com.yuan.analysis.mapper.BottleneckRecordMapper;
import com.yuan.analysis.model.AnalysisComputationResult;
import com.yuan.analysis.model.AnalysisSnapshot;
import com.yuan.analysis.report.AnalysisReportHtmlRenderer;
import com.yuan.analysis.rule.RuleEngine;
import com.yuan.analysis.service.AnalysisService;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.api.test.mq.TestCompletedMessage;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisReportMapper analysisReportMapper;
    private final RuleEngine ruleEngine;
    private final BottleneckRecordMapper bottleneckRecordMapper;
    private final AnalysisSuggestionMapper analysisSuggestionMapper;
    private final AnalysisDataCollector analysisDataCollector;
    private final AiAnalysisEnhancer aiAnalysisEnhancer;
    private final AnalysisReportHtmlRenderer analysisReportHtmlRenderer;

    public AnalysisServiceImpl(
            AnalysisReportMapper analysisReportMapper,
            RuleEngine ruleEngine,
            BottleneckRecordMapper bottleneckRecordMapper,
            AnalysisSuggestionMapper analysisSuggestionMapper,
            AnalysisDataCollector analysisDataCollector,
            AiAnalysisEnhancer aiAnalysisEnhancer,
            AnalysisReportHtmlRenderer analysisReportHtmlRenderer
    ) {
        this.analysisReportMapper = analysisReportMapper;
        this.ruleEngine = ruleEngine;
        this.bottleneckRecordMapper = bottleneckRecordMapper;
        this.analysisSuggestionMapper = analysisSuggestionMapper;
        this.analysisDataCollector = analysisDataCollector;
        this.aiAnalysisEnhancer = aiAnalysisEnhancer;
        this.analysisReportHtmlRenderer = analysisReportHtmlRenderer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleMessage(TestCompletedMessage message) {
        Long userId = message.getUserId();
        Long taskId = message.getTaskId();
        Long runId = message.getRunId();

        if (userId == null || taskId == null || runId == null) {
            log.warn("Ignore completed message because key fields are missing: {}", message);
            return;
        }

        try {
            AnalysisReport report = getOrCreateReport(userId, taskId, runId);
            AnalysisSnapshot snapshot = analysisDataCollector.collectSnapshot(
                    userId,
                    taskId,
                    runId,
                    message.getStatus()
            );
            AnalysisComputationResult result = ruleEngine.analyze(snapshot);
            result = aiAnalysisEnhancer.enhance(userId, snapshot, result);

            fillReport(report, snapshot, result);
            saveReport(report);
            rebuildChildren(report.getId(), result);
        } catch (Exception e) {
            log.error("Failed to generate analysis report, userId={}, taskId={}, runId={}", userId, taskId, runId, e);
            throw new RuntimeException("Failed to generate analysis report", e);
        }
    }

    @Override
    public R<AnalysisResultVO> getReportByRunId(Long userId, Long runId) {
        AnalysisReport report = findReportByRunId(userId, runId);
        if (report == null) {
            return R.fail(HttpStatus.NOT_FOUND, "Report not found");
        }
        return R.success(toResultVO(report));
    }

    @Override
    public R<AnalysisResultVO> getLatestReportByTaskId(Long userId, Long taskId) {
        AnalysisReport report = findLatestReportByTaskId(userId, taskId);
        if (report == null) {
            return R.fail(HttpStatus.NOT_FOUND, "Report not found");
        }
        return R.success(toResultVO(report));
    }

    @Override
    public String getReportHtmlByRunId(Long userId, Long runId) {
        AnalysisReport report = findReportByRunId(userId, runId);
        if (report == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Report not found");
        }
        return analysisReportHtmlRenderer.render(toResultVO(report));
    }

    @Override
    public String getLatestReportHtmlByTaskId(Long userId, Long taskId) {
        AnalysisReport report = findLatestReportByTaskId(userId, taskId);
        if (report == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Report not found");
        }
        return analysisReportHtmlRenderer.render(toResultVO(report));
    }

    private AnalysisReport findReportByRunId(Long userId, Long runId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getRunId, runId)
                .last("limit 1");
        return analysisReportMapper.selectOne(wrapper);
    }

    private AnalysisReport findLatestReportByTaskId(Long userId, Long taskId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getTaskId, taskId)
                .orderByDesc(AnalysisReport::getCreatedAt)
                .last("limit 1");
        return analysisReportMapper.selectOne(wrapper);
    }

    private AnalysisReport getOrCreateReport(Long userId, Long taskId, Long runId) {
        AnalysisReport report = findReportByRunId(userId, runId);
        if (report != null) {
            return report;
        }

        AnalysisReport created = new AnalysisReport();
        created.setUserId(userId);
        created.setTaskId(taskId);
        created.setRunId(runId);
        return created;
    }

    private void fillReport(AnalysisReport report, AnalysisSnapshot snapshot, AnalysisComputationResult result) {
        report.setStatus(snapshot.getFinalStatus());
        report.setSource(StringUtils.hasText(result.getSource()) ? result.getSource() : "DATA_RULE");
        report.setGrade(result.getGrade());
        report.setScore(result.getScore());
        report.setSummary(result.getSummary());
    }

    private void saveReport(AnalysisReport report) {
        if (report.getId() == null) {
            analysisReportMapper.insert(report);
            return;
        }
        analysisReportMapper.updateById(report);
    }

    private void rebuildChildren(Long reportId, AnalysisComputationResult result) {
        deleteChildren(reportId);

        for (BottleneckRecord bottleneck : result.getBottlenecks()) {
            bottleneck.setReportId(reportId);
            bottleneckRecordMapper.insert(bottleneck);
        }

        for (AnalysisSuggestion suggestion : result.getSuggestions()) {
            suggestion.setReportId(reportId);
            analysisSuggestionMapper.insert(suggestion);
        }
    }

    private void deleteChildren(Long reportId) {
        LambdaQueryWrapper<BottleneckRecord> bottleneckWrapper = new LambdaQueryWrapper<>();
        bottleneckWrapper.eq(BottleneckRecord::getReportId, reportId);
        bottleneckRecordMapper.delete(bottleneckWrapper);

        LambdaQueryWrapper<AnalysisSuggestion> suggestionWrapper = new LambdaQueryWrapper<>();
        suggestionWrapper.eq(AnalysisSuggestion::getReportId, reportId);
        analysisSuggestionMapper.delete(suggestionWrapper);
    }

    private AnalysisResultVO toResultVO(AnalysisReport report) {
        AnalysisResultVO vo = new AnalysisResultVO();
        vo.setTaskId(report.getTaskId());
        vo.setRunId(report.getRunId());
        vo.setGrade(report.getGrade());
        vo.setScore(report.getScore());
        vo.setSummary(report.getSummary());
        vo.setStatus(report.getStatus());
        vo.setSource(report.getSource());
        vo.setCreatedAt(report.getCreatedAt());

        LambdaQueryWrapper<AnalysisSuggestion> suggestionWrapper = new LambdaQueryWrapper<>();
        suggestionWrapper.eq(AnalysisSuggestion::getReportId, report.getId());
        analysisSuggestionMapper.selectList(suggestionWrapper).forEach(suggestion -> {
            AnalysisResultVO.SuggestionItemVO item = new AnalysisResultVO.SuggestionItemVO();
            item.setId(suggestion.getId());
            item.setPriority(suggestion.getPriority());
            item.setTitle(suggestion.getTitle());
            item.setDetail(suggestion.getDetail());
            vo.getSuggestions().add(item);
        });

        LambdaQueryWrapper<BottleneckRecord> bottleneckWrapper = new LambdaQueryWrapper<>();
        bottleneckWrapper.eq(BottleneckRecord::getReportId, report.getId());
        bottleneckRecordMapper.selectList(bottleneckWrapper).forEach(bottleneck -> {
            AnalysisResultVO.BottleneckItemVO item = new AnalysisResultVO.BottleneckItemVO();
            item.setType(bottleneck.getType());
            item.setReason(bottleneck.getReason());
            item.setEvidence(bottleneck.getEvidence());
            item.setSeverity(bottleneck.getSeverity());
            item.setTime(bottleneck.getTimePoint());
            vo.getBottlenecks().add(item);
        });

        return vo;
    }
}
