package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.DeviceMonitor;

/**
 * 设备监控服务接口
 */
public interface DeviceMonitorService extends IService<DeviceMonitor> {
    // 可扩展自定义业务方法，例如：根据设备ID查询设备监控信息
    DeviceMonitor getDeviceMonitorById(Long id);
}