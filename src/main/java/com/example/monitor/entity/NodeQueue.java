package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 节点与队列关联表实体
 * 对应数据库表：node_queues
 */
@Data
@TableName("node_queues")
public class NodeQueue {
    private Integer nodeId;

    private String queueName;
}
