package com.example.monitor.entity.prometheus;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Prometheus时序对象
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Data
public class PromSeries {
    /**
     * 状态
     * 成功-- success
     */
    private String status;

    /**
     * 时序数据列表
     */
    private List<LinkedHashMap<String, Object>> data;
}