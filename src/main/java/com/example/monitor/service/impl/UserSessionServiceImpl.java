package com.example.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.monitor.entity.UserSession;
import com.example.monitor.mapper.UserSessionMapper;
import com.example.monitor.service.UserSessionService;
import org.springframework.stereotype.Service;

/**
 * 用户会话服务实现类
 */
@Service
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements UserSessionService {

    @Override
    public UserSession getUserSessionById(Long id) {
        return baseMapper.selectOne(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getId, id));
    }
}