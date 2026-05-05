package com.example.monitor.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPrometheusData {
    private List<UserData> users;

    @Data
    public static class UserData {
        private String user;
        private double maxSlots;
        private double runningJobs;
    }
}