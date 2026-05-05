package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.JobMonitor;

import java.util.List;

/**
 * 作业监控服务接口
 */
public interface JobMonitorService extends IService<JobMonitor> {
    // 可扩展自定义业务方法，例如：根据作业ID查询作业监控信息
    JobMonitor getJobMonitorById(Long id);

    /**
     * 从 Prometheus 获取作业列表
     * @param clusterId 集群ID
     * @param status 状态
     * @param userId 用户ID
     * @return 作业列表
     */
    List<JobMonitor> getJobsFromPrometheus(Integer clusterId, String status, Integer userId);
}