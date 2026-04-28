package com.example.monitor.service;

import java.util.List;
import java.util.Map;

/**
 * 队列信息服务接口
 */
public interface QueueInfoService {
    /**
     * 获取队列静态信息与 Prometheus 动态指标合并后的列表
     *
     * @return 队列信息列表
     */
    List<Map<String, Object>> listQueuesWithPrometheusMetrics();
}
