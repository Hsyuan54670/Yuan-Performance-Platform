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
    public R<List<AlertRecordVO>> listAlertRecords() {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AlertRecord::getCreatedAt)
            .orderByDesc(AlertRecord::getId);

        List<AlertRecordVO> records = alertRecordMapper.selectList(wrapper).stream()
            .map(this::toVO)
            .toList();
        return R.success(records);
    }

    private AlertRecordVO toVO(AlertRecord entity) {
        AlertRecordVO vo = new AlertRecordVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setRuleName(entity.getRuleName());
        vo.setLevel(entity.getLevel());
        vo.setCurrentValue(entity.getCurrentValue());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
