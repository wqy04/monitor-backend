package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.NodeMonitor;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.example.monitor.service.NodeMonitorService;
import com.example.monitor.service.PromQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api")
public class NodeController {

    @Autowired
    private NodeMonitorService nodeMonitorService;

    @Autowired
    private PromQueryService promQueryService;

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

    @GetMapping("/nodes")
    public Result<Map<String, Object>> listNodes() {
        List<NodeMonitor> nodes = nodeMonitorService.list();
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
}