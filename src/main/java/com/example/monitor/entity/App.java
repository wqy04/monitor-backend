package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 应用静态信息表实体
 * 对应数据库表：apps
 */
@Data
@TableName("apps")
public class App {
    @TableId(type = IdType.AUTO)
    private Integer appId;

    private String appName;

    private String description;

    private Integer clusterId;
}
