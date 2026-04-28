package com.example.monitor.service.impl;

import com.example.monitor.entity.Queue;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.example.monitor.service.PromQueryService;
import com.example.monitor.service.QueueInfoService;
import com.example.monitor.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 队列信息服务实现
 */
@Service
public class QueueInfoServiceImpl implements QueueInfoService {

    @Autowired
    private QueueService queueService;

    @Autowired
    private PromQueryService promQueryService;

    @Override
    public List<Map<String, Object>> listQueuesWithPrometheusMetrics() {
        List<Queue> queues = queueService.list();
        Map<String, Map<String, Object>> queueMetricMap = fetchQueueMetrics();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Queue queue : queues) {
            Map<String, Object> item = new HashMap<>();
            item.put("queueId", queue.getQueueId());
            item.put("queueName", queue.getQueueName());
            item.put("clusterId", queue.getClusterId());
            item.put("nice", queue.getNice());
            item.put("priority", queue.getPriority());
            item.put("maxSlots", queue.getMaxSlots());
            item.put("status", queue.getStatus());
            item.put("description", queue.getDescription());

            Map<String, Object> prometheusMetrics = queueMetricMap.getOrDefault(queue.getQueueName(), Collections.emptyMap());
            item.put("jobsPending", prometheusMetrics.getOrDefault("pending", 0.0));
            item.put("jobsRunning", prometheusMetrics.getOrDefault("running", 0.0));
            item.put("jobsSuspended", prometheusMetrics.getOrDefault("suspended", 0.0));

            result.add(item);
        }
        return result;
    }

    private Map<String, Map<String, Object>> fetchQueueMetrics() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        PromQueryData promQueryData = promQueryService.getQueryDataInfo("{__name__=~\"jingxing_queue_jobs_(pending|running|suspended)\"}", null);
        if (promQueryData == null || promQueryData.getResult() == null) {
            return result;
        }

        for (PromQueryResult promQueryResult : promQueryData.getResult()) {
            if (promQueryResult == null) {
                continue;
            }
            Map<String, Object> labels = promQueryResult.getMetric();
            if (labels == null) {
                continue;
            }

            String queueName = labels.get("queue") != null ? labels.get("queue").toString() : null;
            String metricName = labels.get("__name__") != null ? labels.get("__name__").toString() : null;
            if (queueName == null || metricName == null) {
                continue;
            }

            String metricKey;
            if (metricName.endsWith("_pending")) {
                metricKey = "pending";
            } else if (metricName.endsWith("_running")) {
                metricKey = "running";
            } else if (metricName.endsWith("_suspended")) {
                metricKey = "suspended";
            } else {
                continue;
            }

            List<String> value = promQueryResult.getValue();
            if (value == null || value.size() < 2) {
                continue;
            }

            Double metricValue;
            try {
                metricValue = Double.parseDouble(value.get(1));
            } catch (NumberFormatException ignored) {
                continue;
            }

            Map<String, Object> queueMetrics = result.computeIfAbsent(queueName, k -> new HashMap<>());
            queueMetrics.put(metricKey, metricValue);
        }
        return result;
    }
}
