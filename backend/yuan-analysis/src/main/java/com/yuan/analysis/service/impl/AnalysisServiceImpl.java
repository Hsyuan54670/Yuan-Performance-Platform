package com.yuan.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.analysis.collector.AnalysisDataCollector;
import com.yuan.analysis.entity.AnalysisReport;
import com.yuan.analysis.entity.AnalysisSuggestion;
import com.yuan.analysis.entity.BottleneckRecord;
import com.yuan.analysis.mapper.AnalysisReportMapper;
import com.yuan.analysis.mapper.AnalysisSuggestionMapper;
import com.yuan.analysis.mapper.BottleneckRecordMapper;
import com.yuan.analysis.model.AnalysisComputationResult;
import com.yuan.analysis.model.AnalysisSnapshot;
import com.yuan.analysis.rule.RuleEngine;
import com.yuan.analysis.service.AnalysisService;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.api.test.mq.TestCompletedMessage;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
// 作为分析主链的编排层：收消息、采快照、跑规则、再把结果落到主表和子表。
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private AnalysisReportMapper arMapper;
    @Autowired
    private RuleEngine ruleEngine;
    @Autowired
    private BottleneckRecordMapper brMapper;
    @Autowired
    private AnalysisSuggestionMapper asMapper;
    @Autowired
    private AnalysisDataCollector analysisDataCollector;

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

            // 采集器负责把 run 维度的数据拼成统一快照，service 这里只负责编排。
            AnalysisSnapshot snapshot = analysisDataCollector.collectSnapshot(
                    userId,
                    taskId,
                    runId,
                    message.getStatus()
            );

            // 规则引擎只消费快照，不直接依赖 MQ 消息或外部服务。
            AnalysisComputationResult result = ruleEngine.analyze(snapshot);

            // 主表与子表必须一起成功或一起回滚，避免产生半成品分析报告。
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
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getRunId, runId)
                .last("limit 1");

        AnalysisReport report = arMapper.selectOne(wrapper);
        if (report == null) {
            return R.fail(HttpStatus.NOT_FOUND, "Report not found");
        }
        return R.success(toResultVO(report));
    }

    @Override
    public R<AnalysisResultVO> getLatestReportByTaskId(Long userId, Long taskId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getTaskId, taskId)
                .orderByDesc(AnalysisReport::getCreatedAt)
                .last("limit 1");

        AnalysisReport report = arMapper.selectOne(wrapper);
        if (report == null) {
            return R.fail(HttpStatus.NOT_FOUND, "Report not found");
        }
        return R.success(toResultVO(report));
    }

    // 同一个 run 只维护一份分析报告，重复消费时走更新路径而不是重复插入。
    private AnalysisReport getOrCreateReport(Long userId, Long taskId, Long runId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getRunId, runId)
                .last("limit 1");

        AnalysisReport report = arMapper.selectOne(wrapper);
        if (report != null) {
            return report;
        }

        AnalysisReport created = new AnalysisReport();
        created.setUserId(userId);
        created.setTaskId(taskId);
        created.setRunId(runId);
        return created;
    }

    // 主表保存的是摘要结果，真正的规则明细和建议存放在子表中。
    private void fillReport(AnalysisReport report, AnalysisSnapshot snapshot, AnalysisComputationResult result) {
        report.setStatus(snapshot.getFinalStatus());
        report.setSource("DATA_RULE");
        report.setGrade(result.getGrade());
        report.setScore(result.getScore());
        report.setSummary(result.getSummary());
    }

    // insert 和 update 统一在这里收口，避免主流程分叉过多。
    private void saveReport(AnalysisReport report) {
        if (report.getId() == null) {
            arMapper.insert(report);
            return;
        }
        arMapper.updateById(report);
    }

    // 子表采用重建策略，确保重跑分析后不会残留旧的瓶颈和建议。
    private void rebuildChildren(Long reportId, AnalysisComputationResult result) {
        deleteChildren(reportId);

        for (BottleneckRecord bottleneck : result.getBottlenecks()) {
            bottleneck.setReportId(reportId);
            brMapper.insert(bottleneck);
        }

        for (AnalysisSuggestion suggestion : result.getSuggestions()) {
            suggestion.setReportId(reportId);
            asMapper.insert(suggestion);
        }
    }

    // 先清旧数据，再按本次规则结果重建，逻辑更简单也更不容易产生脏数据。
    private void deleteChildren(Long reportId) {
        LambdaQueryWrapper<BottleneckRecord> bottleneckWrapper = new LambdaQueryWrapper<>();
        bottleneckWrapper.eq(BottleneckRecord::getReportId, reportId);
        brMapper.delete(bottleneckWrapper);

        LambdaQueryWrapper<AnalysisSuggestion> suggestionWrapper = new LambdaQueryWrapper<>();
        suggestionWrapper.eq(AnalysisSuggestion::getReportId, reportId);
        asMapper.delete(suggestionWrapper);
    }

    // 查询接口统一在这里把主表与子表重新组装成前端可直接消费的 VO。
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
        asMapper.selectList(suggestionWrapper).forEach(suggestion -> {
            AnalysisResultVO.SuggestionItemVO item = new AnalysisResultVO.SuggestionItemVO();
            item.setId(suggestion.getId());
            item.setPriority(suggestion.getPriority());
            item.setTitle(suggestion.getTitle());
            item.setDetail(suggestion.getDetail());
            vo.getSuggestions().add(item);
        });

        LambdaQueryWrapper<BottleneckRecord> bottleneckWrapper = new LambdaQueryWrapper<>();
        bottleneckWrapper.eq(BottleneckRecord::getReportId, report.getId());
        brMapper.selectList(bottleneckWrapper).forEach(bottleneck -> {
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
