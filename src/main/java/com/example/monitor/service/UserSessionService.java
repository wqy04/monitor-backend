package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.UserSession;

/**
 * 用户会话服务接口
 */
public interface UserSessionService extends IService<UserSession> {
    // 可扩展自定义业务方法，例如：根据会话ID查询用户会话信息
    UserSession getUserSessionById(String id);
}