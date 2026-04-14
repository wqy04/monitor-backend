package com.example.monitor.service;

import com.example.monitor.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据初始化组件，在应用启动时自动执行（支持更新已有记录）
 *
 * @author wqy
 * @date 2026/4/13
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private PromQueryService promQueryService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private NodeMonitorService nodeMonitorService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private AlarmInfoService alarmInfoService;

    @Autowired
    private AppService appService;

    @Autowired
    private ClusterUserService clusterUserService;

    @Autowired
    private GpuService gpuService;

    @Autowired
    private NodeQueueService nodeQueueService;

    @Autowired
    private JobSchedulerService jobSchedulerService;

    @Autowired
    private List<ClusterMetadataAdapter> adapters;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化集群静态数据...");
        for (ClusterMetadataAdapter adapter : adapters) {
            if (!adapter.supports(promQueryService)) {
                log.debug("适配器 {} 不适用于当前环境，跳过", adapter.getClass().getSimpleName());
                continue;
            }
            log.info("使用适配器 {} 进行初始化", adapter.getClass().getSimpleName());

            // 1. 发现并保存/更新集群
            List<Cluster> discoveredClusters = adapter.discoverClusters(promQueryService);
            for (Cluster cluster : discoveredClusters) {
                Cluster existing = clusterService.getByClusterName(cluster.getClusterName());
                if (existing == null) {
                    clusterService.save(cluster);
                    log.info("新增集群: {}", cluster.getClusterName());
                } else {
                    // 更新集群的可变字段（供应商、描述等）
                    existing.setVendor(cluster.getVendor());
                    existing.setDescription(cluster.getDescription());
                    clusterService.updateById(existing);
                    cluster.setClusterId(existing.getClusterId()); // 复用ID供后续使用
                    log.info("更新集群: {}", cluster.getClusterName());
                }

                // 2. 保存/更新节点（构建节点名->ID映射）
                List<NodeMonitor> nodes = adapter.discoverNodes(cluster, promQueryService);
                Map<String, Integer> nodeNameToId = new HashMap<>();
                for (NodeMonitor node : nodes) {
                    node.setClusterId(cluster.getClusterId());
                    NodeMonitor existingNode = nodeMonitorService.findByNodeNameAndClusterId(
                            node.getNodeName(), cluster.getClusterId());
                    if (existingNode == null) {
                        nodeMonitorService.save(node);
                        nodeNameToId.put(node.getNodeName(), node.getNodeId());
                        log.debug("新增节点: {}", node.getNodeName());
                    } else {
                        // 更新节点可变字段
                        existingNode.setNodeIp(node.getNodeIp());
                        existingNode.setCpuTotal(node.getCpuTotal());
                        existingNode.setMemoryTotal(node.getMemoryTotal());
                        existingNode.setSlotsMax(node.getSlotsMax());
                        existingNode.setGpuCount(node.getGpuCount());
                        existingNode.setGpuModel(node.getGpuModel());
                        existingNode.setGpuMemoryTotal(node.getGpuMemoryTotal());
                        existingNode.setCpuModel(node.getCpuModel());
                        existingNode.setOsType(node.getOsType());
                        existingNode.setNodeRole(node.getNodeRole());
                        existingNode.setNodeType(node.getNodeType());
                        existingNode.setPartition(node.getPartition());
                        nodeMonitorService.updateById(existingNode);
                        nodeNameToId.put(node.getNodeName(), existingNode.getNodeId());
                        log.debug("更新节点: {}", node.getNodeName());
                    }
                }
                log.info("节点处理完成，共 {} 个", nodeNameToId.size());

                // 3. 保存/更新队列（构建队列名->ID映射）
                List<Queue> queues = adapter.discoverQueues(cluster, promQueryService);
                Map<String, Integer> queueNameToId = new HashMap<>();
                for (Queue queue : queues) {
                    queue.setClusterId(cluster.getClusterId());
                    Queue existingQueue = queueService.findByQueueNameAndClusterId(
                            queue.getQueueName(), cluster.getClusterId());
                    if (existingQueue == null) {
                        queueService.save(queue);
                        queueNameToId.put(queue.getQueueName(), queue.getQueueId());
                        log.debug("新增队列: {}", queue.getQueueName());
                    } else {
                        // 更新队列可变字段
                        existingQueue.setNice(queue.getNice());
                        existingQueue.setPriority(queue.getPriority());
                        existingQueue.setStatus(queue.getStatus());
                        existingQueue.setDescription(queue.getDescription());
                        queueService.updateById(existingQueue);
                        queueNameToId.put(queue.getQueueName(), existingQueue.getQueueId());
                        log.debug("更新队列: {}", queue.getQueueName());
                    }
                }
                log.info("队列处理完成，共 {} 个", queueNameToId.size());

                // 4. 保存告警（每次全量保存，不去重，保留历史告警记录）
                List<AlarmInfo> alarms = adapter.discoverAlerts(cluster, promQueryService);
                for (AlarmInfo alarm : alarms) {
                    alarm.setClusterId(cluster.getClusterId());
                    alarmInfoService.save(alarm);
                }
                log.info("告警处理完成，共 {} 条", alarms.size());

                // 5. 扩展数据采集（仅对 JingxingAdapter 实例执行）
                if (adapter instanceof JingxingAdapter) {
                    JingxingAdapter jingxingAdapter = (JingxingAdapter) adapter;

                    // 5.1 应用信息
                    List<App> apps = jingxingAdapter.discoverApps(cluster, promQueryService);
                    for (App app : apps) {
                        app.setClusterId(cluster.getClusterId());
                        App existingApp = appService.findByAppNameAndClusterId(app.getAppName(), cluster.getClusterId());
                        if (existingApp == null) {
                            appService.save(app);
                            log.debug("新增应用: {}", app.getAppName());
                        } else {
                            // 更新应用描述
                            existingApp.setDescription(app.getDescription());
                            appService.updateById(existingApp);
                            log.debug("更新应用: {}", app.getAppName());
                        }
                    }
                    log.info("应用处理完成，共 {} 个", apps.size());

                    // 5.2 集群用户
                    List<ClusterUser> clusterUsers = jingxingAdapter.discoverClusterUsers(cluster, promQueryService);
                    for (ClusterUser user : clusterUsers) {
                        user.setClusterId(cluster.getClusterId());
                        ClusterUser existingUser = clusterUserService.findByUsernameAndClusterId(user.getUsername(), cluster.getClusterId());
                        if (existingUser == null) {
                            clusterUserService.save(user);
                            log.debug("新增用户: {}", user.getUsername());
                        } else {
                            // 用户信息一般不需要更新（可扩展 max_slots 等）
                            // 若有其他可变字段，可在此更新
                            log.debug("用户已存在: {}", user.getUsername());
                        }
                    }
                    log.info("集群用户处理完成，共 {} 个", clusterUsers.size());

                    // 5.3 GPU 明细（需要节点ID映射）
                    List<Gpu> gpus = jingxingAdapter.discoverGpus(cluster, promQueryService, nodeNameToId);
                    for (Gpu gpu : gpus) {
                        Gpu existingGpu = gpuService.findByNodeIdAndGpuIndex(gpu.getNodeId(), gpu.getGpuIndex());
                        if (existingGpu == null) {
                            gpuService.save(gpu);
                            log.debug("新增GPU: nodeId={}, index={}", gpu.getNodeId(), gpu.getGpuIndex());
                        } else {
                            // 更新GPU可变字段
                            existingGpu.setGpuModel(gpu.getGpuModel());
                            existingGpu.setMemoryTotal(gpu.getMemoryTotal());
                            existingGpu.setStatus(gpu.getStatus());
                            gpuService.updateById(existingGpu);
                            log.debug("更新GPU: nodeId={}, index={}", gpu.getNodeId(), gpu.getGpuIndex());
                        }
                    }
                    log.info("GPU明细处理完成，共 {} 个", gpus.size());

                    // 5.4 节点-队列关联（联合主键，存在即跳过，无需更新）
                    List<NodeQueue> nodeQueues = jingxingAdapter.discoverNodeQueues(
                            cluster, promQueryService, nodeNameToId, queueNameToId);
                    for (NodeQueue nq : nodeQueues) {
                        if (nodeQueueService.findByNodeIdAndQueueName(nq.getNodeId(), nq.getQueueName()) == null) {
                            nodeQueueService.save(nq);
                            log.debug("新增节点队列关联: nodeId={}, queue={}", nq.getNodeId(), nq.getQueueName());
                        }
                    }
                    log.info("节点队列关联处理完成，共 {} 条", nodeQueues.size());

                    // 5.5 作业调度器
                    JobScheduler scheduler = jingxingAdapter.discoverJobScheduler(cluster);
                    scheduler.setClusterId(cluster.getClusterId());
                    JobScheduler existingScheduler = jobSchedulerService.findByClusterId(cluster.getClusterId());
                    if (existingScheduler == null) {
                        jobSchedulerService.save(scheduler);
                        log.info("新增作业调度器");
                    } else {
                        // 更新调度器状态等（若有变化）
                        existingScheduler.setSchedulerName(scheduler.getSchedulerName());
                        existingScheduler.setStatus(scheduler.getStatus());
                        jobSchedulerService.updateById(existingScheduler);
                        log.info("更新作业调度器");
                    }
                }
            }
        }
        log.info("静态数据初始化完成");
    }
}