package com.example.monitor.service;

import com.example.monitor.entity.*;
import com.example.monitor.entity.Queue;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Slurm 调度器适配器（通用 Slurm 集群）
 * 解析 prometheus-slurm-exporter 暴露的指标，采集静态数据
 */
@Slf4j
@Component
public class SlurmAdapter implements ClusterMetadataAdapter {
    static {
        System.out.println("========== SlurmAdapter 类被加载 ==========");
    }

    private static final long BYTES_TO_MB = 1024 * 1024;
    // 节点名模式匹配
    private static final Pattern COMPUTE_NODE_PATTERN = Pattern.compile("^comput\\d+$");
    private static final Pattern LOGIN_NODE_PATTERN = Pattern.compile("^mgt\\d+$");
    private static final Pattern STORAGE_NODE_PATTERN = Pattern.compile("^mem\\d+$");

    /**
     * 检测当前环境是否支持 Slurm 适配器
     * 通过查询 slurm_node_cpu_total 指标是否存在来判断
     */
    @Override
    public boolean supports(PromQueryService promQueryService) {
        System.out.println("SlurmAdapter.supports 被调用");
        PromQueryData data = promQueryService.getQueryDataInfo("slurm_node_up", null);
        return data != null && !CollectionUtils.isEmpty(data.getResult());
    }

    /**
     * 发现集群信息（由于指标中无集群名，这里创建一个默认集群）
     */
    @Override
    public List<Cluster> discoverClusters(PromQueryService promQueryService) {
        List<Cluster> clusters = new ArrayList<>();
        // 获取任意一个节点的 job 和 instance 作为集群标识
        PromQueryData nodeData = promQueryService.getQueryDataInfo("slurm_node_cpu_cores", null);
        if (nodeData == null || CollectionUtils.isEmpty(nodeData.getResult())) {
            return clusters;
        }
        PromQueryResult sample = nodeData.getResult().get(0);
        Map<String, Object> metric = sample.getMetric();
        String job = (String) metric.get("job");
        String instance = (String) metric.get("instance");

        Cluster cluster = new Cluster();
        cluster.setClusterName("slurm-cluster");          // 可改为配置文件注入
        cluster.setPrometheusJob(job);
        cluster.setInstance(instance);
        cluster.setVendor("Slurm");
        cluster.setDescription("Generic Slurm cluster");
        cluster.setMasterNode(null);                      // 无法从指标获取
        clusters.add(cluster);
        return clusters;
    }

