package com.example.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件管理表实体
 * 对应数据库表：file_manage
 */
@Data
@TableName("file_manage")
public class FileManage {
    @TableId(type = IdType.AUTO)
    private Long fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件所有者ID
     */
    private Long userId;

    /**
     * 所属集群ID
     */
    private Long clusterId;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}