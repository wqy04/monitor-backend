package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.Cluster;

/**
 * 集群服务接口
 */
public interface ClusterService extends IService<Cluster> {
    // 可扩展自定义业务方法，例如：根据集群ID查询集群信息
    Cluster getClusterById(Long id);
}