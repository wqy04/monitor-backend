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
    `cluster_id`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '集群ID',
    `cluster_name` VARCHAR(64)  NOT NULL COMMENT '集群名称',
    `vendor`       VARCHAR(32)  DEFAULT NULL COMMENT '厂商：曙光/浪潮/联想等',
    `description`  VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`cluster_id`),
    UNIQUE KEY `uk_cluster_name` (`cluster_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='集群信息表';

-- 2. 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `user_id`     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(32)  NOT NULL,
    `password`    VARCHAR(255) NOT NULL,
    `user_role`   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-管理员，1-普通用户',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'active',
    `last_login`  DATETIME     DEFAULT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `department`  VARCHAR(64)  DEFAULT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户信息表';

-- 3. 节点监控数据表（静态数据）
DROP TABLE IF EXISTS `nodes`;
CREATE TABLE `nodes` (
    `node_id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `node_name`        VARCHAR(64)  NOT NULL COMMENT '节点主机名',
    `node_ip`          VARCHAR(45)  NOT NULL,
    `partition`        VARCHAR(64)  DEFAULT NULL COMMENT '所属分区/队列',
    `cpu_total`        INT UNSIGNED DEFAULT NULL,
    `memory_total`     BIGINT UNSIGNED DEFAULT NULL COMMENT '总内存(MB)',
    `disk_total`       BIGINT UNSIGNED DEFAULT NULL COMMENT '总磁盘(MB)',
    `node_role`        TINYINT UNSIGNED DEFAULT NULL COMMENT '0-登录，1-计算，2-存储',
    `node_type`        VARCHAR(32)  DEFAULT NULL COMMENT '自定义类型',
    `gpu_model`        VARCHAR(64)  DEFAULT NULL,
    `gpu_count`        INT UNSIGNED DEFAULT NULL,
    `gpu_memory_total` BIGINT UNSIGNED DEFAULT NULL COMMENT '总显存(MB)',
    `cluster_id`       INT UNSIGNED NOT NULL,
    `ipmi_ip`          VARCHAR(45)  DEFAULT NULL,
    `ipmi_user`        VARCHAR(32)  DEFAULT NULL,
    `ipmi_password`    VARCHAR(255) DEFAULT NULL,
    `power_supported`  TINYINT(1) UNSIGNED DEFAULT 0,
    `power_metric_name` VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (`node_id`),
    UNIQUE KEY `uk_node_name_cluster` (`node_name`, `cluster_id`),
    KEY `fk_node_cluster` (`cluster_id`),
    CONSTRAINT `fk_node_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='节点静态信息表';
ALTER TABLE `nodes` ADD COLUMN `cpu_model` VARCHAR(64) DEFAULT NULL COMMENT 'CPU型号/架构';
ALTER TABLE `nodes` ADD COLUMN `os_type` VARCHAR(32) DEFAULT NULL COMMENT '操作系统类型';
ALTER TABLE `nodes` ADD COLUMN `slots_max` INT UNSIGNED DEFAULT NULL COMMENT '最大作业槽数（可能不等于CPU核数）';
ALTER TABLE `nodes` ADD COLUMN `node_model` VARCHAR(128) DEFAULT NULL COMMENT '整机型号（如有）';

CREATE TABLE `node_queues` (
    `node_id` INT UNSIGNED NOT NULL,
    `queue_name` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`node_id`, `queue_name`)
);

DROP TABLE IF EXISTS `apps`;
CREATE TABLE `apps` (
    `app_id`      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `app_name`    VARCHAR(64)  NOT NULL COMMENT '应用名称，如 fluent、vasp',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '应用描述',
    `cluster_id`  INT UNSIGNED DEFAULT NULL COMMENT '若应用与特定集群绑定，否则为全局',
    PRIMARY KEY (`app_id`),
    UNIQUE KEY `uk_app_name_cluster` (`app_name`, `cluster_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='应用静态信息表';

DROP TABLE IF EXISTS `cluster_users`;
CREATE TABLE `cluster_users` (
    `cluster_user_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(32)  NOT NULL COMMENT '集群用户名',
    `cluster_id`      INT UNSIGNED NOT NULL,
    PRIMARY KEY (`cluster_user_id`),
    UNIQUE KEY `uk_username_cluster` (`username`, `cluster_id`),
    CONSTRAINT `fk_clusteruser_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='集群作业系统用户表';

DROP TABLE IF EXISTS `gpus`;
CREATE TABLE `gpus` (
    `gpu_id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `node_id`         INT UNSIGNED NOT NULL,
    `gpu_index`       TINYINT UNSIGNED NOT NULL COMMENT '节点内GPU编号，0-based',
    `gpu_model`       VARCHAR(64)  NOT NULL,
    `memory_total`    BIGINT UNSIGNED NOT NULL COMMENT '总显存(MB)',
    `status`          VARCHAR(16)  DEFAULT 'avail' COMMENT 'avail/unavail/offline',
    PRIMARY KEY (`gpu_id`),
    UNIQUE KEY `uk_node_gpu_index` (`node_id`, `gpu_index`),
    CONSTRAINT `fk_gpu_node` FOREIGN KEY (`node_id`) REFERENCES `nodes` (`node_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='GPU静态信息表';

-- 4. 队列静态信息表（新增）
DROP TABLE IF EXISTS `queues`;
CREATE TABLE `queues` (
    `queue_id`     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `queue_name`   VARCHAR(64)  NOT NULL COMMENT '队列名称',
    `cluster_id`   INT UNSIGNED NOT NULL,
    `nice`         INT          DEFAULT 0 COMMENT '优先级调整值',
    `priority`     INT          DEFAULT 0 COMMENT '静态优先级',
    `max_slots`    INT UNSIGNED DEFAULT NULL COMMENT '最大作业槽数',
    `status`       VARCHAR(16)  DEFAULT 'MAX' COMMENT 'MAX/ACTIVE等',
    `description`  VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`queue_id`),
    UNIQUE KEY `uk_queue_cluster` (`queue_name`, `cluster_id`),
    KEY `fk_queue_cluster` (`cluster_id`),
    CONSTRAINT `fk_queue_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='队列静态配置表';

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
    `device_id`   INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `device_name` VARCHAR(64)  NOT NULL,
    `device_type` VARCHAR(32)  NOT NULL COMMENT 'UPS/AirConditioner',
    `device_ip`   VARCHAR(45)  DEFAULT NULL,
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `last_update` DATETIME     DEFAULT NULL,
    `model`       VARCHAR(64)  DEFAULT NULL,
    `cluster_id`  INT UNSIGNED NOT NULL,
    `monitor_url` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`device_id`),
    KEY `fk_device_cluster` (`cluster_id`),
    CONSTRAINT `fk_device_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='基础设施设备表';

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
    `job_id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `job_name`        VARCHAR(128) NOT NULL,
    `queue`           VARCHAR(64)  NOT NULL,
    `partition`       VARCHAR(64)  DEFAULT NULL,
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    `description`     TEXT         DEFAULT NULL,
    `scheduler_id`    INT UNSIGNED NOT NULL,
    `external_job_id` VARCHAR(64)  NOT NULL COMMENT '调度器原生作业ID',
    `cluster_id`      INT UNSIGNED NOT NULL,
    `user_id`         INT UNSIGNED NOT NULL,
    `submit_time`     DATETIME     DEFAULT NULL,
    `start_time`      DATETIME     DEFAULT NULL,
    `end_time`        DATETIME     DEFAULT NULL,
    `elapsed`         BIGINT UNSIGNED DEFAULT NULL,
    `num_nodes`       INT UNSIGNED DEFAULT NULL,
    `work_dir`        VARCHAR(255) DEFAULT NULL,
    `cpu_cores`       INT UNSIGNED DEFAULT NULL,
    `cpu_cores_hours` DECIMAL(12,2) UNSIGNED DEFAULT NULL,
    `gpu_cores`       INT UNSIGNED DEFAULT NULL,
    `gpu_model`       VARCHAR(64)  DEFAULT NULL,
    `gpu_cores_hours` DECIMAL(12,2) UNSIGNED DEFAULT NULL,
    `app_name`        VARCHAR(64)  DEFAULT NULL,
    PRIMARY KEY (`job_id`),
    KEY `fk_job_scheduler` (`scheduler_id`),
    KEY `fk_job_cluster` (`cluster_id`),
    KEY `fk_job_user` (`user_id`),
    CONSTRAINT `fk_job_scheduler` FOREIGN KEY (`scheduler_id`) REFERENCES `job_schedulers` (`scheduler_id`),
    CONSTRAINT `fk_job_cluster`   FOREIGN KEY (`cluster_id`)   REFERENCES `clusters` (`cluster_id`),
    CONSTRAINT `fk_job_user`      FOREIGN KEY (`user_id`)      REFERENCES `users` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='作业历史与统计表';

-- 8. 告警信息表
DROP TABLE IF EXISTS `alerts`;
CREATE TABLE `alerts` (
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '告警ID，主键',
    `notice`     VARCHAR(255) NOT NULL COMMENT '告警消息内容',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '告警触发时间',
    `target`     VARCHAR(32)           DEFAULT NULL COMMENT '触发告警的节点名称',
    `level`      TINYINT UNSIGNED      DEFAULT 0 COMMENT '告警级别：0-1-2-3，四级',
    `status`     TINYINT UNSIGNED      DEFAULT 0 COMMENT '处理状态：0-未解决，1-已确认，2-已解决',
    `cluster_id`      INT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_alert_cluster` (`cluster_id`),
    CONSTRAINT `fk_alert_cluster` FOREIGN KEY (`cluster_id`) REFERENCES `clusters` (`cluster_id`)
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
SELECT * FROM job_schedulers;
SELECT * FROM clusters;
/* DELETE FROM clusters WHERE cluster_name = 'lenovo'; */
SELECT * FROM nodes;
SELECT * FROM node_queues;
SELECT * FROM queues;
SELECT * FROM gpus;
SELECT * FROM apps;
SELECT * FROM cluster_users;
SELECT * FROM jobs;
SELECT * FROM alerts;
SELECT * FROM alert_rules;
SELECT * FROM devices;
SELECT * FROM user_sessions;
SELECT * FROM refresh_tokens;