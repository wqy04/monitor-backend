package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    // 可扩展自定义业务方法，例如：根据用户名查询用户
    User getUserByUsername(String username);

    /**
     * 根据ID查询用户
     */
    User getById(Long id);
}