package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 节点监控数据表（静态数据）
 * 对应数据库表：nodes
 */
@Data
@TableName("nodes")
public class NodeMonitor {
    @TableId(type = IdType.AUTO)
    private Integer nodeId;

    /**
     * 节点名
     */
    private String nodeName;

    /**
     * 节点IP
     */
    private String nodeIp;

    /**
     * 所属队列
     */
    @TableField(value = "`partition`")
    private String partition;

    /**
     * 总CPU核心数
     */
    private Integer cpuTotal;

    /**
     * 总内存（MB）
     */
    private Long memoryTotal;

    /**
     * 总磁盘空间（MB）
     */
    private Long diskTotal;

    /**
     * 节点角色：0-登录节点 1-计算节点 2-存储节点
     */
    private Integer nodeRole;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * CPU型号/架构
     */
    private String cpuModel;

    /**
     * 操作系统类型
     */
    private String osType;

    /**
     * 最大作业槽数（可能不等于CPU核数）
     */
    private Integer slotsMax;

    /**
     * 整机型号（如有）
     */
    private String nodeModel;

    /**
     * GPU型号
     */
    private String gpuModel;

    /**
     * GPU卡数
     */
    private Integer gpuCount;

    /**
     * GPU总显存（MB）
     */
    private Long gpuMemoryTotal;

    /**
     * 所属集群ID
     */
    private Integer clusterId;

    /**
     * IPMI专用IP地址
     */
    @TableField(value = "ipmi_ip")
    private String ipmiIP;

    /**
     * IPMI用户名
     */
    private String ipmiUser;

    /**
     * IPMI加密密码
     */
    private String ipmiPassword;

    /**
     * 是否支持功耗采集：0-否，1-是
     */
    private Integer powerSupported;

    /**
     * Prometheus功耗指标名称
     */
    private String powerMetricName;
}