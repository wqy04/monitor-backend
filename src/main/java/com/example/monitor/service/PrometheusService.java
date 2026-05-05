package com.example.monitor.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.monitor.dto.UserPrometheusData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrometheusService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String PROMETHEUS_URL = "http://localhost:9090";

    public UserPrometheusData getUserPrometheusData() {
        Map<String, Double> maxSlots = queryMetric("jingxing_user_max_slots");
        Map<String, Double> runningJobs = queryMetric("jingxing_user_running_jobs");

        List<UserPrometheusData.UserData> users = new ArrayList<>();
        // 合并所有用户
        Map<String, UserPrometheusData.UserData> userMap = new HashMap<>();
        for (Map.Entry<String, Double> entry : maxSlots.entrySet()) {
            String user = entry.getKey();
            UserPrometheusData.UserData data = new UserPrometheusData.UserData();
            data.setUser(user);
            data.setMaxSlots(entry.getValue());
            data.setRunningJobs(runningJobs.getOrDefault(user, 0.0));
            userMap.put(user, data);
        }
        for (Map.Entry<String, Double> entry : runningJobs.entrySet()) {
            String user = entry.getKey();
            if (!userMap.containsKey(user)) {
                UserPrometheusData.UserData data = new UserPrometheusData.UserData();
                data.setUser(user);
                data.setMaxSlots(0.0);
                data.setRunningJobs(entry.getValue());
                userMap.put(user, data);
            } else {
                userMap.get(user).setRunningJobs(entry.getValue());
            }
        }
        users.addAll(userMap.values());

        UserPrometheusData result = new UserPrometheusData();
        result.setUsers(users);
        return result;
    }

    private Map<String, Double> queryMetric(String metricName) {
        String url = PROMETHEUS_URL + "/api/v1/query?query=" + metricName;
        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = JSON.parseObject(response);
        if (!"success".equals(json.getString("status"))) {
            throw new RuntimeException("Failed to query Prometheus: " + response);
        }
        JSONArray results = json.getJSONObject("data").getJSONArray("result");
        Map<String, Double> data = new HashMap<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject result = results.getJSONObject(i);
            String user = result.getJSONObject("metric").getString("user");
            JSONArray value = result.getJSONArray("value");
            double val = Double.parseDouble(value.getString(1));
            data.put(user, val);
        }
        return data;
    }
}