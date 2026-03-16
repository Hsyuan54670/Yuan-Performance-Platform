package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.result.R;
import com.yuan.monitor.entity.AlertRecord;
import com.yuan.monitor.mapper.AlertRecordMapper;
import com.yuan.monitor.service.AlertRecordService;
import com.yuan.monitor.vo.AlertRecordVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordMapper alertRecordMapper;

    public AlertRecordServiceImpl(AlertRecordMapper alertRecordMapper) {
        this.alertRecordMapper = alertRecordMapper;
    }

    @Override
    public R<List<AlertRecordVO>> listAlertRecords(Long userId) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getUserId, userId)
                .orderByDesc(AlertRecord::getCreatedAt)
                .orderByDesc(AlertRecord::getId);
        return R.success(alertRecordMapper.selectList(wrapper).stream().map(this::toVO).toList());
    }

    @Override
    public R<List<AlertRecordVO>> listAlertRecordsByRunId(Long userId, Long runId) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getUserId, userId)
                .eq(AlertRecord::getRunId, runId)
                .orderByDesc(AlertRecord::getCreatedAt)
                .orderByDesc(AlertRecord::getId);
        return R.success(alertRecordMapper.selectList(wrapper).stream().map(this::toVO).toList());
    }

    private AlertRecordVO toVO(AlertRecord entity) {
        AlertRecordVO vo = new AlertRecordVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setRunId(entity.getRunId());
        vo.setRuleName(entity.getRuleName());
        vo.setLevel(entity.getLevel());
        vo.setCurrentValue(entity.getCurrentValue());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setEventType(entity.getEventType());
        return vo;
    }
}
