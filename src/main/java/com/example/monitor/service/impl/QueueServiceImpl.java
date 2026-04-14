package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.Queue;
import com.example.monitor.mapper.QueueMapper;
import com.example.monitor.service.QueueService;
import org.springframework.stereotype.Service;

/**
 * 队列服务实现
 */
@Service
public class QueueServiceImpl extends ServiceImpl<QueueMapper, Queue> implements QueueService {

    @Override
    public Queue findByQueueNameAndClusterId(String queueName, Integer clusterId) {
        QueryWrapper<Queue> wrapper = new QueryWrapper<>();
        wrapper.eq("queue_name", queueName).eq("cluster_id", clusterId);
        return getOne(wrapper);
    }
}