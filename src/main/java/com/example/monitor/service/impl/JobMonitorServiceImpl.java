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
import java.util.stream.Collectors;

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
        List<JobMonitor> allJobs = new ArrayList<>();
        // 1. 获取景行调度器作业
        allJobs.addAll(getJingxingJobsFromPrometheus());
        // 2. 获取 Slurm 作业
        allJobs.addAll(getSlurmJobsFromPrometheus());

        // 统一过滤 status 和 userId
        List<JobMonitor> filtered = allJobs.stream()
                .filter(job -> {
                    if (status != null && !status.equals(job.getStatus())) {
                        return false;
                    }
                    if (userId != null) {
                        User user = userService.getById(userId);
                        if (user == null || !user.getUsername().equals(job.getUsername())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return filtered;
    }

    /**
     * 获取景行调度器的作业数据
     */
    private List<JobMonitor> getJingxingJobsFromPrometheus() {
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

            if (jobidStr == null) continue;

            // 获取作业状态
            String status = null;
            PromQueryData statusData = promQueryService.getQueryDataInfo("jingxing_jobs_total{jobid=\"" + jobidStr + "\"}", null);
            if (statusData != null && statusData.getResult() != null && !statusData.getResult().isEmpty()) {
                status = (String) statusData.getResult().get(0).getMetric().get("status");
            }

            JobMonitor job = new JobMonitor();
            job.setJobId(Integer.parseInt(jobidStr));
            job.setJobName(jobName);
            job.setQueue(queue);
            job.setUsername(username);
            job.setCwd(cwd);
            job.setFromHost(fromHost);
            job.setStatus(status);

            // 其他指标
            fillJingxingMetrics(job, jobidStr);
            jobs.add(job);
        }
        return jobs;
    }

    /**
     * 填充景行作业的扩展指标（CPU、内存、运行时间等）
     */
    private void fillJingxingMetrics(JobMonitor job, String jobidStr) {
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
    }

    /**
     * 获取 Slurm 作业数据
     */
    private List<JobMonitor> getSlurmJobsFromPrometheus() {
        List<JobMonitor> jobs = new ArrayList<>();
        PromQueryData jobInfoData = promQueryService.getQueryDataInfo("slurm_job_info", null);
        if (jobInfoData == null || jobInfoData.getResult() == null) {
            return jobs;
        }

        for (PromQueryResult result : jobInfoData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String jobidStr = (String) metric.get("jobid");
            String jobName = (String) metric.get("job_name");
            String queue = (String) metric.get("queue");
            String username = (String) metric.get("user");
            String state = (String) metric.get("state");   // PENDING, RUNNING等

            if (jobidStr == null) continue;

            JobMonitor job = new JobMonitor();
            job.setJobId(Integer.parseInt(jobidStr));
            job.setJobName(jobName);
            job.setQueue(queue);
            job.setUsername(username);
            job.setStatus(state);   // Slurm 状态直接作为 status
            // Slurm 中没有 cwd 和 fromHost，设为 null
            job.setCwd(null);
            job.setFromHost(null);

            // 填充 Slurm 的其他指标
            fillSlurmMetrics(job, jobidStr);
            jobs.add(job);
        }
        return jobs;
    }

    /**
     * 填充 Slurm 作业的扩展指标
     */
    private void fillSlurmMetrics(JobMonitor job, String jobidStr) {
        // CPU 使用秒数
        PromQueryData cpuUsageData = promQueryService.getQueryDataInfo("slurm_job_cpu_usage_seconds{jobid=\"" + jobidStr + "\"}", null);
        if (cpuUsageData != null && cpuUsageData.getResult() != null && !cpuUsageData.getResult().isEmpty()) {
            PromQueryResult result = cpuUsageData.getResult().get(0);
            job.setCpuUsageSeconds(Double.parseDouble(result.getValue().get(1)));
            // 尝试获取执行主机
            Object execHost = result.getMetric().get("exec_host");
            if (execHost != null) job.setExecHost((String) execHost);
        } else {
            // 如果 slurm_job_cpu_usage_seconds 没有数据，不设置 execHost
        }

        // 运行时长
        PromQueryData runtimeData = promQueryService.getQueryDataInfo("slurm_job_runtime_seconds{jobid=\"" + jobidStr + "\"}", null);
        if (runtimeData != null && runtimeData.getResult() != null && !runtimeData.getResult().isEmpty()) {
            job.setRuntimeSeconds(Double.parseDouble(runtimeData.getResult().get(0).getValue().get(1)));
        }

        // Slurm 的指标中没有 cpuUtilPercent, memUsedBytes, idleRate，保持 null
        // 注意：可能有 slurm_job_mem_max_bytes（已完成作业），但当前作业可能未完成，暂时不取
    }
}