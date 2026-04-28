package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.service.QueueInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueueController {

    @Autowired
    private QueueInfoService queueInfoService;

    @GetMapping("/queues")
    public Result<Map<String, Object>> listQueues() {
        List<Map<String, Object>> queues = queueInfoService.listQueuesWithPrometheusMetrics();
        Map<String, Object> data = new HashMap<>();
        data.put("total", queues.size());
        data.put("queues", queues);
        return Result.ok(data);
    }
}
