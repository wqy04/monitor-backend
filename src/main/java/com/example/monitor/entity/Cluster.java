package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 集群表实体
 * 对应数据库表：cluster
 */
@Data
@TableName("cluster")
public class Cluster {
    /**
     * 唯一主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long clusterId;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 集群描述
     */
    private String description;
}