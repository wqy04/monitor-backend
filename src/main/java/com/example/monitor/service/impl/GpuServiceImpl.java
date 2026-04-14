package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.Gpu;
import com.example.monitor.mapper.GpuMapper;
import com.example.monitor.service.GpuService;
import org.springframework.stereotype.Service;

/**
 * GPU 服务实现类
 */
@Service
public class GpuServiceImpl extends ServiceImpl<GpuMapper, Gpu> implements GpuService {

    @Override
    public Gpu findByNodeIdAndGpuIndex(Integer nodeId, Integer gpuIndex) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Gpu>()
                .eq(Gpu::getNodeId, nodeId)
                .eq(Gpu::getGpuIndex, gpuIndex));
    }
}
