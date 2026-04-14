package com.example.monitor.service;

import com.example.monitor.entity.*;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import com.example.monitor.entity.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 景行调度器适配器（联想集群）
 * 修复节点表字段采集，确保 cpu_total、memory_total、gpu 信息等正确写入
 */
@Component
@Slf4j
public class JingxingAdapter implements ClusterMetadataAdapter {

    private static final long BYTES_TO_MB = 1024 * 1024;
    private static final Pattern LOGIN_NODE_PATTERN = Pattern.compile("^mgt\\d+$");
    private static final Pattern COMPUTE_NODE_PATTERN = Pattern.compile("^(cpui|gpu|edge)\\d*$");
    private static final Pattern STORAGE_NODE_PATTERN = Pattern.compile("^mem\\d+$");

    @Override
    public boolean supports(PromQueryService promQueryService) {
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_cluster_info", null);
        return data != null && !CollectionUtils.isEmpty(data.getResult());
    }

    @Override
    public List<Cluster> discoverClusters(PromQueryService promQueryService) {
        List<Cluster> clusters = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_cluster_info", null);
        if (data == null) return clusters;

        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String clusterName = (String) metric.get("cluster");
            if (StringUtils.hasText(clusterName)) {
                Cluster cluster = new Cluster();
                cluster.setClusterName(clusterName);
                cluster.setVendor("联想");
                cluster.setDescription("景行调度器集群");
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    @Override
    public List<NodeMonitor> discoverNodes(Cluster cluster, PromQueryService promQueryService) {
        List<NodeMonitor> nodes = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_node_status", null);
        if (data == null) return nodes;

        Map<String, Integer> cpuTotalMap = queryNodeMetricMapInt(promQueryService, "jingxing_node_cpu_cores");
        Map<String, Integer> slotsMaxMap = queryNodeMetricMapInt(promQueryService, "jingxing_node_slots_max");
        Map<String, Long> memTotalBytesMap = queryNodeMetricMapLong(promQueryService, "jingxing_node_mem_total_bytes");
        Map<String, NodeModelInfo> modelInfoMap = queryNodeModelInfo(promQueryService);
        Map<String, GpuNodeAgg> gpuNodeAggMap = buildGpuNodeAgg(promQueryService);
        
        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String host = (String) metric.get("host");
            if (!StringUtils.hasText(host)) continue;
            
            NodeMonitor node = new NodeMonitor();
            node.setNodeName(host);
            node.setClusterId(cluster.getClusterId());
            node.setNodeIp(resolveIp(host, metric));

            node.setCpuTotal(cpuTotalMap.getOrDefault(host, null));
            node.setSlotsMax(slotsMaxMap.getOrDefault(host, null));

            Long memBytes = memTotalBytesMap.get(host);
            if (memBytes != null) {
                node.setMemoryTotal(memBytes / BYTES_TO_MB);
            }

            NodeModelInfo modelInfo = modelInfoMap.get(host);
            if (modelInfo != null) {
                node.setCpuModel(modelInfo.getModel());
                node.setOsType(modelInfo.getType());
            }

            GpuNodeAgg gpuAgg = gpuNodeAggMap.get(host);
            if (gpuAgg != null && gpuAgg.getCount() > 0) {
                node.setGpuCount(gpuAgg.getCount());
                node.setGpuModel(gpuAgg.getModel());
                node.setGpuMemoryTotal(gpuAgg.getTotalMemoryMB());
            } else {
                node.setGpuCount(0);
                node.setGpuModel(null);
                node.setGpuMemoryTotal(null);
            }

            node.setNodeRole(inferNodeRole(host));
            node.setNodeType(inferNodeType(host, node.getGpuCount(), node.getMemoryTotal()));

            // ✅ 根据节点名称前缀分配主要分区
            node.setPartition(inferPrimaryPartition(host));
            log.info("节点 {} 采集详情: cpuModel={}, osType={}, slotsMax={}, partition={}, gpuCount={}, gpuModel={}, gpuMemoryTotal={}",
                host,
                node.getCpuModel(),
                node.getOsType(),
                node.getSlotsMax(),
                node.getPartition(),
                node.getGpuCount(),
                node.getGpuModel(),
                node.getGpuMemoryTotal());
            nodes.add(node);
        }
        
        return nodes;
    }

    /**
     * 推断节点的主要分区（队列）
     */
    private String inferPrimaryPartition(String hostname) {
        if (hostname.startsWith("cpui")) {
            return "normal";          // CPU 节点归入 normal 队列
        } else if (hostname.startsWith("gpu")) {
            return "gpu_queue";       // GPU 节点归入 gpu_queue 队列
        } else if (hostname.startsWith("mem")) {
            return "mem_queue";       // 大内存节点归入 mem_queue 队列
        } else if (hostname.startsWith("edge")) {
            return "gpu_queue";       // edge 节点带有 GPU，也归入 gpu_queue
        } else {
            return "normal";          // mgt 等管理节点默认归入 normal
        }
    }

    @Override
    public List<Queue> discoverQueues(Cluster cluster, PromQueryService promQueryService) {
        List<Queue> queues = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_queue_info", null);
        if (data == null) return queues;

        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String queueName = (String) metric.get("queue");
            if (!StringUtils.hasText(queueName)) continue;

            Queue queue = new Queue();
            queue.setQueueName(queueName);
            queue.setClusterId(cluster.getClusterId());
            queue.setNice(toInt(metric.getOrDefault("nice", 0)));
            queue.setPriority(toInt(metric.getOrDefault("priority", 0)));
            queue.setStatus((String) metric.get("status"));
            // ✅ 新增采集 description
            queue.setDescription((String) metric.get("description"));
            queues.add(queue);
        }
        return queues;
    }

