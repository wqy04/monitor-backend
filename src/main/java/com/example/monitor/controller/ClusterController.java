package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.Cluster;
import com.example.monitor.entity.NodeMonitor;
import com.example.monitor.service.ClusterService;
import com.example.monitor.service.NodeMonitorService;
import com.example.monitor.utils.RestTemplateUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.date.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    private RestTemplateUtils restTemplateUtils;

    @GetMapping("/clusters")
    public Result<List<Map<String, Object>>> listClusters() {
        List<Cluster> clusters = clusterService.list();
        List<Map<String, Object>> records = new ArrayList<>();

        Map<Integer, JSONObject> promClusterMap = fetchPrometheusClusterInfo();

        // 获取所有节点并按集群分组计数
        List<NodeMonitor> allNodes = nodeMonitorService.list();
        Map<Integer, Long> clusterNodeCount = allNodes.stream()
                .collect(Collectors.groupingBy(NodeMonitor::getClusterId, Collectors.counting()));

        for (Cluster cluster : clusters) {
            Map<String, Object> item = new HashMap<>();
            item.put("clusterId", cluster.getClusterId());
            item.put("clusterName", cluster.getClusterName());
            item.put("description", cluster.getDescription());
            item.put("prometheusJob", cluster.getPrometheusJob() != null ? cluster.getPrometheusJob() : cluster.getClusterName());
            item.put("instance", cluster.getInstance());
            item.put("masterNode", cluster.getMasterNode());
            item.put("vendor", cluster.getVendor());
            item.put("nodeTotal", clusterNodeCount.getOrDefault(cluster.getClusterId(), 0L));

            
            records.add(item);
        }
        return Result.ok(records);
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
        List<NodeMonitor> nodes = nodeMonitorService.list();
        List<Map<String, Object>> nodeList = nodes.stream()
                .filter(n -> Objects.equals(n.getClusterId(), clusterId))
                .map(this::convertNodeSummary)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("clusterId", cluster.getClusterId());
        data.put("clusterName", cluster.getClusterName());
        data.put("nodes", nodeList);
        return Result.ok(data);
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

    @GetMapping("/nodes/{nodeId}/metrics/history")
    public Result<Map<String, Object>> getNodeMetricsHistory(@PathVariable Integer nodeId,
                                                             @RequestParam(value = "metrics") String metrics,
                                                             @RequestParam(value = "start", required = false) Long start,
                                                             @RequestParam(value = "end", required = false) Long end,
                                                             @RequestParam(value = "step", required = false) Integer step) {
        NodeMonitor node = nodeMonitorService.getById(nodeId);
        if (node == null) {
            return Result.fail(404, "node not found");
        }
        // mock data for demonstration
        if (start == null) {
            start = System.currentTimeMillis() / 1000 - 3600;
        }
        if (end == null) {
            end = System.currentTimeMillis() / 1000;
        }
        if (step == null || step <= 0) {
            step = 15;
        }
        String[] metricArray = metrics.split(",");
        List<Map<String, Object>> list = new ArrayList<>();
        for (String metric : metricArray) {
            Map<String, Object> m = new HashMap<>();
            m.put("metricName", metric.trim());
            m.put("unit", "%");
            List<Map<String, Object>> values = new ArrayList<>();
            for (long t = start; t <= end; t += step) {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", t);
                point.put("value", Math.round(Math.random() * 10000) / 100.0);
                values.add(point);
            }
            m.put("values", values);
            list.add(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nodeId", nodeId);
        data.put("nodeName", node.getNodeName());
        data.put("metrics", list);

        return Result.ok(data);
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
