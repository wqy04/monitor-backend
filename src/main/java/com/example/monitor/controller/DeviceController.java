package com.example.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.monitor.dto.Result;
import com.example.monitor.entity.DeviceMonitor;
import com.example.monitor.service.DeviceMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/monitor/api")
public class DeviceController {

    @Autowired
    private DeviceMonitorService deviceMonitorService;

    @GetMapping("/devices")
    public Result<List<DeviceMonitor>> listDevices(@RequestParam(value = "clusterId", required = false) Integer clusterId,
                                                   @RequestParam(value = "deviceType", required = false) String deviceType,
                                                   @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<DeviceMonitor> wrapper = new LambdaQueryWrapper<>();
        if (clusterId != null) wrapper.eq(DeviceMonitor::getClusterId, clusterId);
        if (deviceType != null) wrapper.eq(DeviceMonitor::getDeviceType, deviceType);
        if (status != null) wrapper.eq(DeviceMonitor::getStatus, status);
        List<DeviceMonitor> list = deviceMonitorService.list(wrapper);
        return Result.ok(list);
    }

    @GetMapping("/devices/{deviceId}")
    public Result<Map<String, Object>> getDevice(@PathVariable Integer deviceId) {
        DeviceMonitor device = deviceMonitorService.getById(deviceId);
        if (device == null) {
            return Result.fail(404, "device not found");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", device.getDeviceId());
        data.put("deviceName", device.getDeviceName());
        data.put("deviceType", device.getDeviceType());
        data.put("deviceIp", device.getDeviceIp());
        data.put("status", device.getStatus());
        data.put("model", device.getModel());
        data.put("clusterId", device.getClusterId());
        data.put("monitorUrl", device.getMonitorUrl());
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("inputVoltage", 220.5);
        metrics.put("outputVoltage", 220.3);
        metrics.put("loadPercent", 45.2);
        metrics.put("batteryCharge", 98.5);
        metrics.put("temperature", 28.3);
        data.put("metrics", metrics);
        data.put("lastUpdate", device.getLastUpdate());

        return Result.ok(data);
    }

    @GetMapping("/devices/{deviceId}/metrics/history")
    public Result<Map<String, Object>> deviceHistory(@PathVariable Integer deviceId,
                                                      @RequestParam("metrics") String metrics,
                                                      @RequestParam(value = "start", required = false) Long start,
                                                      @RequestParam(value = "end", required = false) Long end,
                                                      @RequestParam(value = "step", required = false) Integer step) {
        DeviceMonitor device = deviceMonitorService.getById(deviceId);
        if (device == null) {
            return Result.fail(404, "device not found");
        }

        if (start == null) {
            start = System.currentTimeMillis() / 1000 - 3600;
        }
        if (end == null) {
            end = System.currentTimeMillis() / 1000;
        }
        if (step == null || step <= 0) {
            step = 60;
        }

        String[] metricKeys = metrics.split(",");
        List<Map<String, Object>> metricData = new ArrayList<>();
        for (String metric : metricKeys) {
            Map<String, Object> m = new HashMap<>();
            m.put("metricName", metric.trim());
            m.put("unit", "%");
            List<Map<String, Object>> values = new ArrayList<>();
            for (long t = start; t <= end; t += step) {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", t);
                point.put("value", Math.round(Math.random() * 10000) / 100.0);
                values.add(point);
            }
            m.put("values", values);
            metricData.add(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", device.getDeviceId());
        data.put("metrics", metricData);
        return Result.ok(data);
    }
}
