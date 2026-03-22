package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.Cluster;
import com.example.monitor.mapper.ClusterMapper;
import com.example.monitor.service.ClusterService;
import org.springframework.stereotype.Service;

/**
 * 集群服务实现类
 */
@Service
public class ClusterServiceImpl extends ServiceImpl<ClusterMapper, Cluster> implements ClusterService {

    @Override
    public Cluster getClusterById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Cluster>()
                .eq(Cluster::getClusterId, id));
    }
}