package com.example.monitor.service;

import com.example.monitor.entity.prometheus.PromQueryRangeData;
import com.example.monitor.entity.prometheus.PromQueryRange;

/**
 * 指标查询接口类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
public interface PromQueryRangeService {

    /**
     * Prometheus范围区间查询
     *
     * @param queryRangeDto 查询范围类
     * @return {@code PromQueryRangeData}
     */
    PromQueryRangeData getQueryRangeDataInfo(PromQueryRange queryRange);
}