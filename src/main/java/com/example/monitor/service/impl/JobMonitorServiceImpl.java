package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.JobMonitor;
import com.example.monitor.entity.User;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.example.monitor.mapper.JobMonitorMapper;
import com.example.monitor.service.JobMonitorService;
import com.example.monitor.service.PromQueryService;
import com.example.monitor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作业监控服务实现类
 */
@Service
public class JobMonitorServiceImpl extends ServiceImpl<JobMonitorMapper, JobMonitor> implements JobMonitorService {

    @Autowired
    private PromQueryService promQueryService;

    @Autowired
    private UserService userService;

    @Override
    public JobMonitor getJobMonitorById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<JobMonitor>()
                .eq(JobMonitor::getJobId, id));
    }

    @Override
    public List<JobMonitor> getJobsFromPrometheus(Integer clusterId, String status, Integer userId) {
        List<JobMonitor> jobs = new ArrayList<>();

        PromQueryData jobInfoData = promQueryService.getQueryDataInfo("jingxing_job_info", null);
        if (jobInfoData == null || jobInfoData.getResult() == null) {
            return jobs;
        }

        for (PromQueryResult result : jobInfoData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String jobidStr = (String) metric.get("jobid");
            String jobName = (String) metric.get("job_name");
            String queue = (String) metric.get("queue");
            String username = (String) metric.get("user");
            String cwd = (String) metric.get("cwd");
            String fromHost = (String) metric.get("from_host");

            if (jobidStr == null) {
                continue;
            }

            // 过滤 status
            PromQueryData statusData = promQueryService.getQueryDataInfo("jingxing_jobs_total{jobid=\"" + jobidStr + "\"}", null);
            String promStatus = null;
            if (statusData != null && statusData.getResult() != null && !statusData.getResult().isEmpty()) {
                promStatus = (String) statusData.getResult().get(0).getMetric().get("status");
            }
            if (status != null && (promStatus == null || !status.equals(promStatus))) {
                continue;
            }

            // 过滤 userId
            if (userId != null) {
                User userEntity = userService.getById(userId);
                if (userEntity == null || !userEntity.getUsername().equals(username)) {
                    continue;
                }
            }

            // 过滤 clusterId：Prometheus 内没有 clusterId 标签，暂时忽略

            JobMonitor job = new JobMonitor();
            job.setJobId(Integer.parseInt(jobidStr));
            job.setJobName(jobName);
            job.setQueue(queue);
            job.setUsername(username);
            job.setCwd(cwd);
            job.setFromHost(fromHost);
            job.setStatus(promStatus);

            PromQueryData cpuData = promQueryService.getQueryDataInfo("jingxing_job_cpu_usage_seconds{jobid=\"" + jobidStr + "\"}", null);
            if (cpuData != null && cpuData.getResult() != null && !cpuData.getResult().isEmpty()) {
                PromQueryResult cpuResult = cpuData.getResult().get(0);
                job.setCpuUsageSeconds(Double.parseDouble(cpuResult.getValue().get(1)));
                job.setExecHost((String) cpuResult.getMetric().get("exec_host"));
            }

            PromQueryData cpuUtilData = promQueryService.getQueryDataInfo("jingxing_job_cpu_util_percent{jobid=\"" + jobidStr + "\"}", null);
            if (cpuUtilData != null && cpuUtilData.getResult() != null && !cpuUtilData.getResult().isEmpty()) {
                job.setCpuUtilPercent(Double.parseDouble(cpuUtilData.getResult().get(0).getValue().get(1)));
            }

            PromQueryData memData = promQueryService.getQueryDataInfo("jingxing_job_mem_used_bytes{jobid=\"" + jobidStr + "\"}", null);
            if (memData != null && memData.getResult() != null && !memData.getResult().isEmpty()) {
                job.setMemUsedBytes(Double.parseDouble(memData.getResult().get(0).getValue().get(1)));
            }

            PromQueryData idleData = promQueryService.getQueryDataInfo("jingxing_job_idle_rate{jobid=\"" + jobidStr + "\"}", null);
            if (idleData != null && idleData.getResult() != null && !idleData.getResult().isEmpty()) {
                job.setIdleRate(Double.parseDouble(idleData.getResult().get(0).getValue().get(1)));
            }

            PromQueryData runtimeData = promQueryService.getQueryDataInfo("jingxing_job_runtime_seconds{jobid=\"" + jobidStr + "\"}", null);
            if (runtimeData != null && runtimeData.getResult() != null && !runtimeData.getResult().isEmpty()) {
                job.setRuntimeSeconds(Double.parseDouble(runtimeData.getResult().get(0).getValue().get(1)));
            }

            jobs.add(job);
        }

        return jobs;
    }
}