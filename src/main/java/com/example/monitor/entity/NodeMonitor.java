package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 节点监控数据表（静态）
 * 对应数据库表：node_monitor
 */
@Data
@TableName("node_monitor")
public class NodeMonitor {
    @TableId(type = IdType.AUTO)
    private Long nodeId;

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
    private String partition;

    /**
     * 状态：0-离线 1-在线
     */
    private Integer status;

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
     * 最后更新时间
     */
    private LocalDateTime lastUpdate;

    /**
     * 节点角色：0-登录节点 1-计算节点 2-存储节点
     */
    private Integer nodeRole;

    /**
     * 节点类型
     */
    private Integer nodeType;

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
    private Long clusterId;

    /**
     * IPMI专用IP
     */
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
     * 是否允许功耗采集：0-否 1-是
     */
    private Boolean powerSupported;

    /**
     * Prometheus metric名称
     */
    private String powerMetricName;
}