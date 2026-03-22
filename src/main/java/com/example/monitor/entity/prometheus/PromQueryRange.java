package com.example.monitor.entity.prometheus;

import lombok.Data;

/**
 * Prometheus范围查询实体类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Data
public class PromQueryRange {
    /**
     * 查询指标
     */
    private String query;

    /**
     * 区间范围查询开始时间
     * 格式为：时分秒时间戳
     */
    private String start;

    /**
     * 区间范围查询结束时间
     * 格式为：时分秒时间戳
     */
    private String end;

    /**
     * 时间区间步长, 即：时间间隔
     */
    private Integer step;
}
