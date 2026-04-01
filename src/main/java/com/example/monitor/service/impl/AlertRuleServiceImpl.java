package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.AlertRule;
import com.example.monitor.mapper.AlertRuleMapper;
import com.example.monitor.service.AlertRuleService;
import org.springframework.stereotype.Service;

@Service
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements AlertRuleService {

    @Override
    public AlertRule getAlertRuleById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getRuleId, id));
    }
}