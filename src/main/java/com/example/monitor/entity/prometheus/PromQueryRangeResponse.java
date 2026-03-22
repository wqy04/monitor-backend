package com.example.monitor.entity.prometheus;

import lombok.Data;

/**
 * Prometheus范围区间查询响应对象
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Data
public class PromQueryRangeResponse {
    /**
     * 状态
     * 成功-- success
     */
    private String status;

    /**
     * prometheus范围查询指标属性和值
     */
    private PromQueryRangeData data;
}