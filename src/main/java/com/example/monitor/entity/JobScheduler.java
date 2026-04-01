package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 作业调度器表实体
 * 对应数据库表：job_schedulers
 */
@Data
@TableName("job_schedulers")
public class JobScheduler {
    @TableId(type = IdType.AUTO)
    private Integer schedulerId;

    /**
     * 作业调度器名称
     */
    private String schedulerName;

    /**
     * 状态：0-运行中，1-未启用，2-停止中
     */
    private Integer status;

    /**
     * 连接端口
     */
    private Integer port;

    /**
     * 认证方式/密钥
     */
    private String authType;

    /**
     * 所属集群ID
     */
    private Integer clusterId;
}