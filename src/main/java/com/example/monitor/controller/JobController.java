package com.example.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.monitor.dto.Result;
import com.example.monitor.entity.JobMonitor;
import com.example.monitor.service.JobMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JobController {

    @Autowired
    private JobMonitorService jobMonitorService;

    @GetMapping("/jobs")
    public Result<Map<String, Object>> listJobs(@RequestParam(value = "clusterId", required = false) Integer clusterId,
                                                @RequestParam(value = "status", required = false) String status,
                                                @RequestParam(value = "userId", required = false) Integer userId,
                                                @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {

        LambdaQueryWrapper<JobMonitor> wrapper = new LambdaQueryWrapper<>();
        if (clusterId != null) wrapper.eq(JobMonitor::getClusterId, clusterId);
        if (status != null) wrapper.eq(JobMonitor::getStatus, status);
        if (userId != null) wrapper.eq(JobMonitor::getUserId, userId);

        Page<JobMonitor> page = jobMonitorService.page(new Page<>(pageNum, pageSize), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("list", page.getRecords());
        return Result.ok(data);
    }

    @GetMapping("/jobs/{jobId}")
    public Result<JobMonitor> getJob(@PathVariable Integer jobId) {
        JobMonitor job = jobMonitorService.getById(jobId);
        if (job == null) {
            return Result.fail(404, "job not found");
        }
        return Result.ok(job);
    }

    @PostMapping("/jobs/submit")
    public Result<Map<String, Object>> submitJob(@RequestBody JobMonitor payload) {
        payload.setStatus("PENDING");
        payload.setSubmitTime(LocalDateTime.now());
        boolean saved = jobMonitorService.save(payload);

        if (!saved) {
            return Result.fail(500, "作业提交失败");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", payload.getJobId());
        data.put("externalJobId", payload.getExternalJobId() == null ? payload.getJobId() : payload.getExternalJobId());
        data.put("status", payload.getStatus());
        return Result.ok("作业提交成功", data);
    }

    @DeleteMapping("/jobs/{jobId}")
    public Result<Void> stopJob(@PathVariable Integer jobId) {
        JobMonitor job = jobMonitorService.getById(jobId);
        if (job == null) {
            return Result.fail(404, "job not found");
        }
        job.setStatus("CANCELLED");
        job.setEndTime(LocalDateTime.now());
        jobMonitorService.updateById(job);
        return Result.ok("作业已终止", null);
    }
}
