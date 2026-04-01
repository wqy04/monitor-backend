package com.example.monitor.util;

import cn.hutool.crypto.SecureUtil;

public class PasswordUtil {
    public static String encrypt(String plaintext) {
        return SecureUtil.md5(plaintext);
    }

    public static boolean matches(String plaintext, String encrypted) {
        if (plaintext == null || encrypted == null) {
            return false;
        }
        return encrypt(plaintext).equals(encrypted);
    }
}