    @Override
    public List<AlarmInfo> discoverAlerts(Cluster cluster, PromQueryService promQueryService) {
        List<AlarmInfo> alarms = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_node_status", null);
        if (data == null) return alarms;

        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String host = (String) metric.get("host");
            String status = (String) metric.get("status");
            if (host != null && status != null && !"ok".equalsIgnoreCase(status)) {
                AlarmInfo alarm = new AlarmInfo();
                alarm.setNotice("节点 " + host + " 状态异常：" + status);
                alarm.setTarget(host);
                alarm.setLevel("unavail".equals(status) ? 3 : 2);
                alarm.setStatus(0);
                alarm.setUpdateTime(LocalDateTime.now());
                alarm.setClusterId(cluster.getClusterId());
                alarms.add(alarm);
            }
        }
        return alarms;
    }

    // ==================== 扩展采集方法（需外部调用） ====================

    public List<App> discoverApps(Cluster cluster, PromQueryService promQueryService) {
        List<App> apps = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_app_info", null);
        if (data == null) return apps;

        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String appName = (String) metric.get("app");
            String description = (String) metric.get("description");
            if (!StringUtils.hasText(appName)) continue;

            App app = new App();
            app.setAppName(appName);
            app.setDescription(StringUtils.hasText(description) ? description : null);
            app.setClusterId(cluster.getClusterId());
            apps.add(app);
        }
        return apps;
    }

    public List<ClusterUser> discoverClusterUsers(Cluster cluster, PromQueryService promQueryService) {
        List<ClusterUser> users = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("jingxing_user_max_slots", null);
        if (data == null) return users;

        Set<String> uniqueUsers = new HashSet<>();
        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String username = (String) metric.get("user");
            if (StringUtils.hasText(username) && uniqueUsers.add(username)) {
                ClusterUser user = new ClusterUser();
                user.setUsername(username);
                user.setClusterId(cluster.getClusterId());
                users.add(user);
            }
        }
        return users;
    }

    public List<Gpu> discoverGpus(Cluster cluster, PromQueryService promQueryService,
                                  Map<String, Integer> nodeNameToIdMap) {
        List<Gpu> gpus = new ArrayList<>();
        PromQueryData infoData = promQueryService.getQueryDataInfo("jingxing_gpu_info", null);
        if (infoData == null) return gpus;

        // 获取每个 GPU 的显存
        Map<String, Long> gpuMemTotalMap = queryGpuMemTotal(promQueryService);

        for (PromQueryResult result : infoData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String host = (String) metric.get("host");
            String gpuIdStr = (String) metric.get("gpu_id");
            String gpuType = (String) metric.get("gpu_type");
            String status = (String) metric.get("status");

            if (!StringUtils.hasText(host) || !StringUtils.hasText(gpuIdStr)) continue;

            Integer nodeId = nodeNameToIdMap.get(host);
            if (nodeId == null) {
                log.warn("未找到节点 {} 的 node_id，跳过 GPU 信息", host);
                continue;
            }

            int gpuIndex;
            try {
                gpuIndex = Integer.parseInt(gpuIdStr);
            } catch (NumberFormatException e) {
                log.warn("无效的 gpu_id: {}", gpuIdStr);
                continue;
            }

            String memKey = host + "_" + gpuIndex;
            Long memBytes = gpuMemTotalMap.get(memKey);
            Long memMB = memBytes != null ? memBytes / BYTES_TO_MB : null;

            Gpu gpu = new Gpu();
            gpu.setNodeId(nodeId);
            gpu.setGpuIndex(gpuIndex);
            gpu.setGpuModel(gpuType);
            gpu.setMemoryTotal(memMB);
            gpu.setStatus(StringUtils.hasText(status) ? status : "avail");
            gpus.add(gpu);
        }
        return gpus;
    }

    public List<NodeQueue> discoverNodeQueues(Cluster cluster, PromQueryService promQueryService,
                                            Map<String, Integer> nodeNameToIdMap,
                                            Map<String, Integer> queueNameToIdMap) {
        List<NodeQueue> nodeQueues = new ArrayList<>();

        // 1. 获取所有队列信息（含 hosts 字段）
        PromQueryData queueData = promQueryService.getQueryDataInfo("jingxing_queue_info", null);
        if (queueData == null) return nodeQueues;

        // 2. 构建队列名 -> hosts 组名的映射
        Map<String, String> queueHostsMap = new HashMap<>();
        for (PromQueryResult result : queueData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String queueName = (String) metric.get("queue");
            String hosts = (String) metric.get("hosts");
            if (StringUtils.hasText(queueName) && StringUtils.hasText(hosts)) {
                queueHostsMap.put(queueName, hosts);
            }
        }

        // 3. 遍历所有节点，判断应属于哪些队列
        PromQueryData nodeData = promQueryService.getQueryDataInfo("jingxing_node_status", null);
        if (nodeData == null) return nodeQueues;

        for (PromQueryResult result : nodeData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String host = (String) metric.get("host");
            if (!StringUtils.hasText(host)) continue;

            Integer nodeId = nodeNameToIdMap.get(host);
            if (nodeId == null) continue;

            // 判断该节点所属的主机组
            Set<String> nodeGroups = getNodeGroups(host);

            for (Map.Entry<String, String> entry : queueHostsMap.entrySet()) {
                String queueName = entry.getKey();
                String hostsGroup = entry.getValue();

                // 如果队列允许的主机组匹配节点的任一组，则建立关联
                if (matchesHostGroup(nodeGroups, hostsGroup)) {
                    Integer queueId = queueNameToIdMap.get(queueName);
                    if (queueId != null) {
                        NodeQueue nq = new NodeQueue();
                        nq.setNodeId(nodeId);
                        nq.setQueueName(queueName);
                        nodeQueues.add(nq);
                    }
                }
            }
        }
        return nodeQueues;
    }

    /**
     * 根据节点名返回其所属的主机组集合
     */
    private Set<String> getNodeGroups(String hostname) {
        Set<String> groups = new HashSet<>();
        // 所有节点都属于 "all hosts"（实际 prometheus 中写为 "all hosts used by the scheduler system"）
        groups.add("all");

        if (hostname.startsWith("cpui")) {
            groups.add("cpu");
        } else if (hostname.startsWith("gpu")) {
            groups.add("gpu");
        } else if (hostname.startsWith("mem")) {
            groups.add("mem");
        } else if (hostname.startsWith("edge")) {
            groups.add("gpu");   // edge 节点有 GPU，也归入 gpu 组
        } else if (hostname.startsWith("mgt")) {
            // 管理节点通常不属于任何资源组，但可能被 all 组覆盖，这里不加特殊组
        }
        return groups;
    }

    /**
     * 判断节点所属组是否与队列的 hosts 字段匹配
     */
    private boolean matchesHostGroup(Set<String> nodeGroups, String hostsField) {
        if (hostsField == null) return false;

        // "all hosts used by the scheduler system" 视为匹配所有节点
        if (hostsField.toLowerCase().contains("all host")) {
            return true;
        }

        // 检查是否包含 cpu_nodes / gpu_nodes / mem_nodes 等关键词
        String lowerField = hostsField.toLowerCase();
        if (lowerField.contains("cpu") && nodeGroups.contains("cpu")) {
            return true;
        }
        if (lowerField.contains("gpu") && nodeGroups.contains("gpu")) {
            return true;
        }
        if (lowerField.contains("mem") && nodeGroups.contains("mem")) {
            return true;
        }

        return false;
    }

    public JobScheduler discoverJobScheduler(Cluster cluster) {
        JobScheduler scheduler = new JobScheduler();
        scheduler.setSchedulerName("Jingxing");
        scheduler.setStatus(0);
        scheduler.setClusterId(cluster.getClusterId());
        return scheduler;
    }

    // ==================== 辅助方法 ====================

    private Map<String, Integer> queryNodeMetricMapInt(PromQueryService service, String metricName) {
        Map<String, Integer> resultMap = new HashMap<>();
        PromQueryData data = service.getQueryDataInfo(metricName, null);
        if (data == null) return resultMap;
        for (PromQueryResult res : data.getResult()) {
            String host = (String) res.getMetric().get("host");
            if (host == null) continue;
            String raw = extractPrometheusValue(res);
            if (!StringUtils.hasText(raw)) continue;
            try {
                double val = Double.parseDouble(raw);
                resultMap.put(host, (int) val);
            } catch (NumberFormatException ignored) {
                log.warn("无法解析指标 {} 的值: {}", metricName, raw);
            }
        }
        return resultMap;
    }

    private Map<String, Long> queryNodeMetricMapLong(PromQueryService service, String metricName) {
        Map<String, Long> resultMap = new HashMap<>();
        PromQueryData data = service.getQueryDataInfo(metricName, null);
        if (data == null) return resultMap;
        for (PromQueryResult res : data.getResult()) {
            String host = (String) res.getMetric().get("host");
            if (host == null) continue;
            String raw = extractPrometheusValue(res);
            if (!StringUtils.hasText(raw)) continue;
            try {
                double val = Double.parseDouble(raw);
                resultMap.put(host, (long) val);
            } catch (NumberFormatException ignored) {
                log.warn("无法解析指标 {} 的值: {}", metricName, raw);
            }
        }
        return resultMap;
    }

    private Map<String, NodeModelInfo> queryNodeModelInfo(PromQueryService service) {
        Map<String, NodeModelInfo> resultMap = new HashMap<>();
        PromQueryData data = service.getQueryDataInfo("jingxing_node_model_info", null);
        if (data == null) return resultMap;
        for (PromQueryResult res : data.getResult()) {
            String host = (String) res.getMetric().get("host");
            if (host == null) continue;
            String model = (String) res.getMetric().get("model");
            String type = (String) res.getMetric().get("type");
            resultMap.put(host, new NodeModelInfo(model, type));
        }
        return resultMap;
    }

    /**
     * 构建节点 GPU 聚合信息（总数、型号、总显存）
     * 显存通过累加每个 GPU 的 jingxing_gpu_mem_total_bytes 获得，避免依赖复杂 PromQL
     */
    private Map<String, GpuNodeAgg> buildGpuNodeAgg(PromQueryService service) {
        Map<String, GpuNodeAgg> aggMap = new HashMap<>();

        // 1. 获取每个节点的 GPU 数量
        Map<String, Integer> gpuCountMap = queryNodeMetricMapInt(service, "jingxing_node_gpu_total");

        // 2. 获取每个 GPU 的型号和显存，按节点聚合
        Map<String, String> firstGpuModel = new HashMap<>();
        Map<String, Long> nodeTotalMemBytes = new HashMap<>();

        PromQueryData gpuInfoData = service.getQueryDataInfo("jingxing_gpu_info", null);
        PromQueryData gpuMemData = service.getQueryDataInfo("jingxing_gpu_mem_total_bytes", null);

        // 建立 GPU 显存映射 key: host_gpuId -> memBytes
        Map<String, Long> gpuMemMap = new HashMap<>();
        if (gpuMemData != null) {
            for (PromQueryResult res : gpuMemData.getResult()) {
                String host = (String) res.getMetric().get("host");
                String gpuId = (String) res.getMetric().get("gpu_id");
                if (host == null || gpuId == null) continue;
                String raw = extractPrometheusValue(res);
                if (!StringUtils.hasText(raw)) continue;
                try {
                    long memBytes = (long) Double.parseDouble(raw);
                    gpuMemMap.put(host + "_" + gpuId, memBytes);
                } catch (NumberFormatException e) {
                    log.warn("解析 GPU 显存失败: {}", raw);
                }
            }
        }

        // 聚合节点 GPU 型号和总显存
        if (gpuInfoData != null) {
            for (PromQueryResult res : gpuInfoData.getResult()) {
                String host = (String) res.getMetric().get("host");
                String gpuType = (String) res.getMetric().get("gpu_type");
                String gpuId = (String) res.getMetric().get("gpu_id");
                if (host == null || gpuId == null) continue;

                // 记录第一个 GPU 型号
                if (!firstGpuModel.containsKey(host) && gpuType != null) {
                    firstGpuModel.put(host, gpuType);
                }

                // 累加显存
                String memKey = host + "_" + gpuId;
                Long memBytes = gpuMemMap.get(memKey);
                if (memBytes != null) {
                    nodeTotalMemBytes.merge(host, memBytes, Long::sum);
                }
            }
        }

        // 3. 组装结果
        for (Map.Entry<String, Integer> entry : gpuCountMap.entrySet()) {
            String host = entry.getKey();
            Integer count = entry.getValue();
            if (count == null || count == 0) continue;
            String model = firstGpuModel.getOrDefault(host, "Unknown");
            Long totalBytes = nodeTotalMemBytes.get(host);
            Long totalMB = totalBytes != null ? totalBytes / BYTES_TO_MB : null;
            aggMap.put(host, new GpuNodeAgg(count, model, totalMB));
        }
        return aggMap;
    }

    private Map<String, Long> queryGpuMemTotal(PromQueryService service) {
        Map<String, Long> result = new HashMap<>();
        PromQueryData data = service.getQueryDataInfo("jingxing_gpu_mem_total_bytes", null);
        if (data == null) return result;
        for (PromQueryResult res : data.getResult()) {
            Map<String, Object> metric = res.getMetric();
            String host = (String) metric.get("host");
            String gpuId = (String) metric.get("gpu_id");
            if (host == null || gpuId == null) continue;
            String key = host + "_" + gpuId;
            String raw = extractPrometheusValue(res);
            if (!StringUtils.hasText(raw)) continue;
            try {
                double val = Double.parseDouble(raw);
                result.put(key, (long) val);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private String extractPrometheusValue(PromQueryResult res) {
        if (res == null || res.getValue() == null || res.getValue().size() < 2) {
            return null;
        }
        Object val = res.getValue().get(1);
        return val != null ? val.toString() : null;
    }

    private String resolveIp(String host, Map<String, Object> metric) {
        String instance = (String) metric.get("instance");
        if (instance != null && instance.contains(":")) {
            return instance.substring(0, instance.indexOf(":"));
        }
        String exported = (String) metric.get("exported_instance");
        if (StringUtils.hasText(exported)) {
            return exported;
        }
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("无法解析节点 {} 的IP，将设置为 null", host);
            return null;
        }
    }

    private Integer inferNodeRole(String hostname) {
        if (LOGIN_NODE_PATTERN.matcher(hostname).matches()) {
            return 0; // 登录节点
        } else if (COMPUTE_NODE_PATTERN.matcher(hostname).matches()) {
            return 1; // 计算节点
        } else if (STORAGE_NODE_PATTERN.matcher(hostname).matches()) {
            return 2; // 存储节点
        }
        return 1; // 默认计算节点
    }

    private String inferNodeType(String hostname, Integer gpuCount, Long memoryMB) {
        if (gpuCount != null && gpuCount > 0) {
            return "GPU节点";
        }
        if (memoryMB != null && memoryMB > 500 * 1024) { // 超过 500GB 视为大内存节点
            return "大内存节点";
        }
        if (hostname.startsWith("cpui")) {
            return "CPU计算节点";
        }
        if (hostname.startsWith("edge")) {
            return "边缘节点";
        }
        return "普通节点";
    }

    // 内部辅助类
    private static class NodeModelInfo {
        private final String model;
        private final String type;
        NodeModelInfo(String model, String type) { this.model = model; this.type = type; }
        String getModel() { return model; }
        String getType() { return type; }
    }

    private static class GpuNodeAgg {
        private final int count;
        private final String model;
        private final Long totalMemoryMB;
        GpuNodeAgg(int count, String model, Long totalMemoryMB) {
            this.count = count;
            this.model = model;
            this.totalMemoryMB = totalMemoryMB;
        }
        int getCount() { return count; }
        String getModel() { return model; }
        Long getTotalMemoryMB() { return totalMemoryMB; }
    }
}