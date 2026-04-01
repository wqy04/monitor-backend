package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户-会话表实体
 * 对应数据库表：user_sessions
 */
@Data
@TableName("user_sessions")
public class UserSession {
    /**
     * 会话ID，主键
     */
    @TableId
    private String sessionId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录节点ID
     */
    private Integer nodeId;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    private LocalDateTime logoutTime;

    /**
     * 会话状态：0-离线，1-在线
     */
    private Integer status;
}