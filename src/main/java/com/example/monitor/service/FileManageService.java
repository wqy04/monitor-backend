package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.FileManage;

/**
 * 文件管理服务接口
 */
public interface FileManageService extends IService<FileManage> {
    // 可扩展自定义业务方法，例如：根据文件ID查询文件信息
    FileManage getFileManageById(Long id);
}