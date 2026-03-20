package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户-会话表实体
 * 对应数据库表：user_session
 */
@Data
@TableName("user_session")
public class UserSession {
    /**
     * 会话ID，主键
     */
    @TableId
    private String sessionId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 关联节点ID
     */
    private Long nodeId;

    /**
     * 登入时间
     */
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    private LocalDateTime logoutTime;

    /**
     * 状态：0-离线 1-在线
     */
    private Integer status;
}