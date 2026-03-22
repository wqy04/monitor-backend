package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.NodeMonitor;
import com.example.monitor.mapper.NodeMonitorMapper;
import com.example.monitor.service.NodeMonitorService;
import org.springframework.stereotype.Service;

/**
 * 节点监控服务实现类
 */
@Service
public class NodeMonitorServiceImpl extends ServiceImpl<NodeMonitorMapper, NodeMonitor> implements NodeMonitorService {

    @Override
    public NodeMonitor getNodeMonitorById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<NodeMonitor>()
                .eq(NodeMonitor::getNodeId, id));
    }
}