package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.JobScheduler;

/**
 * 作业调度服务接口
 */
public interface JobSchedulerService extends IService<JobScheduler> {
    // 可扩展自定义业务方法，例如：根据调度ID查询作业调度信息
    JobScheduler getJobSchedulerById(Long id);
}