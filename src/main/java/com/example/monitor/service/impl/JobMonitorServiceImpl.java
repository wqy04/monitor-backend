package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.JobMonitor;
import com.example.monitor.mapper.JobMonitorMapper;
import com.example.monitor.service.JobMonitorService;
import org.springframework.stereotype.Service;

/**
 * 作业监控服务实现类
 */
@Service
public class JobMonitorServiceImpl extends ServiceImpl<JobMonitorMapper, JobMonitor> implements JobMonitorService {

    @Override
    public JobMonitor getJobMonitorById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<JobMonitor>()
                .eq(JobMonitor::getJobId, id));
    }
}