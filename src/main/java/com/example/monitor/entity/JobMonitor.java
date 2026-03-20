package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 作业监控数据表
 * 对应数据库表：job_monitor
 */
@Data
@TableName("job_monitor")
public class JobMonitor {
    @TableId(type = IdType.AUTO)
    private Long jobId;

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * 所属队列
     */
    private String queue;

    /**
     * 队列名称/分区
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
    private Long schedulerId;

    /**
     * 调度器生成的作业ID
     */
    private Long externalJobId;

    /**
     * 运行集群ID
     */
    private Long clusterId;

    /**
     * 关联用户ID
     */
    private Long userId;

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
    private Integer cpuCoresHours;

    /**
     * 申请GPU核数
     */
    private Integer gpuCores;

    /**
     * GPU型号
     */
    private String gpuModel;

    /**
     * GPU核时
     */
    private Integer gpuCoresHours;

    /**
     * 应用软件名称
     */
    private String appName;
}