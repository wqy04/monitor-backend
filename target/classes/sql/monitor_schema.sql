-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `userId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `userRole` varchar(32) NOT NULL COMMENT '0:系统管理员 1：普通用户',
  `status` varchar(32) NOT NULL COMMENT 'active:激活 inactive:未激活',
  `lastLogin` datetime DEFAULT NULL COMMENT '最后登录时间',
  `createTime` datetime DEFAULT NULL COMMENT '账号创建时间',
  `department` varchar(32) DEFAULT NULL COMMENT '用户所属学院',
  PRIMARY KEY (`userId`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 用户-会话表
-- ----------------------------
DROP TABLE IF EXISTS `user_session`;
CREATE TABLE `user_session` (
  `sessionId` varchar(64) NOT NULL COMMENT '会话ID，唯一主键',
  `userId` bigint NOT NULL COMMENT '关联用户ID',
  `nodeId` bigint NOT NULL COMMENT '关联节点ID',
  `loginTime` datetime DEFAULT NULL COMMENT '登入时间',
  `logoutTime` datetime DEFAULT NULL COMMENT '登出时间',
  `status` int DEFAULT NULL COMMENT '0:离线，1：在线',
  PRIMARY KEY (`sessionId`),
  KEY `fk_us_user` (`userId`),
  KEY `fk_us_node` (`nodeId`),
  CONSTRAINT `fk_us_user` FOREIGN KEY (`userId`) REFERENCES `user` (`userId`),
  CONSTRAINT `fk_us_node` FOREIGN KEY (`nodeId`) REFERENCES `node_monitor` (`nodeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-会话表';

