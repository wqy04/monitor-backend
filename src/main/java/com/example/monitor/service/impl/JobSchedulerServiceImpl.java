package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.JobScheduler;
import com.example.monitor.mapper.JobSchedulerMapper;
import com.example.monitor.service.JobSchedulerService;
import org.springframework.stereotype.Service;

/**
 * 作业调度服务实现类
 */
@Service
public class JobSchedulerServiceImpl extends ServiceImpl<JobSchedulerMapper, JobScheduler> implements JobSchedulerService {

    @Override
    public JobScheduler getJobSchedulerById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<JobScheduler>()
                .eq(JobScheduler::getSchedulerId, id));
    }

    @Override
    public JobScheduler findByClusterId(Integer clusterId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<JobScheduler>()
                .eq(JobScheduler::getClusterId, clusterId));
    }
}