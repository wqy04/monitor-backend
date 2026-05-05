package com.example.monitor.dto;

import lombok.Data;

/**
 * 应用信息DTO，包含数据库静态信息和Prometheus动态数据
 */
@Data
public class AppInfoDTO {
    /**
     * 应用ID
     */
    private Integer appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用描述
     */
    private String description;

    /**
     * 集群ID
     */
    private Integer clusterId;

    /**
     * 等待作业数 (jingxing_app_jobs_pending)
     */
    private Double jobsPending;

    /**
     * 运行作业数 (jingxing_app_jobs_running)
     */
    private Double jobsRunning;

    /**
     * 挂起作业数 (jingxing_app_jobs_suspended)
     */
    private Double jobsSuspended;
}