    /**
     * 发现所有节点信息
     */
    @Override
    public List<NodeMonitor> discoverNodes(Cluster cluster, PromQueryService promQueryService) {
        List<NodeMonitor> nodes = new ArrayList<>();

        // 1. 获取节点 CPU 总数
        Map<String, Integer> cpuTotalMap = queryNodeMetricMapInt(promQueryService, "slurm_node_cpu_cores", "host");
        Map<String, Long> memTotalMap = queryNodeMetricMapLong(promQueryService, "slurm_node_mem_total_bytes", "host");
        // 3. 获取节点状态（用于后续告警，但节点实体中不存储状态）
        // 4. 遍历节点名（取 CPU 指标的节点集合）
        for (String nodeName : cpuTotalMap.keySet()) {
            NodeMonitor node = new NodeMonitor();
            node.setNodeName(nodeName);
            node.setClusterId(cluster.getClusterId());
            // IP 地址
            String ip = resolveIp(nodeName);
            node.setNodeIp(ip != null ? ip : "unknown");
            // CPU
            Integer cpuTotal = cpuTotalMap.get(nodeName);
            node.setCpuTotal(cpuTotal);
            node.setSlotsMax(cpuTotal);   // 槽位数默认等于 CPU 核数
            // 内存
            Long memMB = memTotalMap.get(nodeName);
            node.setMemoryTotal(memMB);
            // 角色和类型
            node.setNodeRole(inferNodeRole(nodeName));
            node.setNodeType(inferNodeType(nodeName, null, memMB));
            // GPU 信息（本集群无 GPU）
            node.setGpuCount(0);
            node.setGpuModel(null);
            node.setGpuMemoryTotal(null);
            // 分区：根据 CPU 总数推断（示例集群：128核 → amd128c，256核 → amd256）
            node.setPartition(inferPartitionByCpu(cpuTotal));
            // 其他字段留空
            node.setCpuModel(null);
            node.setOsType(null);

            log.debug("采集节点: {} CPU={} 内存={}MB 分区={}", nodeName, cpuTotal, memMB, node.getPartition());
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * 发现队列（分区）信息
     */
    @Override
    public List<Queue> discoverQueues(Cluster cluster, PromQueryService promQueryService) {
        List<Queue> queues = new ArrayList<>();
        // 获取分区总 CPU 指标（包含分区名）
        PromQueryData partitionData = promQueryService.getQueryDataInfo("slurm_queue_info", null);
        if (partitionData == null) return queues;

        for (PromQueryResult result : partitionData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String partitionName = (String) metric.get("queue");
            if (!StringUtils.hasText(partitionName)) continue;

            Queue queue = new Queue();
            queue.setQueueName(partitionName);
            queue.setClusterId(cluster.getClusterId());
            queue.setNice(0);                // 指标无，设默认
            queue.setPriority(0);
            queue.setStatus("ACTIVE");       // 默认活跃
            queue.setDescription(null);
            queues.add(queue);
        }
        return queues;
    }

    /**
     * 发现告警：节点状态异常
     */
    @Override
    public List<AlarmInfo> discoverAlerts(Cluster cluster, PromQueryService promQueryService) {
        List<AlarmInfo> alarms = new ArrayList<>();
        // 获取节点状态（可以从 slurm_node_cpu_total 的 status 标签获取）
        PromQueryData nodeData = promQueryService.getQueryDataInfo("slurm_node_up", null);
        if (nodeData == null) return alarms;

        for (PromQueryResult result : nodeData.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String nodeName = (String) metric.get("host");
            String status = (String) metric.get("status");
            if (nodeName == null) continue;

            // 如果状态不是正常状态（idle/mix/alloc），则触发告警
            if (status != null && !isNormalNodeStatus(status)) {
                AlarmInfo alarm = new AlarmInfo();
                alarm.setNotice(String.format("节点 %s 状态异常: %s", nodeName, status));
                alarm.setTarget(nodeName);
                alarm.setLevel(status.contains("down") ? 3 : 2);
                alarm.setStatus(0);  // 未解决
                alarm.setUpdateTime(LocalDateTime.now());
                alarm.setClusterId(cluster.getClusterId());
                alarms.add(alarm);
            }
        }
        return alarms;
    }

    // ==================== 扩展方法（供 DataInitializer 调用） ====================

    /**
     * 应用信息（Slurm 无此概念，返回空列表）
     */
    public List<App> discoverApps(Cluster cluster, PromQueryService promQueryService) {
        return Collections.emptyList();
    }

    /**
     * 集群用户：从 slurm_account_fairshare 的 account 标签提取
     */
    public List<ClusterUser> discoverClusterUsers(Cluster cluster, PromQueryService promQueryService) {
        List<ClusterUser> users = new ArrayList<>();
        PromQueryData data = promQueryService.getQueryDataInfo("slurm_account_fairshare", null);
        if (data == null) return users;

        Set<String> uniqueUsers = new HashSet<>();
        for (PromQueryResult result : data.getResult()) {
            Map<String, Object> metric = result.getMetric();
            String account = (String) metric.get("account");
            if (StringUtils.hasText(account) && uniqueUsers.add(account)) {
                ClusterUser user = new ClusterUser();
                user.setUsername(account);
                user.setClusterId(cluster.getClusterId());
                users.add(user);
            }
        }
        return users;
    }

    /**
     * GPU 明细（Slurm 无 GPU 指标，返回空列表）
     */
    public List<Gpu> discoverGpus(Cluster cluster, PromQueryService promQueryService,
                                  Map<String, Integer> nodeNameToIdMap) {
        return Collections.emptyList();
    }

    /**
     * 节点-队列关联：根据节点的 partition（由CPU核数推断）与队列名建立映射
     * 注意：node_queues 表只需要队列名字符串，无需队列ID
     */
    public List<NodeQueue> discoverNodeQueues(Cluster cluster, PromQueryService promQueryService,
                                            Map<String, Integer> nodeNameToIdMap,
                                            Map<String, Integer> queueNameToIdMap) {
        List<NodeQueue> nodeQueues = new ArrayList<>();
        // 获取节点CPU核数（用于推断分区）
        Map<String, Integer> nodeCpuMap = queryNodeMetricMapInt(promQueryService, "slurm_node_cpu_cores", "node");
        
        for (Map.Entry<String, Integer> entry : nodeCpuMap.entrySet()) {
            String nodeName = entry.getKey();
            Integer cpuTotal = entry.getValue();
            String partition = inferPartitionByCpu(cpuTotal);  // 推断队列名
            if (partition == null) {
                log.debug("节点 {} CPU核数{} 无法推断队列，跳过", nodeName, cpuTotal);
                continue;
            }
            
            Integer nodeId = nodeNameToIdMap.get(nodeName);
            if (nodeId == null) {
                log.warn("节点 {} 未在 nodeNameToIdMap 中找到，跳过队列关联", nodeName);
                continue;
            }
            
            NodeQueue nq = new NodeQueue();
            nq.setNodeId(nodeId);
            nq.setQueueName(partition);   // 直接使用队列名字符串
            nodeQueues.add(nq);
            log.debug("建立节点-队列关联: 节点 {} (id={}) -> 队列 {}", nodeName, nodeId, partition);
        }
        
        log.info("为 Slurm 集群生成了 {} 条节点-队列关联记录", nodeQueues.size());
        return nodeQueues;
    }

    /**
     * 作业调度器信息（简单返回默认状态）
     */
    public JobScheduler discoverJobScheduler(Cluster cluster) {
        JobScheduler scheduler = new JobScheduler();
        scheduler.setSchedulerName("Slurm");
        scheduler.setStatus(0);
        scheduler.setClusterId(cluster.getClusterId());
        return scheduler;
    }

    // ==================== 辅助方法 ====================

    /**
     * 查询指标返回 Map<节点名, Integer 值>
     */
    private Map<String, Integer> queryNodeMetricMapInt(PromQueryService service, String metricName, String labelKey) {
        Map<String, Integer> result = new HashMap<>();
        PromQueryData data = service.getQueryDataInfo(metricName, null);
        if (data == null) return result;
        for (PromQueryResult res : data.getResult()) {
            String node = (String) res.getMetric().get(labelKey);
            if (node == null) continue;
            String raw = extractPrometheusValue(res);
            if (!StringUtils.hasText(raw)) continue;
            try {
                double val = Double.parseDouble(raw);
                result.put(node, (int) val);
            } catch (NumberFormatException e) {
                log.warn("解析指标 {} 的值失败: {}", metricName, raw);
            }
        }
        return result;
    }

    private Map<String, Long> queryNodeMetricMapLong(PromQueryService service, String metricName, String labelKey) {
        Map<String, Long> result = new HashMap<>();
        PromQueryData data = service.getQueryDataInfo(metricName, null);
        if (data == null) return result;
        for (PromQueryResult res : data.getResult()) {
            String node = (String) res.getMetric().get(labelKey);
            if (node == null) continue;
            String raw = extractPrometheusValue(res);
            if (!StringUtils.hasText(raw)) continue;
            try {
                double val = Double.parseDouble(raw);
                result.put(node, (long) val);
            } catch (NumberFormatException e) {
                log.warn("解析指标 {} 的值失败: {}", metricName, raw);
            }
        }
        return result;
    }

    private String extractPrometheusValue(PromQueryResult res) {
        if (res == null || res.getValue() == null || res.getValue().size() < 2) return null;
        Object val = res.getValue().get(1);
        return val != null ? val.toString() : null;
    }

    private String resolveIp(String hostname) {
        try {
            return InetAddress.getByName(hostname).getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("无法解析节点 {} 的 IP", hostname);
            return null;
        }
    }

    private Integer inferNodeRole(String hostname) {
        if (LOGIN_NODE_PATTERN.matcher(hostname).matches()) return 0;
        if (COMPUTE_NODE_PATTERN.matcher(hostname).matches()) return 1;
        if (STORAGE_NODE_PATTERN.matcher(hostname).matches()) return 2;
        return 1;  // 默认计算节点
    }

    private String inferNodeType(String hostname, Integer gpuCount, Long memoryMB) {
        if (gpuCount != null && gpuCount > 0) return "GPU节点";
        if (memoryMB != null && memoryMB > 500 * 1024) return "大内存节点";
        if (hostname.startsWith("comput")) return "CPU计算节点";
        return "普通节点";
    }

    /**
     * 根据节点 CPU 总数推断所属分区（示例集群固定映射）
     */
    private String inferPartitionByCpu(Integer cpuTotal) {
        if (cpuTotal == null) return null;
        if (cpuTotal == 128) return "amd128c";
        if (cpuTotal == 256) return "amd256";
        return "normal";
    }

    private boolean isNormalNodeStatus(String status) {
        return "idle".equalsIgnoreCase(status) ||
               "mix".equalsIgnoreCase(status) ||
               "alloc".equalsIgnoreCase(status);
    }
}