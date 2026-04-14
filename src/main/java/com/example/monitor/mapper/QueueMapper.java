package com.example.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.monitor.entity.Queue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 队列Mapper
 */
@Mapper
public interface QueueMapper extends BaseMapper<Queue> {
}