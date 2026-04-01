package com.example.monitor.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.HMacJWTSigner;
import cn.hutool.jwt.signers.JWTSigner;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET = "monitor-backend-jwt-secret";
    private static final int ACCESS_EXPIRE_SECONDS = 15 * 60;
    private static final int REFRESH_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    public static String generateAccessToken(Integer userId, String username) {
        return generateToken(userId, username, ACCESS_EXPIRE_SECONDS);
    }

    public static String generateRefreshToken(Integer userId, String username) {
        return generateToken(userId, username, REFRESH_EXPIRE_SECONDS);
    }

    private static String generateToken(Integer userId, String username, int expireSeconds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("iat", DateUtil.currentSeconds());
        payload.put("exp", DateUtil.offsetSecond(new Date(), expireSeconds).getTime() / 1000);

        JWTSigner signer = new HMacJWTSigner("HS256", SECRET.getBytes(StandardCharsets.UTF_8));
        return JWTUtil.createToken(payload, signer);
    }

    public static boolean validToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            jwt.setKey(SECRET.getBytes(StandardCharsets.UTF_8));
            if (!jwt.verify()) {
                return false;
            }
            Object expObj = jwt.getPayload("exp");
            if (expObj != null) {
                long exp = Long.parseLong(String.valueOf(expObj));
                long now = DateUtil.currentSeconds();
                return now <= exp;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static Integer getUserId(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object userId = jwt.getPayload("userId");
            if (userId != null) {
                return Integer.parseInt(String.valueOf(userId));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getUsername(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object username = jwt.getPayload("username");
            return username != null ? username.toString() : null;
        } catch (Exception ignored) {
        }
        return null;
    }
}
