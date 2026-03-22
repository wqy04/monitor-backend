package com.example.monitor.entity.prometheus;

import lombok.Data;

import java.util.List;

/**
 * Prometheus查询数据结果对象
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Data
public class PromQueryData {
    /**
     * prometheus结果类型
     * vector--瞬时向量
     * matrix--区间向量
     * scalar--标量
     * string--字符串
     */
    private String resultType;

    /**
     * prometheus指标属性和值
     */
    private List<PromQueryResult> result;
}