package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Prometheus 作业信息
 * 对应数据库表：jobs
 */
@Data
@TableName("jobs")
public class JobMonitor {
    @TableId(type = IdType.INPUT)
    private Integer jobId;

    /**
     * 作业名称
     */
    private String jobName;

    /**
     * 作业队列
     */
    private String queue;

    /**
     * 作业用户名
     */
    private String username;

    /**
     * 作业工作目录
     */
    private String cwd;

    /**
     * 作业提交主机
     */
    private String fromHost;

    /**
     * 作业执行主机
     */
    private String execHost;

    /**
     * 作业状态
     */
    private String status;

    /**
     * CPU 使用秒数
     */
    private Double cpuUsageSeconds;

    /**
     * CPU 利用率
     */
    private Double cpuUtilPercent;

    /**
     * 内存使用字节
     */
    private Double memUsedBytes;

    /**
     * 空闲率
     */
    private Double idleRate;

    /**
     * 运行时间秒
     */
    private Double runtimeSeconds;
}