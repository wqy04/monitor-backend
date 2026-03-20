package com.example.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.monitor.entity.DeviceMonitor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMonitorMapper extends BaseMapper<DeviceMonitor> {
}