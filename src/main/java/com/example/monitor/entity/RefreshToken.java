package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Refresh Token实体
 * 对应数据库表：refresh_tokens
 */
@Data
@TableName("refresh_tokens")
public class RefreshToken {
    /**
     * ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * Refresh Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}