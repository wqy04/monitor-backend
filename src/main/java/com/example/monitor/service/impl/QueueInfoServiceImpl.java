package com.example.monitor.service.impl;

import com.example.monitor.entity.Queue;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.example.monitor.service.PromQueryService;
import com.example.monitor.service.QueueInfoService;
import com.example.monitor.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
        Map<String, Integer> queuePriorityMap = fetchQueuePrioritiesFromPrometheus();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Queue queue : queues) {
            Map<String, Object> item = new HashMap<>();
            item.put("queueId", queue.getQueueId());
            item.put("queueName", queue.getQueueName());
            item.put("clusterId", queue.getClusterId());
            item.put("nice", queue.getNice());
            // 优先使用 Prometheus 中的优先级，若不存在则使用数据库值
            Integer priority = queuePriorityMap.getOrDefault(queue.getQueueName(), queue.getPriority());
            item.put("priority", priority);
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

    /**
     * 获取所有队列的 pending/running/suspended 指标（同时支持景行和 Slurm）
     * 注意：Slurm 指标中没有 suspended，该字段会保持默认 0
     */
    private Map<String, Map<String, Object>> fetchQueueMetrics() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        // 同时查询景行和 Slurm 的队列作业数指标
        // 景行：jingxing_queue_jobs_pending, _running, _suspended
        // Slurm：slurm_queue_jobs_pending, _running （无 _suspended）
        String query = "{__name__=~\"jingxing_queue_jobs_(pending|running|suspended)|slurm_queue_jobs_(pending|running)\"}";
        PromQueryData promQueryData = promQueryService.getQueryDataInfo(query, null);
        if (promQueryData == null || promQueryData.getResult() == null) {
            return result;
        }

        for (PromQueryResult promQueryResult : promQueryData.getResult()) {
            if (promQueryResult == null) continue;
            Map<String, Object> labels = promQueryResult.getMetric();
            if (labels == null) continue;

            String queueName = labels.get("queue") != null ? labels.get("queue").toString() : null;
            String metricName = labels.get("__name__") != null ? labels.get("__name__").toString() : null;
            if (queueName == null || metricName == null) continue;

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
            if (value == null || value.size() < 2) continue;

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

    /**
     * 从 Prometheus 中获取队列优先级（覆盖数据库值）
     * 查询 slurm_queue_info 和 jingxing_queue_info 指标中的 priority 标签
     */
    private Map<String, Integer> fetchQueuePrioritiesFromPrometheus() {
        Map<String, Integer> priorityMap = new HashMap<>();

        // 1. 获取 Slurm 队列优先级
        PromQueryData slurmData = promQueryService.getQueryDataInfo("slurm_queue_info", null);
        if (slurmData != null && slurmData.getResult() != null) {
            for (PromQueryResult result : slurmData.getResult()) {
                Map<String, Object> metric = result.getMetric();
                String queueName = metric.get("queue") != null ? metric.get("queue").toString() : null;
                Object priorityObj = metric.get("priority");
                if (queueName != null && priorityObj != null) {
                    try {
                        int priority = Integer.parseInt(priorityObj.toString());
                        priorityMap.put(queueName, priority);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // 2. 获取景行队列优先级
        PromQueryData jingxingData = promQueryService.getQueryDataInfo("jingxing_queue_info", null);
        if (jingxingData != null && jingxingData.getResult() != null) {
            for (PromQueryResult result : jingxingData.getResult()) {
                Map<String, Object> metric = result.getMetric();
                String queueName = metric.get("queue") != null ? metric.get("queue").toString() : null;
                Object priorityObj = metric.get("priority");
                if (queueName != null && priorityObj != null) {
                    try {
                        int priority = Integer.parseInt(priorityObj.toString());
                        priorityMap.put(queueName, priority);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return priorityMap;
    }
}