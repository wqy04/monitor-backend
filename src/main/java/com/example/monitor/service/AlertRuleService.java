package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.AlertRule;

public interface AlertRuleService extends IService<AlertRule> {
    AlertRule getAlertRuleById(Long id);
}
