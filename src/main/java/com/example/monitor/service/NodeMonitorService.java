package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.NodeMonitor;

/**
 * 节点监控服务接口
 */
public interface NodeMonitorService extends IService<NodeMonitor> {
    // 可扩展自定义业务方法，例如：根据节点ID查询节点监控信息
    NodeMonitor getNodeMonitorById(Long id);
}