package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.ClusterUser;

/**
 * 集群用户服务接口
 */
public interface ClusterUserService extends IService<ClusterUser> {
    ClusterUser findByUsernameAndClusterId(String username, Integer clusterId);
}
