package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.App;
import com.example.monitor.mapper.AppMapper;
import com.example.monitor.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用服务实现类
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Override
    public App findByAppNameAndClusterId(String appName, Integer clusterId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<App>()
                .eq(App::getAppName, appName)
                .eq(App::getClusterId, clusterId));
    }
}
