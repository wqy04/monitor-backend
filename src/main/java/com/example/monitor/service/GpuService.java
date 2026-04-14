package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.Gpu;

/**
 * GPU 服务接口
 */
public interface GpuService extends IService<Gpu> {
    Gpu findByNodeIdAndGpuIndex(Integer nodeId, Integer gpuIndex);
}
