package com.yuan.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.analysis.entity.AnalysisReport;
import com.yuan.analysis.mapper.AnalysisReportMapper;
import com.yuan.analysis.model.AlertEvidenceItem;
import com.yuan.analysis.model.AnalysisSnapshot;
import com.yuan.analysis.service.AnalysisService;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.api.monitor.dto.AlertRecordDTO;
import com.yuan.api.monitor.feign.MonitorFeignClient;
import com.yuan.api.test.mq.TestCompletedMessage;
import com.yuan.common.result.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisServiceImpl implements AnalysisService {

}
