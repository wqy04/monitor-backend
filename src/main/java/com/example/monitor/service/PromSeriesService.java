package com.example.monitor.service;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 指标查询接口类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
public interface PromSeriesService {

    /**
     * 获取时序数据
     *
     * @param start      开始时间戳, 单位：秒
      * @param end        结束时间戳, 单位：秒
     * @param match      查询指标
     * @param datasource 数据源（可选）
     * @return {@code List<LinkedHashMap<String, Object>>}
     */
    List<LinkedHashMap<String, Object>> getSeriesList(String start, String end, String match, Integer datasource);
}