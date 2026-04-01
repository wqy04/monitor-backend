-- Active: 1755161368222@@127.0.0.1@3306@monitor_db
-- ======================================================
-- 数据库: 集群管理平台
-- 说明: 根据设计方案生成的建表脚本
-- 版本: MySQL 5.7+
-- ======================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 集群表
DROP TABLE IF EXISTS `clusters`;
CREATE TABLE `clusters` (
    `clusterId`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '集群ID，主键',
    `clusterName` VARCHAR(32)  NOT NULL COMMENT '集群名称',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '集群描述',
    PRIMARY KEY (`clusterId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='集群信息表';

-- 2. 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `userId`     INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
    `username`   VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `userRole`   VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT '用户角色：0-系统管理员，1-普通用户',
    `status`     VARCHAR(32)  NOT NULL DEFAULT 'active' COMMENT '账号状态：active-激活，inactive-未激活',
    `lastLogin`  DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `createTime` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
    `department` VARCHAR(32)           DEFAULT NULL COMMENT '所属学院',
    PRIMARY KEY (`userId`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户信息表';

-- 3. 节点监控数据表（静态数据）
DROP TABLE IF EXISTS `nodes`;
CREATE TABLE `nodes` (
    `nodeId`          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '节点ID，主键',
    `nodeName`        VARCHAR(32)  NOT NULL COMMENT '节点名',
    `nodeIp`          VARCHAR(32)  NOT NULL COMMENT '节点IP',
    `partition`       VARCHAR(32)           DEFAULT NULL COMMENT '所属队列',
    `cpuTotal`        INT UNSIGNED          DEFAULT NULL COMMENT '总CPU核心数',
    `memoryTotal`     INT UNSIGNED          DEFAULT NULL COMMENT '总内存（MB）',
    `diskTotal`       INT UNSIGNED          DEFAULT NULL COMMENT '总磁盘空间（MB）',
    `nodeRole`        TINYINT UNSIGNED      DEFAULT NULL COMMENT '节点角色：0-登录节点，1-计算节点，2-存储节点',
    `nodeType`        TINYINT UNSIGNED      DEFAULT NULL COMMENT '节点类型（自定义）',
    `gpuModel`        VARCHAR(32)           DEFAULT NULL COMMENT 'GPU型号',
    `gpuCount`        INT UNSIGNED          DEFAULT NULL COMMENT 'GPU卡数',
    `gpuMemoryTotal`  INT UNSIGNED          DEFAULT NULL COMMENT 'GPU总显存（MB）',
    `clusterId`       INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    `ipmiIP`          VARCHAR(45)           DEFAULT NULL COMMENT 'IPMI专用IP地址',
    `ipmiUser`        VARCHAR(32)           DEFAULT NULL COMMENT 'IPMI用户名',
    `ipmiPassword`    VARCHAR(255)          DEFAULT NULL COMMENT 'IPMI加密密码',
    `powerSupported`  TINYINT(1) UNSIGNED   DEFAULT 0 COMMENT '是否支持功耗采集：0-否，1-是',
    `powerMetricName` VARCHAR(64)           DEFAULT NULL COMMENT 'Prometheus功耗指标名称',
    PRIMARY KEY (`nodeId`),
    KEY `fk_node_cluster` (`clusterId`),
    CONSTRAINT `fk_node_cluster` FOREIGN KEY (`clusterId`) REFERENCES `clusters` (`clusterId`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点静态信息表（动态数据通过Prometheus获取）';

-- 4. 作业调度器数据表
DROP TABLE IF EXISTS `job_schedulers`;
CREATE TABLE `job_schedulers` (
    `schedulerId`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '调度器ID，主键',
    `schedulerName` VARCHAR(32)  NOT NULL COMMENT '作业调度器名称',
    `status`        TINYINT UNSIGNED      DEFAULT 0 COMMENT '状态：0-运行中，1-未启用，2-停止中',
    `port`          INT UNSIGNED          DEFAULT NULL COMMENT '连接端口',
    `authType`      VARCHAR(32)           DEFAULT NULL COMMENT '认证方式/密钥',
    `clusterId`     INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    PRIMARY KEY (`schedulerId`),
    KEY `fk_scheduler_cluster` (`clusterId`),
    CONSTRAINT `fk_scheduler_cluster` FOREIGN KEY (`clusterId`) REFERENCES `clusters` (`clusterId`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作业调度器信息表';

-- 5. 设备监控表
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices` (
    `deviceId`   INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备ID，主键',
    `deviceName` VARCHAR(32)  NOT NULL COMMENT '设备名称',
    `deviceType` VARCHAR(32)  NOT NULL COMMENT '设备类型',
    `deviceIp`   VARCHAR(32)           DEFAULT NULL COMMENT '设备IP',
    `status`     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '设备状态：0-离线，1-在线',
    `lastUpdate` DATETIME              DEFAULT NULL COMMENT '最近数据采集时间',
    `model`      VARCHAR(32)           DEFAULT NULL COMMENT '设备型号',
    `clusterId`  INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    `monitorUrl` VARCHAR(255)          DEFAULT NULL COMMENT '外部监控系统链接',
    PRIMARY KEY (`deviceId`),
    KEY `fk_device_cluster` (`clusterId`),
    CONSTRAINT `fk_device_cluster` FOREIGN KEY (`clusterId`) REFERENCES `clusters` (`clusterId`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='外部设备监控表';

-- 6. 用户-会话表
DROP TABLE IF EXISTS `user_sessions`;
CREATE TABLE `user_sessions` (
    `sessionId`  VARCHAR(64) NOT NULL COMMENT '会话ID，主键',
    `userId`     INT UNSIGNED NOT NULL COMMENT '用户ID',
    `nodeId`     INT UNSIGNED NOT NULL COMMENT '登录节点ID',
    `loginTime`  DATETIME              DEFAULT NULL COMMENT '登录时间',
    `logoutTime` DATETIME              DEFAULT NULL COMMENT '登出时间',
    `status`     TINYINT UNSIGNED      DEFAULT 0 COMMENT '会话状态：0-离线，1-在线',
    PRIMARY KEY (`sessionId`),
    KEY `fk_session_user` (`userId`),
    KEY `fk_session_node` (`nodeId`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_session_node` FOREIGN KEY (`nodeId`) REFERENCES `nodes` (`nodeId`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户登录会话记录表';

-- 7. 作业监控数据表
DROP TABLE IF EXISTS `jobs`;
CREATE TABLE `jobs` (
    `jobId`          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '作业ID，主键',
    `jobName`        VARCHAR(32)  NOT NULL COMMENT '作业名称',
    `queue`          VARCHAR(32)  NOT NULL COMMENT '所属队列',
    `partition`      VARCHAR(32)           DEFAULT NULL COMMENT '队列名称（或分区）',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '作业状态：PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    `description`    VARCHAR(255)          DEFAULT NULL COMMENT '作业描述',
    `schedulerId`    INT UNSIGNED NOT NULL COMMENT '调度器ID',
    `externalJobId`  INT UNSIGNED NOT NULL COMMENT '调度器生成的作业ID',
    `clusterId`      INT UNSIGNED NOT NULL COMMENT '运行集群ID',
    `userId`         INT UNSIGNED NOT NULL COMMENT '提交用户ID',
    `submitTime`     DATETIME              DEFAULT NULL COMMENT '提交时间',
    `startTime`      DATETIME              DEFAULT NULL COMMENT '开始运行时间',
    `endTime`        DATETIME              DEFAULT NULL COMMENT '结束时间',
    `elapsed`        BIGINT UNSIGNED       DEFAULT NULL COMMENT '运行时长（秒）',
    `numNodes`       INT UNSIGNED          DEFAULT NULL COMMENT '申请节点数',
    `workDir`        VARCHAR(255)          DEFAULT NULL COMMENT '工作目录',
    `cpuCores`       INT UNSIGNED          DEFAULT NULL COMMENT '申请CPU核数',
    `cpuCoresHours`  DECIMAL(12,2) UNSIGNED DEFAULT NULL COMMENT 'CPU核时',
    `gpuCores`       INT UNSIGNED          DEFAULT NULL COMMENT '申请GPU核数',
    `gpuModel`       VARCHAR(32)           DEFAULT NULL COMMENT '申请GPU型号',
    `gpuCoresHours`  DECIMAL(12,2) UNSIGNED DEFAULT NULL COMMENT 'GPU核时',
    `appName`        VARCHAR(32)           DEFAULT NULL COMMENT '应用软件名称',
    PRIMARY KEY (`jobId`),
    KEY `fk_job_scheduler` (`schedulerId`),
    KEY `fk_job_cluster` (`clusterId`),
    KEY `fk_job_user` (`userId`),
    CONSTRAINT `fk_job_scheduler` FOREIGN KEY (`schedulerId`) REFERENCES `job_schedulers` (`schedulerId`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_job_cluster` FOREIGN KEY (`clusterId`) REFERENCES `clusters` (`clusterId`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_job_user` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作业监控数据表';

-- 8. 告警信息表
DROP TABLE IF EXISTS `alerts`;
CREATE TABLE `alerts` (
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '告警ID，主键',
    `notice`     VARCHAR(255) NOT NULL COMMENT '告警消息内容',
    `updateTime` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '告警触发时间',
    `target`     VARCHAR(32)           DEFAULT NULL COMMENT '触发告警的节点名称',
    `level`      TINYINT UNSIGNED      DEFAULT 0 COMMENT '告警级别：0-1-2-3，四级',
    `status`     TINYINT UNSIGNED      DEFAULT 0 COMMENT '处理状态：0-未解决，1-已确认，2-已解决',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警信息表';

-- 9. 告警规则表
DROP TABLE IF EXISTS `alert_rules`;
CREATE TABLE `alert_rules` (
    `ruleId`        INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规则ID，主键',
    `ruleName`      VARCHAR(64)  NOT NULL COMMENT '规则名称，如GPU温度过高',
    `ruleType`      VARCHAR(64)  NOT NULL COMMENT '告警类型：threshold（阈值告警）/ status（状态变化）',
    `targetType`    VARCHAR(16)  NOT NULL COMMENT '目标类型：node / device / job',
    `metricName`    VARCHAR(64)           DEFAULT NULL COMMENT '监控指标名（阈值告警时使用）',
    `condition`     VARCHAR(32)           DEFAULT NULL COMMENT '条件表达式，如 >、<、>=、<=',
    `threshold`     DECIMAL(10,2) UNSIGNED DEFAULT NULL COMMENT '阈值，如 85、5000',
    `duration`      INT UNSIGNED          DEFAULT NULL COMMENT '持续时间（秒），如持续60秒才触发',
    `level`         TINYINT UNSIGNED NOT NULL COMMENT '告警级别 0/1/2/3',
    `enabled`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0:禁用 1:启用',
    `notifyMethods` VARCHAR(64)           DEFAULT NULL COMMENT '通知方式（JSON或逗号分隔），如站内消息、邮件',
    `description`   VARCHAR(255)          DEFAULT NULL COMMENT '规则描述',
    `createTime`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `scope_type`    VARCHAR(16)           DEFAULT NULL COMMENT '作用范围：cluster / node / global',
    `scope_id`      INT UNSIGNED          DEFAULT NULL COMMENT '对应集群/节点的ID（当scope_type为cluster或node时使用）',
    PRIMARY KEY (`ruleId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='告警规则表';

-- 10. 文件管理表
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files` (
    `fileId`     INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件ID，主键',
    `fileName`   VARCHAR(255)          DEFAULT NULL COMMENT '文件名',
    `filePath`   VARCHAR(255)          DEFAULT NULL COMMENT '文件路径',
    `fileSize`   BIGINT UNSIGNED       DEFAULT NULL COMMENT '文件大小（字节）',
    `userId`     INT UNSIGNED NOT NULL COMMENT '文件所有者ID',
    `clusterId`  INT UNSIGNED NOT NULL COMMENT '所属集群ID',
    `uploadTime` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `fileHash`   VARCHAR(64)           DEFAULT NULL COMMENT 'MD5，用于校验文件完整性',
    PRIMARY KEY (`fileId`),
    KEY `fk_file_user` (`userId`),
    KEY `fk_file_cluster` (`clusterId`),
    CONSTRAINT `fk_file_user` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_file_cluster` FOREIGN KEY (`clusterId`) REFERENCES `clusters` (`clusterId`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户文件管理表';

SET FOREIGN_KEY_CHECKS = 1;