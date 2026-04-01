package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表实体
 * 对应数据库表：users
 */
@Data // Lombok注解，自动生成get/set/toString，毕设必加（简化代码）
@TableName("users") // 指定数据库表名
public class User {
    /**
     * 用户ID，主键
     */
    @TableId(type = IdType.AUTO) // 主键自增，和配置文件一致
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户角色：0-系统管理员 1-普通用户
     */
    private String userRole;

    /**
     * 状态：active-激活 inactive-未激活
     */
    private String status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;

    /**
     * 账号创建时间
     */
    private LocalDateTime createTime;

    /**
     * 用户所属学院
     */
    private String department;
}