package com.example.monitor.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Throwables;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResponse;
import com.example.monitor.service.PromQueryService;
import com.example.monitor.utils.RestTemplateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 指标查询接口实现类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Slf4j
@Service("promQueryService")
public class PromQueryServiceImpl implements PromQueryService {
    @Resource
    private RestTemplateUtils restTemplateUtils;

    @Value("${prometheus.url:http://prometheus-server:9090}")
    private String prometheusUrl;
    
    @Override
    public PromQueryData getQueryDataInfo(String query, String time) {
        if (StringUtils.isBlank(time)) {
            time = String.valueOf(DateUtil.currentSeconds());
        }

        JSONObject param = new JSONObject();
        param.put("query", query);
        param.put("time", time);
    
        // prometheus的URL连接地址, 从配置获取
        String url = prometheusUrl + "/api/v1/query";
        return (PromQueryData) getDataInfo(url, param);
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
        PromQueryResponse responseInfo = JSON.parseObject(http, PromQueryResponse.class);
        log.info("即时查询请求地址: {}, 请求参数: {}", promUrl, param);
        if (Objects.isNull(responseInfo)) {
            return null;
        }

        String status = responseInfo.getStatus();
        if (StringUtils.isBlank(status) || !StringUtils.equals("success", status)) {
            return null;
        }
        return responseInfo.getData();
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