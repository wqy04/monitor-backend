package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.App;

/**
 * 应用服务接口
 */
public interface AppService extends IService<App> {
    App findByAppNameAndClusterId(String appName, Integer clusterId);
}
