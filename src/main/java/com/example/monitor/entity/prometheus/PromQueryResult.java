package com.example.monitor.entity.prometheus;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Prometheus查询结果对象信息
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Data
public class PromQueryResult {
    /**
     * prometheus指标属性
     */
    private Map<String, Object> metric;

    /**
     * prometheus即时查询指标值
     */
    private List<String> value;

    /**
     * prometheus范围查询指标值列表
     */
    private List<List<String>> values;
}