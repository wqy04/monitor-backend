package com.example.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.monitor.dto.Result;
import com.example.monitor.entity.AlarmInfo;
import com.example.monitor.entity.AlertRule;
import com.example.monitor.entity.Cluster;
import com.example.monitor.service.AlarmInfoService;
import com.example.monitor.service.AlertRuleService;
import com.example.monitor.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private ClusterService clusterService;

    @GetMapping("/alerts")
    public Result<Map<String, Object>> listAlerts(@RequestParam(value = "status", required = false) Integer status,
                                                @RequestParam(value = "level", required = false) Integer level,
                                                @RequestParam(value = "target", required = false) String target,
                                                @RequestParam(value = "notice", required = false) String notice,
                                                @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<AlarmInfo> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(AlarmInfo::getStatus, status);
        if (level != null) wrapper.eq(AlarmInfo::getLevel, level);
        if (target != null) wrapper.like(AlarmInfo::getTarget, target);
        if (notice != null) wrapper.like(AlarmInfo::getNotice, notice);
        
        Page<AlarmInfo> page = new Page<>(pageNum, pageSize);
        IPage<AlarmInfo> result = alarmInfoService.page(page, wrapper);
        
        List<Map<String, Object>> enhancedList = new ArrayList<>();
        for (AlarmInfo alarm : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", alarm.getId());
            map.put("notice", alarm.getNotice());
            map.put("updateTime", alarm.getUpdateTime());
            map.put("target", alarm.getTarget());
            map.put("level", alarm.getLevel());
            map.put("status", alarm.getStatus());
            map.put("clusterId", alarm.getClusterId());
            if (alarm.getClusterId() != null) {
                Cluster cluster = clusterService.getById(alarm.getClusterId());
                map.put("clusterName", cluster != null ? cluster.getClusterName() : null);
            } else {
                map.put("clusterName", null);
            }
            enhancedList.add(map);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("list", enhancedList);
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
        return Result.ok("规则删除成功", data);
    }

    @DeleteMapping("/alerts/{id}")
    public Result<Map<String, Object>> deleteAlertById(@PathVariable Integer id) {
        AlarmInfo alarm = alarmInfoService.getAlarmInfoById(id);
        if (alarm == null) {
            return Result.fail(404, "alert not found");
        }
        alarmInfoService.removeById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return Result.ok("告警删除成功", data);
    }
}
