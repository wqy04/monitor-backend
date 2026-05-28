package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.Cluster;
import com.example.monitor.entity.NodeMonitor;
import com.example.monitor.service.ClusterService;
import com.example.monitor.service.NodeMonitorService;
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
import java.util.function.Function;
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

    // 排除的静态指标键名
    private static final Set<String> EXCLUDED_NODE_KEYS = Set.of(
        "mem_total_bytes", "cpu_cores", "slots_max", "gpu_total", "model_info"
    );

    private static final Set<String> EXCLUDED_GPU_KEYS = Set.of("mem_total_bytes");

    // ==================== 集群列表接口 ====================
    @GetMapping("/clusters")
    public Result<Map<String, Object>> listClusters() {
        List<Cluster> clusters = clusterService.list();
        List<NodeMonitor> allNodes = nodeMonitorService.list();

        // 查询所有节点的动态数据（景行 + Slurm）
        Map<String, Map<String, Object>> promNodeMap = fetchAllNodeDynamicData();

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
        long totalMemoryTotal = 0;
        double totalMemoryFree = 0.0;
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
            long memTotalSumBytes = 0;
            double memFreeSumBytes = 0.0;
            double cpuUtilWeightedSum = 0.0;
            long cpuCoresForUtil = 0;

            for (NodeMonitor node : clusterNodes) {
                long cpuCores = node.getCpuTotal() == null ? 0L : node.getCpuTotal();
                long memTotalMB = node.getMemoryTotal() == null ? 0L : node.getMemoryTotal();
                long memTotalBytes = memTotalMB * 1024L * 1024L;
                memTotalSumBytes += memTotalBytes;
                cpuCoresForUtil += cpuCores;

                cpuSum += cpuCores;
                gpuSum += node.getGpuCount() == null ? 0L : node.getGpuCount();
                slotsMaxSum += node.getSlotsMax() == null ? 0L : node.getSlotsMax();

                Map<String, Object> dynamic = promNodeMap.get(node.getNodeName());
                if (dynamic != null) {
                    String status = (String) dynamic.get("status");
                    if ("ok".equals(status)) {
                        onlineCount++;
                    } else {
                        offlineCount++;
                    }
                    Object usedObj = dynamic.get("slots_used");
                    if (usedObj != null) {
                        slotsUsedSum += ((Number) usedObj).doubleValue();
                    }
                    Object cpuUtilObj = dynamic.get("cpu_util_percent");
                    if (cpuUtilObj != null) {
                        double cpuUtil = ((Number) cpuUtilObj).doubleValue();
                        cpuUtilWeightedSum += cpuUtil * cpuCores;
                    }
                    Object memFreeObj = dynamic.get("mem_free_bytes");
                    if (memFreeObj != null) {
                        memFreeSumBytes += ((Number) memFreeObj).doubleValue();
                    }
                } else {
                    offlineCount++;
                }
            }

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

            // 关键修改：当没有在线节点时，利用率相关字段返回 null
            if (onlineCount == 0) {
                item.put("cpuUtilAvg", null);
                item.put("memoryFreeTotal", null);
            } else {
                double cpuAvg = cpuCoresForUtil > 0 ? cpuUtilWeightedSum / cpuCoresForUtil : 0.0;
                item.put("cpuUtilAvg", cpuAvg);
                item.put("memoryFreeTotal", memFreeSumBytes);
            }

            item.put("memoryTotal", memTotalSumBytes);

            clusterList.add(item);
        }

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

    // ==================== 集群详情接口 ====================
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

    // ==================== 集群节点列表接口 ====================
    @GetMapping("/clusters/{clusterId}/nodes")
    public Result<Map<String, Object>> getClusterNodes(@PathVariable Integer clusterId) {
        Cluster cluster = clusterService.getById(clusterId);
        if (cluster == null) {
            return Result.fail(404, "cluster not found");
        }
        List<NodeMonitor> nodes = nodeMonitorService.list().stream()
                .filter(n -> Objects.equals(n.getClusterId(), clusterId))
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> promNodeMap = fetchAllNodeDynamicData();

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

            // 动态字段（来自 Prometheus）
            Map<String, Object> dynamic = promNodeMap.get(node.getNodeName());
            if (dynamic != null) {
                if (dynamic.containsKey("cpu_util_percent")) {
                    item.put("cpuUtilPercent", dynamic.get("cpu_util_percent"));
                }
                if (dynamic.containsKey("mem_free_bytes")) {
                    item.put("memFreeBytes", dynamic.get("mem_free_bytes"));
                }
                if (dynamic.containsKey("slots_used")) {
                    item.put("slotsUsed", dynamic.get("slots_used"));
                }
                if (dynamic.containsKey("status")) {
                    item.put("status", dynamic.get("status"));
                }
                // 其他字段（如 GPU 指标）直接放入
                dynamic.forEach((k, v) -> {
                    if (!k.equals("cpu_util_percent") && !k.equals("mem_free_bytes") && !k.equals("slots_used") && !k.equals("status")) {
                        item.put(k, v);
                    }
                });
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

    // ==================== 单个节点详情接口 ====================
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

    // ==================== 集群历史指标接口 ====================
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

        // 判断集群类型（根据 vendor 或 prometheusJob）
        boolean isSlurm = "Slurm".equalsIgnoreCase(cluster.getVendor())
                || "slurm".equalsIgnoreCase(cluster.getPrometheusJob());

        List<NodeMonitor> nodes = nodeMonitorService.list().stream()
                .filter(n -> Objects.equals(n.getClusterId(), clusterId))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return Result.fail(404, "no nodes in cluster");
        }

        // 处理时间范围
        long nowSec = System.currentTimeMillis() / 1000;
        if (start == null && end == null && range != null) {
            switch (range) {
                case "1h": start = nowSec - 3600; end = nowSec; break;
                case "6h": start = nowSec - 6 * 3600; end = nowSec; break;
                case "12h": start = nowSec - 12 * 3600; end = nowSec; break;
                case "1d": start = nowSec - 24 * 3600; end = nowSec; break;
                case "7d": start = nowSec - 7 * 24 * 3600; end = nowSec; break;
                case "30d": start = nowSec - 30 * 24 * 3600; end = nowSec; break;
                default: start = nowSec - 3600; end = nowSec;
            }
        }
        if (start == null) start = nowSec - 3600;
        if (end == null) end = nowSec;
        if (start >= end) {
            return Result.fail(400, "start must be less than end");
        }

        long durationSec = end - start;
        final int MAX_POINTS = 400;
        int autoStep = (int) Math.max(1, durationSec / MAX_POINTS);
        int finalStep = (step == null || step <= 0) ? autoStep : step;

        String startStr = String.valueOf(start);
        String endStr = String.valueOf(end);
        String stepStr = String.valueOf(finalStep);

        String hosts = nodes.stream()
                .map(NodeMonitor::getNodeName)
                .collect(Collectors.joining("|"));

        Map<String, String> metricsQueries = new HashMap<>();
        if (isSlurm) {
            // Slurm 集群使用 host 标签，仅查询 CPU 和 Slot 相关指标
            metricsQueries.put("cpuUtil",
                    "avg((slurm_node_cpu_alloc{host=~\"" + hosts + "\"} / slurm_node_cpu_cores{host=~\"" + hosts + "\"}) * 100)");
            metricsQueries.put("slotsUsed",
                    "sum(slurm_node_cpu_alloc{host=~\"" + hosts + "\"})");
            // 不再查询 memFree，因为指标不存在
        } else {
            // 景行集群原有 PromQL
            metricsQueries.put("cpuUtil", "avg(jingxing_node_cpu_util_percent{host=~\"" + hosts + "\"})");
            metricsQueries.put("slotsUsed", "sum(jingxing_node_slots_used{host=~\"" + hosts + "\"})");
            metricsQueries.put("memFree", "sum(jingxing_node_mem_free_bytes{host=~\"" + hosts + "\"})");
        }
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
            case "cpuUtil": return "%";
            case "slotsUsed": return "count";
            case "memFree": return "bytes";
            default: return "";
        }
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

    // 模拟动态数据（实际应来自实时查询，此处仅用于单个节点接口示例）
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

    // ==================== 动态数据获取核心方法 ====================
    private Map<String, Map<String, Object>> fetchAllNodeDynamicData() {
        Map<String, Map<String, Object>> allNodes = new HashMap<>();
        allNodes.putAll(fetchJingxingNodeInfo());
        allNodes.putAll(fetchSlurmNodeInfo());
        return allNodes;
    }

    /**
     * 景行集群节点动态指标
     */
    private Map<String, Map<String, Object>> fetchJingxingNodeInfo() {
        Map<String, Map<String, Object>> result = new HashMap<>();

        PromQueryData nodeData = promQueryService.getQueryDataInfo("{__name__=~\"jingxing_node_.*\"}", null);
        if (nodeData != null && nodeData.getResult() != null) {
            for (PromQueryResult res : nodeData.getResult()) {
                Map<String, Object> labels = res.getMetric();
                String host = labels.get("host") != null ? labels.get("host").toString() : null;
                if (host == null) continue;
                String metric = labels.get("__name__") != null ? labels.get("__name__").toString() : null;
                if (metric == null || !metric.startsWith("jingxing_node_")) continue;
                String key = metric.substring("jingxing_node_".length());
                if (EXCLUDED_NODE_KEYS.contains(key)) continue;

                List<String> valueList = res.getValue();
                if (valueList != null && valueList.size() >= 2) {
                    double value = Double.parseDouble(valueList.get(1));
                    Map<String, Object> nodeMap = result.computeIfAbsent(host, k -> new HashMap<>());
                    if ("status".equals(key)) {
                        nodeMap.put(key, value == 1.0 ? "ok" : "unavail");
                    } else {
                        nodeMap.put(key, value);
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
                if (EXCLUDED_GPU_KEYS.contains(key)) continue;

                List<String> valueList = res.getValue();
                if (valueList != null && valueList.size() >= 2) {
                    double value = Double.parseDouble(valueList.get(1));
                    Map<String, Object> nodeMap = result.computeIfAbsent(host, k -> new HashMap<>());
                    List<Map<String, Object>> gpuList = (List<Map<String, Object>>) nodeMap.computeIfAbsent("gpuMetrics", k -> new ArrayList<>());
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

    /**
     * Slurm 集群节点动态指标（修复标签名 host）
     */
    private Map<String, Map<String, Object>> fetchSlurmNodeInfo() {
        Map<String, Map<String, Object>> slurmNodes = new HashMap<>();

        // 查询所需指标的最新值（使用 host 标签）
        PromQueryData cpuCoresData = promQueryService.getQueryDataInfo("slurm_node_cpu_cores", null);
        PromQueryData cpuAllocData = promQueryService.getQueryDataInfo("slurm_node_cpu_alloc", null);
        PromQueryData memFreeData = promQueryService.getQueryDataInfo("slurm_node_mem_free_bytes", null);
        PromQueryData nodeUpData = promQueryService.getQueryDataInfo("slurm_node_up", null);

        // 辅助函数：将 PromQueryData 转为 Map<节点名, 数值>，标签使用 "host"
        Function<PromQueryData, Map<String, Double>> toNodeValueMap = (data) -> {
            Map<String, Double> map = new HashMap<>();
            if (data != null && data.getResult() != null) {
                for (PromQueryResult res : data.getResult()) {
                    Map<String, Object> labels = res.getMetric();
                    String host = labels.get("host") != null ? labels.get("host").toString() : null;
                    if (host == null) continue;
                    List<String> values = res.getValue();
                    if (values != null && values.size() >= 2) {
                        try {
                            double val = Double.parseDouble(values.get(1));
                            map.put(host, val);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            return map;
        };

        Map<String, Double> cpuCoresMap = toNodeValueMap.apply(cpuCoresData);
        Map<String, Double> cpuAllocMap = toNodeValueMap.apply(cpuAllocData);
        Map<String, Double> memFreeMap = toNodeValueMap.apply(memFreeData);
        Map<String, Double> nodeUpMap = toNodeValueMap.apply(nodeUpData);

        // 收集所有出现的节点名
        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(cpuCoresMap.keySet());
        allNodes.addAll(cpuAllocMap.keySet());
        allNodes.addAll(memFreeMap.keySet());
        allNodes.addAll(nodeUpMap.keySet());

        for (String node : allNodes) {
            Map<String, Object> dynamic = new HashMap<>();

            // 1. 节点状态
            Double up = nodeUpMap.get(node);
            if (up != null && up == 1.0) {
                dynamic.put("status", "ok");
            } else {
                dynamic.put("status", "unavail");
            }

            // 2. 已用作业槽
            Double alloc = cpuAllocMap.get(node);
            dynamic.put("slots_used", alloc != null ? alloc : 0.0);

            // 3. CPU 利用率 (alloc / cores * 100)
            Double cores = cpuCoresMap.get(node);
            if (cores != null && cores > 0 && alloc != null) {
                double cpuUtil = (alloc / cores) * 100.0;
                dynamic.put("cpu_util_percent", cpuUtil);
            } else {
                dynamic.put("cpu_util_percent", 0.0);
            }

            // 4. 空闲内存
            Double freeMem = memFreeMap.get(node);
            dynamic.put("mem_free_bytes", freeMem != null ? freeMem : 0.0);

            slurmNodes.put(node, dynamic);
        }

        return slurmNodes;
    }

    // ==================== 辅助方法 ====================
    private Map<Integer, JSONObject> fetchPrometheusClusterInfo() {
        Map<Integer, JSONObject> map = new HashMap<>();
        try {
            String targetJson = restTemplateUtils.getHttp("http://localhost:9090/api/v1/status/config", new JSONObject());
            if (targetJson != null) {
                JSONObject body = JSONObject.parseObject(targetJson);
                if ("success".equals(body.getString("status"))) {
                    // 可解析 target 信息，此处留空或按需实现
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }
}