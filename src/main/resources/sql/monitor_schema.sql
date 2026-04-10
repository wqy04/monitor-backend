-- Active: 1755161368222@@127.0.0.1@3306@monitor_db
-- ======================================================
-- 数据库: 集群管理平台
-- 说明: 根据设计方案生成的建表脚本（已统一改为下划线命名）
-- 版本: MySQL 5.7+
-- ======================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 集群表
DROP TABLE IF EXISTS `clusters`;
CREATE TABLE `clusters` (
    `cluster_id`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '集群ID，主键',
    `cluster_name` VARCHAR(32)  NOT NULL COMMENT '集群名称',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '集群描述',
    PRIMARY KEY (`cluster_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='集群信息表';

-- 2. 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `user_id`     INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
    `username`   VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `user_role`   VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT '用户角色：0-系统管理员，1-普通用户',
    `status`     VARCHAR(32)  NOT NULL DEFAULT 'active' COMMENT '账号状态：active-激活，inactive-未激活',
    `last_login`  DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
    `department` VARCHAR(32)           DEFAULT NULL COMMENT '所属学院',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户信息表';

-- 3. 节点监控数据表（静态数据）
DROP TABLE IF EXISTS `nodes`;
CREATE TABLE `nodes` (
    `node_id`          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '节点ID，主键',
    `node_name`        VARCHAR(32)  NOT NULL COMMENT '节点名',
    `node_ip`          VARCHAR(32)  NOT NULL COMMENT '节点IP',
    `partition`       VARCHAR(32)           DEFAULT NULL COMMENT '所属队列',
    `cpu_total`        INT UNSIGNED          DEFAULT NULL COMMENT '总CPU核心数',
    `memory_total`     INT UNSIGNED          DEFAULT NULL COMMENT '总内存（MB）',
    `disk_total`       INT UNSIGNED          DEFAULT NULL COMMENT '总磁盘空间（MB）',
    `node_role`        TINYINT UNSIGNED      DEFAULT NULL COMMENT '节点角色：0-登录节点，1-计算节点，2-存储节点',
    `node_type`        TINYINT UNSIGNED      DEFAULT NULL COMMENT '节点类型（自定义）',
    `gpu_model`        VARCHAR(32)           DEFAULT NULL COMMENT 'GPU型号',
    `gpu_count`        INT UNSIGNED          DEFAULT NULL COMMENT 'GPU卡数',
    `gpu_memory_total`  INT UNSIGNED          DEFAULT NULL COMMENT 'GPU总显存（MB）',
    `cluster_id`       INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    `ipmi_ip`          VARCHAR(45)           DEFAULT NULL COMMENT 'IPMI专用IP地址',
    `ipmi_user`        VARCHAR(32)           DEFAULT NULL COMMENT 'IPMI用户名',
    `ipmi_password`    VARCHAR(255)          DEFAULT NULL COMMENT 'IPMI加密密码',
    `power_supported`  TINYINT(1) UNSIGNED   DEFAULT 0 COMMENT '是否支持功耗采集：0-否，1-是',
    `power_metric_name` VARCHAR(64)           DEFAULT NULL COMMENT 'Prometheus功耗指标名称',
    PRIMARY KEY (`node_id`),
    KEY `fk_node_cluster` (`cluster_id`),
    CONSTRAINT `fk_node_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点静态信息表（动态数据通过Prometheus获取）';

-- 4. 作业调度器数据表
DROP TABLE IF EXISTS `job_schedulers`;
CREATE TABLE `job_schedulers` (
    `scheduler_id`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '调度器ID，主键',
    `scheduler_name` VARCHAR(32)  NOT NULL COMMENT '作业调度器名称',
    `status`        TINYINT UNSIGNED      DEFAULT 0 COMMENT '状态：0-运行中，1-未启用，2-停止中',
    `port`          INT UNSIGNED          DEFAULT NULL COMMENT '连接端口',
    `auth_type`      VARCHAR(32)           DEFAULT NULL COMMENT '认证方式/密钥',
    `cluster_id`     INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    PRIMARY KEY (`scheduler_id`),
    KEY `fk_scheduler_cluster` (`cluster_id`),
    CONSTRAINT `fk_scheduler_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作业调度器信息表';

-- 5. 设备监控表
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices` (
    `device_id`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备ID，主键',
    `device_name` VARCHAR(32)  NOT NULL COMMENT '设备名称',
    `device_type` VARCHAR(32)  NOT NULL COMMENT '设备类型',
    `device_ip`   VARCHAR(32)           DEFAULT NULL COMMENT '设备IP',
    `status`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '设备状态：0-离线，1-在线',
    `last_update` DATETIME              DEFAULT NULL COMMENT '最近数据采集时间',
    `model`      VARCHAR(32)           DEFAULT NULL COMMENT '设备型号',
    `cluster_id`  INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    `monitor_url` VARCHAR(255)          DEFAULT NULL COMMENT '外部监控系统链接',
    PRIMARY KEY (`device_id`),
    KEY `fk_device_cluster` (`cluster_id`),
    CONSTRAINT `fk_device_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='外部设备监控表';

-- 6. 用户-会话表
DROP TABLE IF EXISTS `user_sessions`;
CREATE TABLE `user_sessions` (
    `session_id`  VARCHAR(64) NOT NULL COMMENT '会话ID，主键',
    `user_id`     INT UNSIGNED NOT NULL COMMENT '用户ID',
    `node_id`     INT UNSIGNED NOT NULL COMMENT '登录节点ID',
    `login_time`  DATETIME              DEFAULT NULL COMMENT '登录时间',
    `logout_time` DATETIME              DEFAULT NULL COMMENT '登出时间',
    `status`     TINYINT UNSIGNED      DEFAULT 0 COMMENT '会话状态：0-离线，1-在线',
    PRIMARY KEY (`session_id`),
    KEY `fk_session_user` (`user_id`),
    KEY `fk_session_node` (`node_id`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_session_node` FOREIGN KEY (`node_id`) REFERENCES `nodes` (`node_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户登录会话记录表';

-- 7. 作业监控数据表
DROP TABLE IF EXISTS `jobs`;
CREATE TABLE `jobs` (
    `job_id`          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '作业ID，主键',
    `job_name`        VARCHAR(32)  NOT NULL COMMENT '作业名称',
    `queue`          VARCHAR(32)  NOT NULL COMMENT '所属队列',
    `partition`      VARCHAR(32)           DEFAULT NULL COMMENT '队列名称（或分区）',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '作业状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    `description`    VARCHAR(255)          DEFAULT NULL COMMENT '作业描述',
    `scheduler_id`    INT UNSIGNED NOT NULL COMMENT '调度器ID',
    `external_job_id`  INT UNSIGNED NOT NULL COMMENT '调度器生成的作业ID',
    `cluster_id`      INT UNSIGNED NOT NULL COMMENT '运行集群ID',
    `user_id`         INT UNSIGNED NOT NULL COMMENT '提交用户ID',
    `submit_time`     DATETIME              DEFAULT NULL COMMENT '提交时间',
    `start_time`      DATETIME              DEFAULT NULL COMMENT '开始运行时间',
    `end_time`        DATETIME              DEFAULT NULL COMMENT '结束时间',
    `elapsed`        BIGINT UNSIGNED       DEFAULT NULL COMMENT '运行时长（秒）',
    `num_nodes`       INT UNSIGNED          DEFAULT NULL COMMENT '申请节点数',
    `work_dir`        VARCHAR(255)          DEFAULT NULL COMMENT '工作目录',
    `cpu_cores`       INT UNSIGNED          DEFAULT NULL COMMENT '申请CPU核数',
    `cpu_cores_hours`  DECIMAL(12,2) UNSIGNED DEFAULT NULL COMMENT 'CPU核时',
    `gpu_cores`       INT UNSIGNED          DEFAULT NULL COMMENT '申请GPU核数',
    `gpu_model`       VARCHAR(32)           DEFAULT NULL COMMENT '申请GPU型号',
    `gpu_cores_hours`  DECIMAL(12,2) UNSIGNED DEFAULT NULL COMMENT 'GPU核时',
    `app_name`        VARCHAR(32)           DEFAULT NULL COMMENT '应用软件名称',
    PRIMARY KEY (`job_id`),
    KEY `fk_job_scheduler` (`scheduler_id`),
    KEY `fk_job_cluster` (`cluster_id`),
    KEY `fk_job_user` (`user_id`),
    CONSTRAINT `fk_job_scheduler` FOREIGN KEY (`scheduler_id`) REFERENCES `job_schedulers` (`scheduler_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_job_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_job_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作业监控数据表';

-- 8. 告警信息表
DROP TABLE IF EXISTS `alerts`;
CREATE TABLE `alerts` (
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '告警ID，主键',
    `notice`     VARCHAR(255) NOT NULL COMMENT '告警消息内容',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '告警触发时间',
    `target`     VARCHAR(32)           DEFAULT NULL COMMENT '触发告警的节点名称',
    `level`      TINYINT UNSIGNED      DEFAULT 0 COMMENT '告警级别：0-1-2-3，四级',
    `status`     TINYINT UNSIGNED      DEFAULT 0 COMMENT '处理状态：0-未解决，1-已确认，2-已解决',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警信息表';

-- 9. 告警规则表
DROP TABLE IF EXISTS `alert_rules`;
CREATE TABLE `alert_rules` (
    `rule_id`        INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规则ID，主键',
    `rule_name`      VARCHAR(64)  NOT NULL COMMENT '规则名称，如GPU温度过高',
    `rule_type`      VARCHAR(64)  NOT NULL COMMENT '告警类型：threshold（阈值告警）/ status（状态变化）',
    `target_type`    VARCHAR(16)  NOT NULL COMMENT '目标类型：node / device / job',
    `metric_name`    VARCHAR(64)           DEFAULT NULL COMMENT '监控指标名（阈值告警时使用）',
    `condition`     VARCHAR(32)           DEFAULT NULL COMMENT '条件表达式，如 >、<、>=、<=',
    `threshold`     DECIMAL(10,2) UNSIGNED DEFAULT NULL COMMENT '阈值，如 85、5000',
    `duration`      INT UNSIGNED          DEFAULT NULL COMMENT '持续时间（秒），如持续60秒才触发',
    `level`         TINYINT UNSIGNED NOT NULL COMMENT '告警级别 0/1/2/3',
    `enabled`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0:禁用 1:启用',
    `notify_methods` VARCHAR(64)           DEFAULT NULL COMMENT '通知方式（JSON或逗号分隔），如站内消息、邮件',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '规则描述',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `scope_type`    VARCHAR(16)           DEFAULT NULL COMMENT '作用范围：cluster / node / global',
    `scope_id`      INT UNSIGNED          DEFAULT NULL COMMENT '对应集群/节点的ID（当scope_type为cluster或node时使用）',
    PRIMARY KEY (`rule_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警规则表';

-- 10. 文件管理表
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files` (
    `file_id`     INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID，主键',
    `file_name`   VARCHAR(255)          DEFAULT NULL COMMENT '文件名',
    `file_path`   VARCHAR(255)          DEFAULT NULL COMMENT '文件路径',
    `file_size`   BIGINT UNSIGNED       DEFAULT NULL COMMENT '文件大小（字节）',
    `user_id`     INT UNSIGNED NOT NULL COMMENT '文件所有者ID',
    `cluster_id`  INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    `upload_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `file_hash`   VARCHAR(64)           DEFAULT NULL COMMENT 'MD5，用于校验文件完整性',
    PRIMARY KEY (`file_id`),
    KEY `fk_file_user` (`user_id`),
    KEY `fk_file_cluster` (`cluster_id`),
    CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_file_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户文件管理表';

-- 11. Refresh Token表
DROP TABLE IF EXISTS `refresh_tokens`;
CREATE TABLE `refresh_tokens` (
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID，主键',
    `token`      VARCHAR(512) NOT NULL COMMENT 'Refresh Token',
    `user_id`     INT UNSIGNED NOT NULL COMMENT '用户ID',
    `expires_at`  DATETIME NOT NULL COMMENT '过期时间',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `fk_refresh_user` (`user_id`),
    CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Refresh Token存储表';

SET FOREIGN_KEY_CHECKS = 1;

SELECT * FROM users;
INSERT INTO users (username, password, user_role, status, department) VALUES ('admin', MD5('admin123'), '0', 'active', '系统管理员');