package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.Cluster;
import com.example.monitor.entity.NodeMonitor;
import com.example.monitor.service.ClusterService;
import com.example.monitor.service.NodeMonitorService;
import com.example.monitor.service.PromQueryService;
import com.example.monitor.service.PromQueryService;
import com.example.monitor.utils.RestTemplateUtils;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.date.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private NodeMonitorService nodeMonitorService;

    @Autowired
    private PromQueryService promQueryService;

    @Autowired
    private RestTemplateUtils restTemplateUtils;

    // 定义与数据库静态字段重复的 Prometheus 指标键名（节点维度）
    private static final Set<String> EXCLUDED_NODE_KEYS = Set.of(
        "mem_total_bytes",   // 对应数据库 memoryTotal
        "cpu_cores",         // 对应数据库 cpuTotal
        "slots_max",         // 最大作业槽，数据库无直接对应但通常等于 cpuTotal，为避免冗余排除
        "gpu_total",         // 对应数据库 gpuCount
        "model_info"         // 静态型号信息，无变化
    );

    // 定义与数据库静态字段重复的 GPU 指标键名
    private static final Set<String> EXCLUDED_GPU_KEYS = Set.of(
        "mem_total_bytes"    // 对应数据库 gpuMemoryTotal（单卡显存）
    );

    @GetMapping("/clusters")
    public Result<Map<String, Object>> listClusters() {
        List<Cluster> clusters = clusterService.list();
        List<NodeMonitor> allNodes = nodeMonitorService.list();

        // 查询 Prometheus 中所有 jingxing_node_* 指标，解析 status/slots_used/cpu_util/mem_free
        Map<String, Map<String, Object>> promNodeMap = new HashMap<>();
        PromQueryData allNodeMetrics = promQueryService.getQueryDataInfo("{__name__=~\"jingxing_node_.*\"}", null);
        if (allNodeMetrics != null && allNodeMetrics.getResult() != null) {
            for (PromQueryResult res : allNodeMetrics.getResult()) {
                Map<String, Object> labels = res.getMetric();
                String host = labels.get("host") != null ? labels.get("host").toString() : null;
                if (host == null) continue;
                String metricName = labels.get("__name__") != null ? labels.get("__name__").toString() : null;
                if (metricName == null || !metricName.startsWith("jingxing_node_")) continue;
                String key = metricName.substring("jingxing_node_".length());
                List<String> valueList = res.getValue();
                if (valueList == null || valueList.size() < 2) continue;
                double val;
                try {
                    val = Double.parseDouble(valueList.get(1));
                } catch (NumberFormatException e) {
                    continue;
                }
                Map<String, Object> nodeInfo = promNodeMap.computeIfAbsent(host, k -> new HashMap<>());
                if ("status".equals(key)) {
                    nodeInfo.put("status", val == 1.0 ? "ok" : "unavail");
                } else if ("slots_used".equals(key)) {
                    nodeInfo.put("slotsUsed", val);
                } else if ("cpu_util_percent".equals(key)) {
                    nodeInfo.put("cpuUtil", val);
                } else if ("mem_free_bytes".equals(key)) {
                    nodeInfo.put("memFree", val);
                }
            }
        }

        // 统计每个集群的节点总数
        Map<Integer, Long> clusterNodeCount = allNodes.stream()
                .collect(Collectors.groupingBy(NodeMonitor::getClusterId, Collectors.counting()));

        List<Map<String, Object>> clusterList = new ArrayList<>();
        // 全局汇总统计
        int totalClusterCount = clusters.size();
        long totalCpuCores = 0;
        long totalGpuCount = 0;
        long totalSlotsMax = 0;
        double totalSlotsUsed = 0.0;
        long totalNodeCount = 0;
        long totalOnlineCount = 0;
        long totalOfflineCount = 0;
        long totalMemoryTotal = 0;   // 字节
        double totalMemoryFree = 0.0; // 字节
        double totalCpuUtilWeighted = 0.0;
        long totalCpuCoresForUtil = 0;

        for (Cluster cluster : clusters) {
            Integer clusterId = cluster.getClusterId();
            List<NodeMonitor> clusterNodes = allNodes.stream()
                    .filter(n -> Objects.equals(n.getClusterId(), clusterId))
                    .collect(Collectors.toList());

            long cpuSum = 0;
            long gpuSum = 0;
            long slotsMaxSum = 0;
            double slotsUsedSum = 0.0;
            long onlineCount = 0;
            long offlineCount = 0;
            long memTotalSumBytes = 0;      // 改为字节
            double memFreeSumBytes = 0.0;   // 字节
            double cpuUtilWeightedSum = 0.0;
            long cpuCoresForUtil = 0;

            for (NodeMonitor node : clusterNodes) {
                long cpuCores = node.getCpuTotal() == null ? 0L : node.getCpuTotal();
                // 修复点：数据库内存单位为 MB，转换为字节
                long memTotalMB = node.getMemoryTotal() == null ? 0L : node.getMemoryTotal();
                long memTotalBytes = memTotalMB * 1024L * 1024L;
                memTotalSumBytes += memTotalBytes;
                cpuCoresForUtil += cpuCores;

                // 累加静态指标
                cpuSum += cpuCores;
                gpuSum += node.getGpuCount() == null ? 0L : node.getGpuCount();
                slotsMaxSum += node.getSlotsMax() == null ? 0L : node.getSlotsMax();

                // 动态指标（从 Prometheus 获取）
                Map<String, Object> dynamic = promNodeMap.get(node.getNodeName());
                if (dynamic != null) {
                    String status = (String) dynamic.get("status");
                    if ("ok".equals(status)) {
                        onlineCount++;
                    } else if ("unavail".equals(status)) {
                        offlineCount++;
                    }
                    Object usedObj = dynamic.get("slotsUsed");
                    if (usedObj != null) {
                        slotsUsedSum += ((Number) usedObj).doubleValue();
                    }
                    Object cpuUtilObj = dynamic.get("cpuUtil");
                    if (cpuUtilObj != null) {
                        double cpuUtil = ((Number) cpuUtilObj).doubleValue();
                        cpuUtilWeightedSum += cpuUtil * cpuCores;
                    }
                    Object memFreeObj = dynamic.get("memFree");
                    if (memFreeObj != null) {
                        memFreeSumBytes += ((Number) memFreeObj).doubleValue();
                    }
                } else {
                    // 无监控数据的节点视为离线
                    offlineCount++;
                }
            }

            // 累加全局统计
            totalCpuCores += cpuSum;
            totalGpuCount += gpuSum;
            totalSlotsMax += slotsMaxSum;
            totalSlotsUsed += slotsUsedSum;
            totalNodeCount += clusterNodeCount.getOrDefault(clusterId, 0L);
            totalOnlineCount += onlineCount;
            totalOfflineCount += offlineCount;
            totalMemoryTotal += memTotalSumBytes;
            totalMemoryFree += memFreeSumBytes;
            totalCpuUtilWeighted += cpuUtilWeightedSum;
            totalCpuCoresForUtil += cpuCoresForUtil;

            // 构造集群对象
            Map<String, Object> item = new HashMap<>();
            item.put("clusterId", cluster.getClusterId());
            item.put("clusterName", cluster.getClusterName());
            item.put("description", cluster.getDescription());
            item.put("prometheusJob", cluster.getPrometheusJob() != null ? cluster.getPrometheusJob() : cluster.getClusterName());
            item.put("instance", cluster.getInstance());
            item.put("masterNode", cluster.getMasterNode());
            item.put("vendor", cluster.getVendor());
            item.put("nodeTotal", clusterNodeCount.getOrDefault(clusterId, 0L));
            item.put("onlineNode", onlineCount);
            item.put("offlineNode", offlineCount);
            item.put("cpuTotalCores", cpuSum);
            item.put("gpuCount", gpuSum);
            item.put("slotsMaxTotal", slotsMaxSum);
            item.put("slotsUsedTotal", slotsUsedSum);
            item.put("cpuUtilAvg", cpuCoresForUtil > 0 ? cpuUtilWeightedSum / cpuCoresForUtil : 0.0);
            // 内存单位统一为字节（与 memFree 一致）
            item.put("memoryTotal", memTotalSumBytes);
            item.put("memoryFreeTotal", memFreeSumBytes);

            clusterList.add(item);
        }

        // 全局汇总
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClusterCount", totalClusterCount);
        summary.put("totalNodeCount", totalNodeCount);
        summary.put("totalOnlineNodeCount", totalOnlineCount);
        summary.put("totalOfflineNodeCount", totalOfflineCount);
        summary.put("totalCpuCores", totalCpuCores);
        summary.put("totalGpuCount", totalGpuCount);
        summary.put("totalSlotsMax", totalSlotsMax);
        summary.put("totalSlotsUsed", totalSlotsUsed);
        summary.put("totalMemoryTotal", totalMemoryTotal);
        summary.put("totalMemoryFree", totalMemoryFree);
        summary.put("totalCpuUtilAvg", totalCpuCoresForUtil > 0 ? totalCpuUtilWeighted / totalCpuCoresForUtil : 0.0);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("clusters", clusterList);
        resultData.put("summary", summary);

        return Result.ok(resultData);
    }

    @GetMapping("/clusters/{clusterId}")
    public Result<Map<String, Object>> getCluster(@PathVariable Integer clusterId) {
        Cluster cluster = clusterService.getById(clusterId);
        if (cluster == null) {
            return Result.fail(404, "cluster not found");
        }
        Map<String, Object> item = new HashMap<>();
        item.put("clusterId", cluster.getClusterId());
        item.put("clusterName", cluster.getClusterName());
        item.put("description", cluster.getDescription());
        item.put("prometheusJob", cluster.getPrometheusJob() != null ? cluster.getPrometheusJob() : cluster.getClusterName());
        item.put("instance", cluster.getInstance());
        item.put("masterNode", cluster.getMasterNode());

        JSONObject prom = fetchPrometheusClusterInfo().get(cluster.getClusterId());
        if (prom != null) {
            item.put("prometheusTargets", prom.getJSONArray("targets"));
            item.put("status", prom.getString("status"));
            item.put("lastScrape", prom.getString("lastScrape"));
        } else {
            item.put("prometheusTargets", Collections.emptyList());
            item.put("status", "unknown");
            item.put("lastScrape", null);
        }

        return Result.ok(item);
    }

    @GetMapping("/clusters/{clusterId}/nodes")
    public Result<Map<String, Object>> getClusterNodes(@PathVariable Integer clusterId) {
        Cluster cluster = clusterService.getById(clusterId);
        if (cluster == null) {
            return Result.fail(404, "cluster not found");
        }
        List<NodeMonitor> nodes = nodeMonitorService.list().stream()
                .filter(n -> Objects.equals(n.getClusterId(), clusterId))
                .collect(Collectors.toList());
        Map<String, Map<String, Object>> promNodeMap = fetchPrometheusNodeInfo();

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (NodeMonitor node : nodes) {
            Map<String, Object> item = new HashMap<>();

            // 静态字段（数据库）
            item.put("nodeId", node.getNodeId());
            item.put("nodeName", node.getNodeName());
            item.put("nodeIp", node.getNodeIp());
            item.put("partition", node.getPartition());
            item.put("nodeRole", node.getNodeRole());
            item.put("nodeType", node.getNodeType());
            item.put("cpuTotal", node.getCpuTotal());
            item.put("memoryTotal", node.getMemoryTotal());
            item.put("diskTotal", node.getDiskTotal());
            item.put("gpuModel", node.getGpuModel());
            item.put("gpuCount", node.getGpuCount());
            item.put("gpuMemoryTotal", node.getGpuMemoryTotal());
            item.put("ipmiIP", node.getIpmiIP());
            item.put("powerSupported", node.getPowerSupported());
            item.put("powerMetricName", node.getPowerMetricName());
            item.put("clusterId", node.getClusterId());

            // 动态字段（来自 Prometheus，已过滤冗余）
            Map<String, Object> dynamic = promNodeMap.get(node.getNodeName());
            if (dynamic != null) {
                item.putAll(dynamic);
            }

            nodeList.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("clusterId", cluster.getClusterId());
        data.put("clusterName", cluster.getClusterName());
        data.put("total", nodes.size());
        data.put("nodes", nodeList);
        return Result.ok(data);
    }

    private Map<String, Map<String, Object>> fetchPrometheusNodeInfo() {
        Map<String, Map<String, Object>> result = new HashMap<>();

        // 查询节点指标
        PromQueryData nodeData = promQueryService.getQueryDataInfo("{__name__=~\"jingxing_node_.*\"}", null);
        if (nodeData != null && nodeData.getResult() != null) {
            for (PromQueryResult res : nodeData.getResult()) {
                Map<String, Object> labels = res.getMetric();
                String host = labels.get("host") != null ? labels.get("host").toString() : null;
                if (host == null) continue;
                String metric = labels.get("__name__") != null ? labels.get("__name__").toString() : null;
                if (metric == null || !metric.startsWith("jingxing_node_")) continue;
                String key = metric.substring("jingxing_node_".length());

                // 跳过与数据库重复的冗余字段
                if (EXCLUDED_NODE_KEYS.contains(key)) {
                    continue;
                }

                List<String> valueList = res.getValue();
                if (valueList != null && valueList.size() >= 2) {
                    double value = Double.parseDouble(valueList.get(1));
                    Map<String, Object> nodeDataMap = result.computeIfAbsent(host, k -> new HashMap<>());
                    if ("status".equals(key)) {
                        nodeDataMap.put(key, value == 1.0 ? "ok" : "unavail");
                    } else {
                        nodeDataMap.put(key, value);
                    }
                }
            }
        }

        // 查询 GPU 指标
        PromQueryData gpuData = promQueryService.getQueryDataInfo("{__name__=~\"jingxing_gpu_.*\"}", null);
        if (gpuData != null && gpuData.getResult() != null) {
            for (PromQueryResult res : gpuData.getResult()) {
                Map<String, Object> labels = res.getMetric();
                String host = labels.get("host") != null ? labels.get("host").toString() : null;
                String gpuId = labels.get("gpu_id") != null ? labels.get("gpu_id").toString() : null;
                if (host == null || gpuId == null) continue;
                String metric = labels.get("__name__") != null ? labels.get("__name__").toString() : null;
                if (metric == null || !metric.startsWith("jingxing_gpu_")) continue;
                String key = metric.substring("jingxing_gpu_".length());

                // 跳过与数据库重复的 GPU 冗余字段（如总显存）
                if (EXCLUDED_GPU_KEYS.contains(key)) {
                    continue;
                }

                List<String> valueList = res.getValue();
                if (valueList != null && valueList.size() >= 2) {
                    double value = Double.parseDouble(valueList.get(1));
                    Map<String, Object> nodeDataMap = result.computeIfAbsent(host, k -> new HashMap<>());
                    List<Map<String, Object>> gpuList = (List<Map<String, Object>>) nodeDataMap.computeIfAbsent("gpuMetrics", k -> new ArrayList<>());
                    Map<String, Object> gpu = null;
                    for (Map<String, Object> g : gpuList) {
                        if (gpuId.equals(g.get("gpuIndex").toString())) {
                            gpu = g;
                            break;
                        }
                    }
                    if (gpu == null) {
                        gpu = new HashMap<>();
                        gpu.put("gpuIndex", Integer.parseInt(gpuId));
                        gpuList.add(gpu);
                    }
                    gpu.put(key, value);
                }
            }
        }

        return result;
    }

    @GetMapping("/clusters/{clusterId}/nodes/{nodeId}")
    public Result<Map<String, Object>> getClusterNode(@PathVariable Integer clusterId, @PathVariable Integer nodeId) {
        Cluster cluster = clusterService.getById(clusterId);
        if (cluster == null) {
            return Result.fail(404, "cluster not found");
        }
        NodeMonitor node = nodeMonitorService.getById(nodeId);
        if (node == null || !Objects.equals(node.getClusterId(), clusterId)) {
            return Result.fail(404, "node not found");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("static", convertNodeStatic(node));
        data.put("dynamic", buildDynamicNode(node));
        data.put("lastScrape", DateUtil.now());
        return Result.ok(data);
    }

    @GetMapping("/clusters/{clusterId}/history")
    public Result<Map<String, Object>> getClusterHistory(
            @PathVariable Integer clusterId,
            @RequestParam(value = "start", required = false) Long start,
            @RequestParam(value = "end", required = false) Long end,
            @RequestParam(value = "step", required = false) Integer step,
            @RequestParam(value = "range", required = false) String range) {

        Cluster cluster = clusterService.getById(clusterId);
        if (cluster == null) {
            return Result.fail(404, "cluster not found");
        }

        // 获取集群节点列表
        List<NodeMonitor> nodes = nodeMonitorService.list().stream()
                .filter(n -> Objects.equals(n.getClusterId(), clusterId))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return Result.fail(404, "no nodes in cluster");
        }

        // 1. 处理 range 参数（优先级低于显式的 start/end）
        long nowSec = System.currentTimeMillis() / 1000;
        if (start == null && end == null && range != null) {
            switch (range) {
                case "1h":
                    start = nowSec - 3600;
                    end = nowSec;
                    break;
                case "6h":
                    start = nowSec - 6 * 3600;
                    end = nowSec;
                    break;
                case "12h":
                    start = nowSec - 12 * 3600;
                    end = nowSec;
                    break;
                case "1d":
                    start = nowSec - 24 * 3600;
                    end = nowSec;
                    break;
                case "7d":
                    start = nowSec - 7 * 24 * 3600;
                    end = nowSec;
                    break;
                case "30d":
                    start = nowSec - 30 * 24 * 3600;
                    end = nowSec;
                    break;
                default:
                    // 无效 range，使用默认 1h
                    start = nowSec - 3600;
                    end = nowSec;
            }
        }

        // 2. 默认 1 小时范围
        if (start == null) {
            start = nowSec - 3600;
        }
        if (end == null) {
            end = nowSec;
        }
        if (start >= end) {
            return Result.fail(400, "start must be less than end");
        }

        long durationSec = end - start;
        // 3. 自动计算 step（控制最大返回点数，例如 400）
        final int MAX_POINTS = 400;
        int autoStep = (int) Math.max(1, durationSec / MAX_POINTS);
        // 如果 step 未传或 <=0，使用自动 step；否则使用传入 step
        int finalStep = (step == null || step <= 0) ? autoStep : step;

        String startStr = String.valueOf(start);
        String endStr = String.valueOf(end);
        String stepStr = String.valueOf(finalStep);

        // 构建 host 列表
        String hosts = nodes.stream()
                .map(NodeMonitor::getNodeName)
                .collect(Collectors.joining("|"));

        // 指标查询
        Map<String, String> metricsQueries = new HashMap<>();
        metricsQueries.put("cpuUtil", "avg(jingxing_node_cpu_util_percent{host=~\"" + hosts + "\"})");
        metricsQueries.put("slotsUsed", "sum(jingxing_node_slots_used{host=~\"" + hosts + "\"})");
        metricsQueries.put("memFree", "sum(jingxing_node_mem_free_bytes{host=~\"" + hosts + "\"})");

        List<Map<String, Object>> metricsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : metricsQueries.entrySet()) {
            String metricName = entry.getKey();
            String query = entry.getValue();
            PromQueryData data = promQueryService.getQueryRangeDataInfo(query, startStr, endStr, stepStr);
            if (data != null && data.getResult() != null && !data.getResult().isEmpty()) {
                PromQueryResult result = data.getResult().get(0);
                Map<String, Object> metric = new HashMap<>();
                metric.put("metricName", metricName);
                metric.put("unit", getUnitForMetric(metricName));
                List<Map<String, Object>> values = new ArrayList<>();
                if (result.getValues() != null) {
                    for (List<String> point : result.getValues()) {
                        if (point.size() >= 2) {
                            Map<String, Object> valuePoint = new HashMap<>();
                            valuePoint.put("timestamp", Long.parseLong(point.get(0)));
                            try {
                                valuePoint.put("value", Double.parseDouble(point.get(1)));
                            } catch (NumberFormatException e) {
                                valuePoint.put("value", 0.0);
                            }
                            values.add(valuePoint);
                        }
                    }
                }
                metric.put("values", values);
                metricsList.add(metric);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("clusterId", clusterId);
        data.put("clusterName", cluster.getClusterName());
        data.put("metrics", metricsList);

        return Result.ok(data);
    }

    private String getUnitForMetric(String metricName) {
        switch (metricName) {
            case "cpuUtil":
                return "%";
            case "slotsUsed":
                return "count";
            case "memFree":
                return "bytes";
            default:
                return "";
        }
    }

    private Map<String, Object> convertNodeSummary(NodeMonitor node) {
        Map<String, Object> m = new HashMap<>();
        m.put("nodeId", node.getNodeId());
        m.put("nodeName", node.getNodeName());
        m.put("nodeIp", node.getNodeIp());
        m.put("nodeRole", node.getNodeRole());
        m.put("gpuCount", node.getGpuCount());
        m.put("cpuTotal", node.getCpuTotal());
        m.put("memoryTotal", node.getMemoryTotal());
        m.put("status", "up");
        m.put("cpuUsagePercent", 11.2);
        m.put("memoryUsagePercent", 45.2);
        m.put("diskUsagePercent", 38.0);
        m.put("loadAvg", 1.2);
        m.put("temperatureCelsius", 45.2);
        m.put("powerWatts", 1250.5);
        m.put("gpuUtilAvg", 87.5);
        m.put("lastScrape", DateUtil.now());
        return m;
    }

    private Map<String, Object> convertNodeStatic(NodeMonitor node) {
        Map<String, Object> m = new HashMap<>();
        m.put("nodeId", node.getNodeId());
        m.put("nodeName", node.getNodeName());
        m.put("nodeIp", node.getNodeIp());
        m.put("partition", node.getPartition());
        m.put("nodeRole", node.getNodeRole());
        m.put("nodeType", node.getNodeType());
        m.put("cpuTotal", node.getCpuTotal());
        m.put("memoryTotal", node.getMemoryTotal());
        m.put("diskTotal", node.getDiskTotal());
        m.put("gpuModel", node.getGpuModel());
        m.put("gpuCount", node.getGpuCount());
        m.put("gpuMemoryTotal", node.getGpuMemoryTotal());
        m.put("ipmiIP", node.getIpmiIP());
        m.put("powerSupported", node.getPowerSupported());
        m.put("powerMetricName", node.getPowerMetricName());
        return m;
    }

    private Map<String, Object> buildDynamicNode(NodeMonitor node) {
        Map<String, Object> d = new HashMap<>();
        d.put("status", "up");
        Map<String, Object> loadAvg = new HashMap<>();
        loadAvg.put("load1", 1.2);
        loadAvg.put("load5", 0.8);
        loadAvg.put("load15", 0.6);
        d.put("loadAvg", loadAvg);
        d.put("cpuUsagePercent", 12.5);
        d.put("memoryUsagePercent", 45.2);
        d.put("diskUsagePercent", 38.0);
        d.put("temperatureCelsius", 45.2);
        d.put("powerWatts", 1250.5);
        List<Map<String, Object>> gpus = new ArrayList<>();
        for (int i = 0; i < Math.max(1, node.getGpuCount() == null ? 1 : node.getGpuCount()); i++) {
            Map<String, Object> gpu = new HashMap<>();
            gpu.put("gpuIndex", i);
            gpu.put("gpuUtilPercent", 85);
            gpu.put("memUtilPercent", 45);
            gpu.put("memUsedMB", 20480);
            gpu.put("temperatureCelsius", 72);
            gpu.put("powerWatts", 220.5);
            gpus.add(gpu);
        }
        d.put("gpuMetrics", gpus);
        return d;
    }

    private Map<Integer, JSONObject> fetchPrometheusClusterInfo() {
        Map<Integer, JSONObject> map = new HashMap<>();
        try {
            String targetJson = restTemplateUtils.getHttp("http://localhost:9090/api/v1/status/config", new JSONObject());
            if (targetJson != null) {
                JSONObject body = JSONObject.parseObject(targetJson);
                if ("success".equals(body.getString("status"))) {
                    // 解析部分信息
                    // 这里只作示范，用实际数据可扩展
                    JSONArray jobs = body.getJSONObject("data").getJSONArray("yaml");
                    // 暂不解析具体节点，返回空
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }
}
