package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则表实体
 * 对应数据库表：alert_rules
 */
@Data
@TableName("alert_rules")
public class AlertRule {
    /**
     * 规则ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Integer ruleId;

    /**
     * 规则名称，如GPU温度过高
     */
    private String ruleName;

    /**
     * 告警类型：threshold（阈值告警）/ status（状态变化）
     */
    private String ruleType;

    /**
     * 目标类型：node / device / job
     */
    private String targetType;

    /**
     * 监控指标名（阈值告警时使用）
     */
    private String metricName;

    /**
     * 条件表达式，如 >、<、>=、<=
     */
    private String condition;

    /**
     * 阈值，如 85、5000
     */
    private BigDecimal threshold;

    /**
     * 持续时间（秒），如持续60秒才触发
     */
    private Integer duration;

    /**
     * 告警级别 0/1/2/3
     */
    private Integer level;

    /**
     * 0:禁用 1:启用
     */
    private Integer enabled;

    /**
     * 通知方式（JSON或逗号分隔），如站内消息、邮件
     */
    private String notifyMethods;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 作用范围：cluster / node / global
     */
    private String scopeType;

    /**
     * 对应集群/节点的ID（当scope_type为cluster或node时使用）
     */
    private Integer scopeId;
}