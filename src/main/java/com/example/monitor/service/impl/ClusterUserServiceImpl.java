package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.ClusterUser;
import com.example.monitor.mapper.ClusterUserMapper;
import com.example.monitor.service.ClusterUserService;
import org.springframework.stereotype.Service;

/**
 * 集群用户服务实现类
 */
@Service
public class ClusterUserServiceImpl extends ServiceImpl<ClusterUserMapper, ClusterUser> implements ClusterUserService {

    @Override
    public ClusterUser findByUsernameAndClusterId(String username, Integer clusterId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<ClusterUser>()
                .eq(ClusterUser::getUsername, username)
                .eq(ClusterUser::getClusterId, clusterId));
    }
}
