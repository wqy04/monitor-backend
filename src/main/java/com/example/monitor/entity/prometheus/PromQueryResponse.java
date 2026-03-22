package com.example.monitor.entity.prometheus;

import lombok.Data;

/**
 * Prometheus查询响应对象
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Data
public class PromQueryResponse {
    /**
     * 状态
     * 成功-- success
     */
    private String status;

    /**
     * prometheus指标属性和值
     */
    private PromQueryData data;
}