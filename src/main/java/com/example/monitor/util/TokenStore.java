package com.example.monitor.util;

import com.example.monitor.entity.RefreshToken;
import com.example.monitor.mapper.RefreshTokenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    // blacklist access tokens on logout
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public void saveRefreshToken(String refreshToken, Integer userId) {
        // 先删除该用户的旧token
        refreshTokenMapper.deleteByUserId(userId);

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken(refreshToken);
        tokenEntity.setUserId(userId);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7天过期
        tokenEntity.setCreatedAt(LocalDateTime.now());
        refreshTokenMapper.insert(tokenEntity);
    }

    public Integer getUserIdByRefreshToken(String refreshToken) {
        RefreshToken tokenEntity = refreshTokenMapper.selectByToken(refreshToken);
        if (tokenEntity != null && tokenEntity.getExpiresAt().isAfter(LocalDateTime.now())) {
            return tokenEntity.getUserId();
        }
        return null;
    }

    public void removeRefreshToken(String refreshToken) {
        refreshTokenMapper.deleteByToken(refreshToken);
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
