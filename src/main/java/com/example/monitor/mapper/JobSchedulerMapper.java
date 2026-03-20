package com.example.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.monitor.entity.JobScheduler;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobSchedulerMapper extends BaseMapper<JobScheduler> {
}