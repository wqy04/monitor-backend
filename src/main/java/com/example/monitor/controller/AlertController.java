package com.example.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.monitor.dto.Result;
import com.example.monitor.entity.AlarmInfo;
import com.example.monitor.entity.AlertRule;
import com.example.monitor.service.AlarmInfoService;
import com.example.monitor.service.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AlertController {

    @Autowired
    private AlarmInfoService alarmInfoService;

    @Autowired
    private AlertRuleService alertRuleService;

    @GetMapping("/alerts")
    public Result<Map<String, Object>> listAlerts(@RequestParam(value = "status", required = false) Integer status,
                                                   @RequestParam(value = "level", required = false) Integer level,
                                                   @RequestParam(value = "target", required = false) String target,
                                                   @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<AlarmInfo> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(AlarmInfo::getStatus, status);
        if (level != null) wrapper.eq(AlarmInfo::getLevel, level);
        if (target != null) wrapper.like(AlarmInfo::getTarget, target);
        List<AlarmInfo> list = alarmInfoService.list(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", list.size());
        data.put("list", list);
        return Result.ok(data);
    }

    @PutMapping("/alerts/{id}/status")
    public Result<Void> updateAlertStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        AlarmInfo alarm = alarmInfoService.getById(id);
        if (alarm == null) {
            return Result.fail(404, "alert not found");
        }
        alarm.setStatus(status);
        alarmInfoService.updateById(alarm);
        return Result.ok("更新成功", null);
    }

    @GetMapping("/alerts-rules/rules")
    public Result<List<AlertRule>> listAlertRules() {
        List<AlertRule> rules = alertRuleService.list();
        return Result.ok(rules);
    }

    @PostMapping("/alert-rules")
    public Result<Map<String, Object>> createAlertRule(@RequestBody AlertRule rule) {
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        alertRuleService.save(rule);
        Map<String, Object> data = new HashMap<>();
        data.put("ruleId", rule.getRuleId());
        return Result.ok("规则创建成功", data);
    }

    @PutMapping("/alert-rules/{ruleId}")
    public Result<Map<String, Object>> updateAlertRule(@PathVariable Integer ruleId,
                                                       @RequestBody AlertRule rule) {
        AlertRule existing = alertRuleService.getById(ruleId);
        if (existing == null) {
            return Result.fail(404, "rule not found");
        }
        rule.setRuleId(ruleId);
        rule.setUpdateTime(LocalDateTime.now());
        alertRuleService.updateById(rule);
        Map<String, Object> data = new HashMap<>();
        data.put("ruleId", ruleId);
        return Result.ok("规则更新成功", data);
    }

    @DeleteMapping("/alert-rules/{ruleId}")
    public Result<Map<String, Object>> deleteAlertRule(@PathVariable Integer ruleId) {
        AlertRule existing = alertRuleService.getById(ruleId);
        if (existing == null) {
            return Result.fail(404, "rule not found");
        }
        alertRuleService.removeById(ruleId);
        Map<String, Object> data = new HashMap<>();
        data.put("ruleId", ruleId);
        return Result.ok("规则创建成功", data);
    }
}
