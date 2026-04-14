package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.NodeQueue;

/**
 * 节点队列关联服务接口
 */
public interface NodeQueueService extends IService<NodeQueue> {
    NodeQueue findByNodeIdAndQueueName(Integer nodeId, String queueName);
}
