package com.example.monitor.service;

import com.example.monitor.entity.prometheus.PromQueryData;

/**
 * 指标查询接口类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
public interface PromQueryService {

    /**
     * Prometheus即时查询
     *
     * @param query      查询
     * @param time       时间戳, 单位: 秒
     * @return {@code PromQueryData}
     */
    PromQueryData getQueryDataInfo(String query, String time);
}