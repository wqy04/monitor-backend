package com.example.monitor.controller;

import com.example.monitor.dto.AppInfoDTO;
import com.example.monitor.dto.Result;
import com.example.monitor.entity.App;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.example.monitor.service.AppService;
import com.example.monitor.service.PromQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用管理控制器
 */
@RestController
@RequestMapping("/api")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private PromQueryService promQueryService;

    /**
     * 获取所有应用信息（包含Prometheus动态数据）
     */
    @GetMapping("/apps")
    public Result<List<AppInfoDTO>> getAllApps() {
        // 获取所有应用
        List<App> apps = appService.list();
        
        // 从Prometheus获取三种指标数据
        Map<String, Double> pendingData = getMetricData("jingxing_app_jobs_pending");
        Map<String, Double> runningData = getMetricData("jingxing_app_jobs_running");
        Map<String, Double> suspendedData = getMetricData("jingxing_app_jobs_suspended");
        
        // 组装应用信息DTO
        List<AppInfoDTO> appInfoList = apps.stream().map(app -> {
            AppInfoDTO dto = new AppInfoDTO();
            dto.setAppId(app.getAppId());
            dto.setAppName(app.getAppName());
            dto.setDescription(app.getDescription());
            dto.setClusterId(app.getClusterId());
            
            // 从Prometheus数据中获取对应应用的动态数据
            String appName = app.getAppName();
            dto.setJobsPending(pendingData.getOrDefault(appName, 0.0));
            dto.setJobsRunning(runningData.getOrDefault(appName, 0.0));
            dto.setJobsSuspended(suspendedData.getOrDefault(appName, 0.0));
            
            return dto;
        }).collect(Collectors.toList());
        
        return Result.ok("获取成功", appInfoList);
    }
    
    /**
     * 从Prometheus获取指标数据
     */
    private Map<String, Double> getMetricData(String metricName) {
        Map<String, Double> result = new HashMap<>();
        try {
            PromQueryData queryData = promQueryService.getQueryDataInfo(metricName, null);
            if (queryData != null && queryData.getResult() != null) {
                for (PromQueryResult pr : queryData.getResult()) {
                    Object appObj = pr.getMetric().get("app");
                    if (appObj != null) {
                        String appName = appObj.toString();
                        // value[1] 是指标值
                        if (pr.getValue() != null && pr.getValue().size() > 1) {
                            try {
                                double value = Double.parseDouble(pr.getValue().get(1));
                                result.put(appName, value);
                            } catch (NumberFormatException e) {
                                result.put(appName, 0.0);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果Prometheus查询失败，返回空数据
            return result;
        }
        return result;
    }

    /**
     * 根据ID获取应用信息
     */
    @GetMapping("/apps/{id}")
    public Result<App> getAppById(@PathVariable("id") Integer id) {
        App app = appService.getById(id);
        if (app != null) {
            return Result.ok("获取成功", app);
        }
        return Result.fail(404, "应用不存在");
    }

    /**
     * 根据集群ID获取应用列表
     */
    @GetMapping("/apps/cluster/{clusterId}")
    public Result<List<App>> getAppsByClusterId(@PathVariable("clusterId") Integer clusterId) {
        List<App> apps = appService.list();
        return Result.ok("获取成功", apps);
    }

    /**
     * 创建新应用
     */
    @PostMapping("/apps")
    public Result<App> createApp(@RequestBody App app) {
        boolean success = appService.save(app);
        if (success) {
            return Result.ok("注册成功", app);
        }
        return Result.fail("创建失败");
    }

    /**
     * 更新应用信息
     */
    @PutMapping("/apps/{id}")
    public Result<App> updateApp(@PathVariable("id") Integer id, @RequestBody App app) {
        app.setAppId(id);
        boolean success = appService.updateById(app);
        if (success) {
            return Result.ok("更新成功", app);
        }
        return Result.fail("更新失败");
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/apps/{id}")
    public Result<Void> deleteApp(@PathVariable("id") Integer id) {
        boolean success = appService.removeById(id);
        if (success) {
            return Result.ok("删除成功", null);
        }
        return Result.fail("删除失败");
    }
}