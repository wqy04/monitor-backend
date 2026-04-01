package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业监控数据表
 * 对应数据库表：jobs
 */
@Data
@TableName("jobs")
public class JobMonitor {
    @TableId(type = IdType.AUTO)
    private Integer jobId;

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * 所属队列
     */
    private String queue;

    /**
     * 队列名称（或分区）
     */
    private String partition;

    /**
     * 作业状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED
     */
    private String status;

    /**
     * 作业描述
     */
    private String description;

    /**
     * 调度器ID
     */
    private Integer schedulerId;

    /**
     * 调度器生成的作业ID
     */
    private Integer externalJobId;

    /**
     * 运行集群ID
     */
    private Integer clusterId;

    /**
     * 提交用户ID
     */
    private Integer userId;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 开始运行时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 运行时长（秒）
     */
    private Long elapsed;

    /**
     * 申请节点数
     */
    private Integer numNodes;

    /**
     * 工作目录
     */
    private String workDir;

    /**
     * 申请CPU核数
     */
    private Integer cpuCores;

    /**
     * CPU核时
     */
    private BigDecimal cpuCoresHours;

    /**
     * 申请GPU核数
     */
    private Integer gpuCores;

    /**
     * 申请GPU型号
     */
    private String gpuModel;

    /**
     * GPU核时
     */
    private BigDecimal gpuCoresHours;

    /**
     * 应用软件名称
     */
    private String appName;
}