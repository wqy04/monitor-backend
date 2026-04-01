package com.example.monitor.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    // refreshToken -> userId
    private final Map<String, Integer> refreshTokenMap = new ConcurrentHashMap<>();
    // blacklist access tokens on logout
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public void saveRefreshToken(String refreshToken, Integer userId) {
        refreshTokenMap.put(refreshToken, userId);
    }

    public Integer getUserIdByRefreshToken(String refreshToken) {
        return refreshTokenMap.get(refreshToken);
    }

    public void removeRefreshToken(String refreshToken) {
        refreshTokenMap.remove(refreshToken);
    }

    public void blacklistAccessToken(String accessToken) {
        if (accessToken != null) {
            blacklist.add(accessToken);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return accessToken != null && blacklist.contains(accessToken);
    }
}
