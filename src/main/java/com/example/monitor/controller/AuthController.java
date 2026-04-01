package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.User;
import com.example.monitor.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/monitor/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody User user) {
        return authService.register(user);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        return authService.login(username, password);
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestBody(required = false) Map<String, String> body) {
        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        String refreshToken = body != null ? body.get("refreshToken") : null;
        return authService.logout(accessToken, refreshToken);
    }

    @GetMapping("/current-user")
    public Result<User> getCurrentUser(@RequestParam("id") Integer userId) {
        return authService.getCurrentUser(userId);
    }

    @PutMapping("/password")
    public Result<Map<String, Object>> updatePassword(@RequestBody Map<String, Object> body) {
        Integer userId = body.get("id") == null ? null : Integer.parseInt(body.get("id").toString());
        String password = (String) body.get("password");
        return authService.updatePassword(userId, password);
    }
}
