package com.example.monitor.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Throwables;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.example.monitor.entity.prometheus.PromSeries;
import com.example.monitor.entity.prometheus.PromQueryData;
import com.example.monitor.entity.prometheus.PromQueryResponse;
import com.example.monitor.service.PromSeriesService;
import com.example.monitor.utils.RestTemplateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * 指标查询接口实现类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Slf4j
@Service("promSeriesService")
public class PromSeriesServiceImpl implements com.example.monitor.service.PromSeriesService {
    @Resource
    private RestTemplateUtils restTemplateUtils;
    
    @Override
    public List<LinkedHashMap<String, Object>> getSeriesList(String start, String end, String match, Integer datasource) {
        JSONObject param = new JSONObject();
        param.put("start", start);
        param.put("end", end);
        param.put("match[]", match);

        // prometheus的URL连接地址, 根据需要修改
        String url = "http://localhost:9090" + "/api/v1/series";
        return getSeriesDataList(url, param);
    }
    
    /**
     * 获取时序数据列表
     *
     * @param promUrl 时序URL
     * @param param   请求参数
     * @return 时序数据列表
     */
    private List<LinkedHashMap<String, Object>> getSeriesDataList(String promUrl, JSONObject param) {
        String http = getHttp(promUrl, param);
        PromSeries promSeries = JSON.parseObject(http, PromSeries.class);
        if (Objects.nonNull(promSeries)) {
            String status = promSeries.getStatus();
            if (StringUtils.isBlank(status) || !StringUtils.equals("success", status)) {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }

        return promSeries.getData();
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