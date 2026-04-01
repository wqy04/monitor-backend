package com.example.monitor.service;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.User;
import java.util.Map;

public interface AuthService {
    Result<Map<String, Object>> register(User user);
    Result<Map<String, Object>> login(String username, String password);
    Result<Map<String, Object>> refreshToken(String refreshToken);
    Result<Void> logout(String accessToken, String refreshToken);
    Result<User> getCurrentUser(Integer userId);
    Result<Map<String, Object>> updatePassword(Integer userId, String password);
}
