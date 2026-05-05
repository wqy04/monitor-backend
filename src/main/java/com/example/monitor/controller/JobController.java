package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.JobMonitor;
import com.example.monitor.service.JobMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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

        List<JobMonitor> jobs = jobMonitorService.getJobsFromPrometheus(clusterId, status, userId);

        // 手动分页
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, jobs.size());
        List<JobMonitor> pageList = jobs.subList(start, end);

        Map<String, Object> data = new HashMap<>();
        data.put("total", jobs.size());
        data.put("list", pageList);
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
        boolean saved = jobMonitorService.save(payload);

        if (!saved) {
            return Result.fail(500, "作业提交失败");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", payload.getJobId());
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
        jobMonitorService.updateById(job);
        return Result.ok("作业已终止", null);
    }
}
