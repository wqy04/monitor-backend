package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 集群表实体
 * 对应数据库表：clusters
 */
@Data
@TableName("clusters")
public class Cluster {
    /**
     * 集群ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Integer clusterId;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 厂商：曙光/浪潮/联想等
     */
    private String vendor;

    /**
     * 集群描述
     */
    private String description;
}