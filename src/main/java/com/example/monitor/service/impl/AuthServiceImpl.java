package com.example.monitor.service.impl;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.User;
import com.example.monitor.service.AuthService;
import com.example.monitor.service.UserService;
import com.example.monitor.util.JwtUtil;
import com.example.monitor.util.PasswordUtil;
import com.example.monitor.util.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Result<Map<String, Object>> register(User user) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.fail(400, "username/password required");
        }

        User existing = userService.getUserByUsername(user.getUsername());
        if (existing != null) {
            return Result.fail(409, "username already exists");
        }

        user.setPassword(PasswordUtil.encrypt(user.getPassword()));
        user.setStatus("active");
        user.setUserRole(1);
        user.setCreateTime(LocalDateTime.now());

        boolean saved = userService.save(user);
        if (!saved) {
            return Result.fail(500, "register failed");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        return Result.ok("注册成功", data);
    }

    @Override
    public Result<Map<String, Object>> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.fail(400, "请填写用户名和密码");
        }

        User user = userService.getUserByUsername(username);
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getUsername());
        tokenStore.saveRefreshToken(refreshToken, user.getUserId());

        user.setLastLogin(LocalDateTime.now());
        userService.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("userRole", user.getUserRole());
        userMap.put("department", user.getDepartment());

        data.put("user", userMap);

        return Result.ok("登录成功", data);
    }

    @Override
    public Result<Map<String, Object>> refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return Result.fail(400, "refreshToken required");
        }

        Integer userId = tokenStore.getUserIdByRefreshToken(refreshToken);
        if (userId == null || !jwtUtil.validToken(refreshToken)) {
            return Result.fail(401, "refreshToken无效");
        }

        // 检查token类型
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            return Result.fail(401, "Invalid token type");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getUsername());

        tokenStore.removeRefreshToken(refreshToken);
        tokenStore.saveRefreshToken(newRefreshToken, user.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        return Result.ok("刷新成功", data);
    }

    @Override
    public Result<Void> logout(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            tokenStore.blacklistAccessToken(accessToken);
        }
        if (StringUtils.hasText(refreshToken)) {
            tokenStore.removeRefreshToken(refreshToken);
        }
        return Result.ok(null);
    }

    @Override
    public Result<User> getCurrentUser(Integer userId) {
        if (userId == null) {
            return Result.fail(400, "userId required");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.ok("返回用户信息成功", user);
    }

    @Override
    public Result<Map<String, Object>> updatePassword(String username, String password) {
        if (username == null || !StringUtils.hasText(password)) {
            return Result.fail(400, "缺少用户名或密码");
        }

        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        user.setPassword(PasswordUtil.encrypt(password));
        userService.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        return Result.ok("密码修改成功", data);
    }
}
