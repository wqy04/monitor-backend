package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.DeviceMonitor;
import com.example.monitor.mapper.DeviceMonitorMapper;
import com.example.monitor.service.DeviceMonitorService;
import org.springframework.stereotype.Service;

/**
 * 设备监控服务实现类
 */
@Service
public class DeviceMonitorServiceImpl extends ServiceImpl<DeviceMonitorMapper, DeviceMonitor> implements DeviceMonitorService {

    @Override
    public DeviceMonitor getDeviceMonitorById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<DeviceMonitor>()
                .eq(DeviceMonitor::getId, id));
    }
}