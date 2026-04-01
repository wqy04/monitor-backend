package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警信息表实体
 * 对应数据库表：alerts
 */
@Data
@TableName("alerts")
public class AlarmInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 告警消息内容
     */
    private String notice;

    /**
     * 告警触发时间
     */
    private LocalDateTime updateTime;

    /**
     * 触发告警的节点名称
     */
    private String target;

    /**
     * 告警级别：0-1-2-3，四级
     */
    private Integer level;

    /**
     * 处理状态：0-未解决，1-已确认，2-已解决
     */
    private Integer status;
}