package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.vo.AlertRecordVO;

import java.util.List;

public interface AlertRecordService {
    R<List<AlertRecordVO>> listAlertRecords(Long userId);
}
