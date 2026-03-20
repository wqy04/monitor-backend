package com.example.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.monitor.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表Mapper
 * 继承BaseMapper，获得所有CRUD方法
 */
@Mapper // 可选，启动类加了@MapperScan可省略
public interface UserMapper extends BaseMapper<User> {
    // 无需要写任何代码，BaseMapper已实现：insert/delete/update/selectById/selectList等
}