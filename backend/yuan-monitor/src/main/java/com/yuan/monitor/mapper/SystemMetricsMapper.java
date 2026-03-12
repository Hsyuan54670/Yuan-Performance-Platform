package com.yuan.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.monitor.entity.SystemMetricsRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemMetricsMapper extends BaseMapper<SystemMetricsRecord> {
}
