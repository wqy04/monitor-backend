package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.NodeQueue;
import com.example.monitor.mapper.NodeQueueMapper;
import com.example.monitor.service.NodeQueueService;
import org.springframework.stereotype.Service;

/**
 * 节点队列关联服务实现类
 */
@Service
public class NodeQueueServiceImpl extends ServiceImpl<NodeQueueMapper, NodeQueue> implements NodeQueueService {

    @Override
    public NodeQueue findByNodeIdAndQueueName(Integer nodeId, String queueName) {
        return baseMapper.selectOne(new LambdaQueryWrapper<NodeQueue>()
                .eq(NodeQueue::getNodeId, nodeId)
                .eq(NodeQueue::getQueueName, queueName));
    }
}
