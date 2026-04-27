package com.example.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.monitor.entity.AlarmInfo;

/**
 * 告警信息服务接口
 */
public interface AlarmInfoService extends IService<AlarmInfo> {
    // 可扩展自定义业务方法，例如：根据告警ID查询告警信息
    AlarmInfo getAlarmInfoById(Integer id);

    AlarmInfo findByNoticeAndTarget(String notice, String target);

    /**
     * 根据告警ID删除告警信息
     */
    boolean removeById(Integer id);
}