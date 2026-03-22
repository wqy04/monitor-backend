package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.FileManage;
import com.example.monitor.mapper.FileManageMapper;
import com.example.monitor.service.FileManageService;
import org.springframework.stereotype.Service;

/**
 * 文件管理服务实现类
 */
@Service
public class FileManageServiceImpl extends ServiceImpl<FileManageMapper, FileManage> implements FileManageService {

    @Override
    public FileManage getFileManageById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<FileManage>()
                .eq(FileManage::getFileId, id));
    }
}