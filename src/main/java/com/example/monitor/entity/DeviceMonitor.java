package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备监控表实体
 * 对应数据库表：device_monitor
 */
@Data
@TableName("device_monitor")
public class DeviceMonitor {
    @TableId(type = IdType.AUTO)
    private Long deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备IP
     */
    private String deviceIp;

    /**
     * 状态：0-离线 1-在线
     */
    private Integer status;

    /**
     * 最近采集时间
     */
    private LocalDateTime lastUpdate;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 所属集群ID
     */
    private Long clusterId;

    /**
     * 外部监控链接
     */
    private String monitorUrl;
}