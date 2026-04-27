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

    /**
     * Prometheus范围查询
     *
     * @param query      查询
     * @param start      开始时间戳, 单位: 秒
     * @param end        结束时间戳, 单位: 秒
     * @param step       步长, 单位: 秒
     * @return {@code PromQueryData}
     */
    PromQueryData getQueryRangeDataInfo(String query, String start, String end, String step);
}