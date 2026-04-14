package com.example.monitor.service;

import com.example.monitor.entity.AlarmInfo;
import com.example.monitor.entity.Cluster;
import com.example.monitor.entity.NodeMonitor;
import com.example.monitor.entity.Queue;

import java.util.List;

/**
 * 集群元数据适配器接口
 * 用于从Prometheus中发现和采集不同类型集群的元数据
 */
public interface ClusterMetadataAdapter {

    /**
     * 判断当前Prometheus中是否存在本适配器能够处理的指标
     * @param promQueryService Prometheus查询服务
     * @return true如果支持，否则false
     */
    boolean supports(PromQueryService promQueryService);

    /**
     * 发现所有集群（返回待保存的Cluster对象，clusterId为null）
     * @param promQueryService Prometheus查询服务
     * @return 发现的集群列表
     */
    List<Cluster> discoverClusters(PromQueryService promQueryService);

    /**
     * 根据集群发现节点
     * @param cluster 已保存或待保存的集群信息（至少包含clusterName）
     * @param promQueryService Prometheus查询服务
     * @return 该集群的节点列表
     */
    List<NodeMonitor> discoverNodes(Cluster cluster, PromQueryService promQueryService);

    /**
     * 根据集群发现队列
     * @param cluster 已保存或待保存的集群信息（至少包含clusterName）
     * @param promQueryService Prometheus查询服务
     * @return 该集群的队列列表
     */
    List<Queue> discoverQueues(Cluster cluster, PromQueryService promQueryService);

    /**
     * 根据集群发现告警
     * @param cluster 已保存或待保存的集群信息（至少包含clusterName）
     * @param promQueryService Prometheus查询服务
     * @return 该集群的告警列表
     */
    List<AlarmInfo> discoverAlerts(Cluster cluster, PromQueryService promQueryService);
}