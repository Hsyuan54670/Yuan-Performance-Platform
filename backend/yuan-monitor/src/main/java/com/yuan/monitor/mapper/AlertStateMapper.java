package com.yuan.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.monitor.entity.AlertRecord;
import com.yuan.monitor.entity.AlertState;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertStateMapper extends BaseMapper<AlertState> {
}