-- ----------------------------
-- 集群表
-- ----------------------------
DROP TABLE IF EXISTS `cluster`;
CREATE TABLE `cluster` (
  `clusterId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `schedulerId` bigint DEFAULT NULL COMMENT '调度器ID',
  `clusterName` varchar(32) NOT NULL COMMENT '集群名称',
  `description` varchar(255) DEFAULT NULL COMMENT '集群描述',
  PRIMARY KEY (`clusterId`),
  KEY `fk_c_scheduler` (`schedulerId`),
  CONSTRAINT `fk_c_scheduler` FOREIGN KEY (`schedulerId`) REFERENCES `job_scheduler` (`schedulerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集群表';

-- ----------------------------
-- 作业调度器数据表
-- ----------------------------
DROP TABLE IF EXISTS `job_scheduler`;
CREATE TABLE `job_scheduler` (
  `schedulerId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `schedulerName` varchar(32) NOT NULL COMMENT '作业调度器的名称',
  `status` int DEFAULT NULL COMMENT '0:运行中1:未启用2:停止中',
  `port` int DEFAULT NULL COMMENT '连接端口',
  `authType` varchar(32) DEFAULT NULL COMMENT '连接密钥',
  `clusterId` bigint DEFAULT NULL COMMENT '属于的集群ID',
  PRIMARY KEY (`schedulerId`),
  KEY `fk_js_cluster` (`clusterId`),
  CONSTRAINT `fk_js_cluster` FOREIGN KEY (`clusterId`) REFERENCES `cluster` (`clusterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业调度器数据表';

-- ----------------------------
-- 节点监控数据表（静态）
-- ----------------------------
DROP TABLE IF EXISTS `node_monitor`;
CREATE TABLE `node_monitor` (
  `nodeId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `nodeName` varchar(32) NOT NULL COMMENT '节点名',
  `nodeIp` varchar(32) NOT NULL COMMENT '节点IP',
  `partition` varchar(32) DEFAULT NULL COMMENT '所属队列',
  `status` int NOT NULL COMMENT '0:离线 1:在线',
  `cpuTotal` int DEFAULT NULL COMMENT '总CPU核心数',
  `memoryTotal` bigint DEFAULT NULL COMMENT '总内存（MB）',
  `diskTotal` bigint DEFAULT NULL COMMENT '总磁盘空间（MB）',
  `lastUpdate` datetime DEFAULT NULL COMMENT '最后更新时间',
  `nodeRole` int DEFAULT NULL COMMENT '0:登录节点1:计算节点2:存储节点',
  `nodeType` int DEFAULT NULL COMMENT '节点类型',
  `gpuModel` varchar(32) DEFAULT NULL COMMENT 'GPU型号',
  `gpuCount` int DEFAULT NULL COMMENT 'GPU卡数',
  `gpuMemoryTotal` bigint DEFAULT NULL COMMENT 'GPU总显存',
  `clusterId` bigint DEFAULT NULL COMMENT '所属集群ID',
  `ipmiIP` varchar(45) DEFAULT NULL COMMENT 'IPMI专用IP',
  `ipmiUser` varchar(32) DEFAULT NULL COMMENT 'IPMI用户名',
  `ipmiPassword` varchar(255) DEFAULT NULL COMMENT 'IPMI加密密码',
  `powerSupported` tinyint(1) DEFAULT NULL COMMENT '是否允许功耗采集',
  `powerMetricName` varchar(64) DEFAULT NULL COMMENT 'Prometheus metric名称',
  PRIMARY KEY (`nodeId`),
  KEY `fk_nm_cluster` (`clusterId`),
  CONSTRAINT `fk_nm_cluster` FOREIGN KEY (`clusterId`) REFERENCES `cluster` (`clusterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点监控数据表';

-- ----------------------------
-- 作业监控数据表
-- ----------------------------
DROP TABLE IF EXISTS `job_monitor`;
CREATE TABLE `job_monitor` (
  `JobId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `jobName` varchar(32) NOT NULL COMMENT '作业名称',
  `queue` varchar(32) NOT NULL COMMENT '所属队列',
  `partition` varchar(32) DEFAULT NULL COMMENT '队列名称',
  `status` varchar(32) NOT NULL COMMENT 'PENDING:排队中 RUNNING:运行中 COMPLETED:已完成 FAILED:失败 CANCELLED：取消',
  `description` varchar(255) DEFAULT NULL COMMENT '作业描述',
  `schedulerId` bigint NOT NULL COMMENT '调度器ID',
  `externalJobId` bigint NOT NULL COMMENT '调度器生成的作业ID',
  `clusterId` bigint NOT NULL COMMENT '运行集群ID',
  `userId` bigint DEFAULT NULL COMMENT '关联用户ID',
  `submitTime` datetime DEFAULT NULL COMMENT '提交时间',
  `startTime` datetime DEFAULT NULL COMMENT '开始运行时间',
  `endTime` datetime DEFAULT NULL COMMENT '结束时间',
  `elapsed` bigint DEFAULT NULL COMMENT '运行时间（秒）',
  `numNodes` int DEFAULT NULL COMMENT '申请节点数',
  `workDir` varchar(255) DEFAULT NULL COMMENT '工作目录',
  `cpuCores` int DEFAULT NULL COMMENT '申请CPU核数',
  `cpuCoresHours` int DEFAULT NULL COMMENT 'CPU核时',
  `gpuCores` int DEFAULT NULL COMMENT '申请GPU核数',
  `gpuModel` varchar(32) DEFAULT NULL COMMENT 'GPU型号',
  `gpuCoresHours` int DEFAULT NULL COMMENT 'GPU核时',
  `appName` varchar(32) DEFAULT NULL COMMENT '应用软件名称',
  PRIMARY KEY (`JobId`),
  KEY `fk_jm_scheduler` (`schedulerId`),
  KEY `fk_jm_cluster` (`clusterId`),
  KEY `fk_jm_user` (`userId`),
  CONSTRAINT `fk_jm_scheduler` FOREIGN KEY (`schedulerId`) REFERENCES `job_scheduler` (`schedulerId`),
  CONSTRAINT `fk_jm_cluster` FOREIGN KEY (`clusterId`) REFERENCES `cluster` (`clusterId`),
  CONSTRAINT `fk_jm_user` FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业监控数据表';

-- ----------------------------
-- 设备监控表
-- ----------------------------
DROP TABLE IF EXISTS `device_monitor`;
CREATE TABLE `device_monitor` (
  `deviceId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `deviceName` varchar(32) NOT NULL COMMENT '设备名称',
  `deviceType` varchar(32) NOT NULL COMMENT '设备类型',
  `deviceIp` varchar(32) DEFAULT NULL COMMENT '设备IP',
  `status` int NOT NULL COMMENT '0:离线 1:在线',
  `lastUpdate` datetime DEFAULT NULL COMMENT '最近采集时间',
  `model` varchar(32) DEFAULT NULL COMMENT '设备型号',
  `clusterId` bigint DEFAULT NULL COMMENT '所属集群ID',
  `monitorUrl` varchar(255) DEFAULT NULL COMMENT '外部监控链接',
  PRIMARY KEY (`deviceId`),
  KEY `fk_dm_cluster` (`clusterId`),
  CONSTRAINT `fk_dm_cluster` FOREIGN KEY (`clusterId`) REFERENCES `cluster` (`clusterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备监控表';

-- ----------------------------
-- 告警信息表
-- ----------------------------
DROP TABLE IF EXISTS `alarm_info`;
CREATE TABLE `alarm_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `notice` varchar(255) NOT NULL COMMENT '告警消息内容',
  `updateTime` datetime DEFAULT NULL COMMENT '告警触发时间',
  `target` varchar(32) DEFAULT NULL COMMENT '触发告警的节点名称',
  `level` int DEFAULT NULL COMMENT '0/1/2/3四级告警',
  `status` int DEFAULT NULL COMMENT '0:未解决1:已确认2:已解决',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警信息表';

-- ----------------------------
-- 文件管理表
-- ----------------------------
DROP TABLE IF EXISTS `file_manage`;
CREATE TABLE `file_manage` (
  `fileId` bigint NOT NULL AUTO_INCREMENT COMMENT '唯一主键，自增',
  `fileName` varchar(255) DEFAULT NULL COMMENT '文件名',
  `filePath` varchar(255) DEFAULT NULL COMMENT '文件路径',
  `fileSize` bigint DEFAULT NULL COMMENT '文件大小',
  `userId` bigint DEFAULT NULL COMMENT '文件所有者ID',
  `clusterId` bigint DEFAULT NULL COMMENT '所属集群ID',
  `uploadTime` datetime DEFAULT NULL COMMENT '文件上传时间',
  PRIMARY KEY (`fileId`),
  KEY `fk_fm_user` (`userId`),
  KEY `fk_fm_cluster` (`clusterId`),
  CONSTRAINT `fk_fm_user` FOREIGN KEY (`userId`) REFERENCES `user` (`userId`),
  CONSTRAINT `fk_fm_cluster` FOREIGN KEY (`clusterId`) REFERENCES `cluster` (`clusterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理表';