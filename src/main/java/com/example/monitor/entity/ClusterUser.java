package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 集群作业系统用户表实体
 * 对应数据库表：cluster_users
 */
@Data
@TableName("cluster_users")
public class ClusterUser {
    @TableId(type = IdType.AUTO)
    private Integer clusterUserId;

    private String username;

    private Integer clusterId;
}
