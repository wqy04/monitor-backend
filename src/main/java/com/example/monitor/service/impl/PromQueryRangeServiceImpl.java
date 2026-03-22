package com.example.monitor.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Throwables;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.example.monitor.entity.prometheus.*;
import com.example.monitor.service.PromQueryRangeService;
import com.example.monitor.utils.RestTemplateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 指标查询接口实现类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Slf4j
@Service("promQueryRangeService")
public class PromQueryRangeServiceImpl implements PromQueryRangeService {
    @Resource
    private RestTemplateUtils restTemplateUtils;
    
    @Override
    public PromQueryRangeData getQueryRangeDataInfo(PromQueryRange queryRange) {
        JSONObject param = new JSONObject();
        handleQueryRangeParams(param, queryRange);

        // prometheus的URL连接地址, 根据需要修改
        String url = "http://localhost:9090" + "/api/v1/query_range";
        return (PromQueryRangeData) getDataInfo(url, param);
    }
    
    /**
     * 处理范围查询参数
     *
     * @param param      参数
     * @param queryRange PromQueryRange对象
     */
    private void handleQueryRangeParams(JSONObject param, PromQueryRange queryRange) {
        String start = queryRange.getStart();
        if (StringUtils.isBlank(start)) {
            // 开始时间为空, 则设置默认值为当前时间
            start = String.valueOf(DateUtil.currentSeconds());
        }

        String end = queryRange.getEnd();
        if (StringUtils.isBlank(end)) {
            // 结束时间为空, 则设置默认值为当前时间向后偏移1小时
            end = String.valueOf(DateUtil.offsetHour(DateUtil.parse(start), 1).getTime());
        }

        param.put("query", queryRange.getQuery());
        param.put("start", start);
        param.put("end", end);
        param.put("step", queryRange.getStep());
    }
    
    /**
     * 获取查询结果数据
     *
     * @param promUrl   调用的prometheus的URL
     * @param param     请求参数
     * @return 查询结果对象
     */
    private Object getDataInfo(String promUrl, JSONObject param) {
        String http = getHttp(promUrl, param);
        PromQueryRangeResponse rangeResponse = JSON.parseObject(http, PromQueryRangeResponse.class);
        log.info("范围区间查询请求地址: {}, 请求参数: {}", promUrl, param);
        if (Objects.isNull(rangeResponse)) {
            return null;
        }

        String status = rangeResponse.getStatus();
        if (StringUtils.isBlank(status) || !StringUtils.equals("success", status)) {
            return null;
        }
        return rangeResponse.getData();
    }
    
    /**
     * 获取http连接
     *
     * @param promUrl 连接URL
     * @param param   请求参数
     * @return http连接
     */
    private String getHttp(String promUrl, JSONObject param) {
        String http = null;
        try {
            http = restTemplateUtils.getHttp(promUrl, param);
        } catch (Exception e) {
            log.error("请求地址: {}, 请求参数: {}, 异常信息: {}", promUrl, param, Throwables.getStackTraceAsString(e));
        }
        return http;
    }
}