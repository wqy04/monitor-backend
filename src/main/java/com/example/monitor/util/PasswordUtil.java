package com.example.monitor.util;

import cn.hutool.crypto.SecureUtil;

public class PasswordUtil {
    public static String encrypt(String plaintext) {
        // 改用 SHA-256 哈希算法
        return SecureUtil.sha256(plaintext);
    }

    public static boolean matches(String plaintext, String encrypted) {
        if (plaintext == null || encrypted == null) {
            return false;
        }
        return encrypt(plaintext).equals(encrypted);
    }
}