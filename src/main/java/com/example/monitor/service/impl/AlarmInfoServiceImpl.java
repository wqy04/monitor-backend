package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.AlarmInfo;
import com.example.monitor.mapper.AlarmInfoMapper;
import com.example.monitor.service.AlarmInfoService;
import org.springframework.stereotype.Service;

@Service
public class AlarmInfoServiceImpl extends ServiceImpl<AlarmInfoMapper, AlarmInfo> implements AlarmInfoService {

    @Override
    public AlarmInfo getAlarmInfoById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<AlarmInfo>()
                .eq(AlarmInfo::getId, id));
    }
}