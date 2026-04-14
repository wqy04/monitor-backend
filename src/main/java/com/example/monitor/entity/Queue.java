package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 队列静态信息实体
 */
@Data
@TableName("queues")
public class Queue {
    @TableId(type = IdType.AUTO)
    private Integer queueId;

    private String queueName;

    private Integer clusterId;

    private Integer nice;

    private Integer priority;

    private Integer maxSlots;

    private String status;

    private String description;
}