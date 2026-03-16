package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.vo.ActiveAlertVO;

import java.util.List;

public interface AlertStateService {
    R<List<ActiveAlertVO>> listActiveAlerts(Long userId);
}
