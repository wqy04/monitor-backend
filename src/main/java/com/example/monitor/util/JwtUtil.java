package com.example.monitor.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.HMacJWTSigner;
import cn.hutool.jwt.signers.JWTSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.access-expire}")
    private int ACCESS_EXPIRE_SECONDS;

    @Value("${jwt.refresh-expire}")
    private int REFRESH_EXPIRE_SECONDS;

    public String generateAccessToken(Integer userId, String username) {
        return generateToken(userId, username, ACCESS_EXPIRE_SECONDS, "access");
    }

    public String generateRefreshToken(Integer userId, String username) {
        return generateToken(userId, username, REFRESH_EXPIRE_SECONDS, "refresh");
    }

    private String generateToken(Integer userId, String username, int expireSeconds, String type) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("type", type);
        payload.put("iat", DateUtil.currentSeconds());
        payload.put("exp", DateUtil.offsetSecond(new Date(), expireSeconds).getTime() / 1000);

        JWTSigner signer = new HMacJWTSigner("HmacSHA256", SECRET.getBytes(StandardCharsets.UTF_8));
        return JWTUtil.createToken(payload, signer);
    }

    public boolean validToken(String token) {
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

    public boolean isTokenExpiringSoon(String token, int bufferSeconds) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object expObj = jwt.getPayload("exp");
            if (expObj != null) {
                long exp = Long.parseLong(String.valueOf(expObj));
                long now = DateUtil.currentSeconds();
                return (exp - now) <= bufferSeconds;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String getTokenType(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object type = jwt.getPayload("type");
            return type != null ? type.toString() : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    public Integer getUserId(String token) {
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

    public String getUsername(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object username = jwt.getPayload("username");
            return username != null ? username.toString() : null;
        } catch (Exception ignored) {
        }
        return null;
    }
}
