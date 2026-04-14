package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.Queue;

/**
 * 队列服务接口
 */
public interface QueueService extends IService<Queue> {
    Queue findByQueueNameAndClusterId(String queueName, Integer clusterId);
}