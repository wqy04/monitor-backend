package com.example.monitor.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * RestTemplate工具类
 *
 * @author 星空流年
 * @date 2023/7/10
 */
@Slf4j
@Component
@SuppressWarnings("all")
public class RestTemplateUtils {

    /**
     * http 请求 GET
     *
     * @param url    地址
     * @param params 参数
     * @return Http连接
     */
    public String getHttp(String url, JSONObject params) {
        return getRestConnection(url, params, "http");
    }

    /**
     * https 请求 GET
     *
     * @param url    地址
     * @param params 参数
     * @return Https连接
     */
    public String getHttps(String url, JSONObject params) {
        return getRestConnection(url, params, "https");
    }

    /**
     * 获取远程连接
     *
     * @param url            请求地址
     * @param params         JSON对象
     * @param connectionFlag 请求标志
     * @return 远程连接
     */
    private String getRestConnection(String url, JSONObject params, String connectionFlag) {
        String restConnection = null;
        if (StringUtils.equals("http", connectionFlag)) {
            restConnection = getRestHttpConnection(url, params, 10000, 60000, 3);
        }

        if (StringUtils.equals("https", connectionFlag)) {
            restConnection = getRestHttpsConnection(url, params, 10000, 60000, 3);
        }
        return restConnection;
    }

    /**
     * http 请求 GET
     *
     * @param url            地址
     * @param params         参数
     * @param connectTimeout 连接时间
     * @param readTimeout    读取时间
     * @param retryCount     重试机制
     * @return 请求字符串
     */
    public String getRestHttpConnection(String url, JSONObject params, int connectTimeout, int readTimeout, int retryCount) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        // 设置编码集
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        // 异常处理
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        // 获取URI
        URI uri = getUriByUrl(url, params);
        // 重试机制
        for (int i = 1; i <= retryCount; i++) {
            try {
                // 此处设置值为认证的用户名和密码信息, 请注意修改
                restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor("username", "password"));
                return restTemplate.getForEntity(uri, String.class).getBody();
            } catch (Exception e) {
                log.error("[GET/HTTP请求信息]异常, 重试次数:{}, 请求地址:{}, 请求参数:{}, 异常信息:{}", i, url, params, Throwables.getStackTraceAsString(e));
            }
        }
        return null;
    }

    /**
     * https 请求 GET
     *
     * @param url            地址
     * @param params         参数
     * @param connectTimeout 连接时间
     * @param readTimeout    读取时间
     * @param retryCount     重试机制
     * @return 请求字符串
     */
    public String getRestHttpsConnection(String url, JSONObject params, int connectTimeout, int readTimeout, int retryCount) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestTemplate restTemplate = restTemplate();
        clientHttpRequestFactory();
        // 设置编码集
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        // 异常处理
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        // 绕过https
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        // 获取URI
        URI uri = getUriByUrl(url, params);

        for (int i = 1; i <= retryCount; i++) {
            try {
                // 此处设置值为认证的用户名和密码信息, 请注意修改
                restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor("username", "password"));
                return restTemplate.getForEntity(uri, String.class).getBody();
            } catch (Exception e) {
                log.error("[GET/HTTPS请求信息]异常, 重试次数:{}, 请求地址:{}, 请求参数:{}, 异常信息:{}", i, url, params, Throwables.getStackTraceAsString(e));
            }
        }
        return null;
    }

    /**
     * 获取RestTemplate实例对象，可自由调用其方法
     *
     * @return RestTemplate实例对象
     */
    public HttpClient httpClient() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try {
            //设置信任SSL访问
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
            httpClientBuilder.setSSLContext(sslContext);
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    // 注册http和https请求
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory).build();

            //使用Httpclient连接池的方式配置
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            // 最大连接数
            poolingHttpClientConnectionManager.setMaxTotal(1000);
            // 同路由并发数
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(100);
            // 配置连接池
            httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);
            // 重试次数
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(1, true));
            // 设置默认请求头
            List<Header> headers = new ArrayList<>();
            httpClientBuilder.setDefaultHeaders(headers);
            // 设置请求连接超时时间
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(10000)
                    .setConnectTimeout(10000)
                    .setSocketTimeout(60000).build();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            return (HttpClient) httpClientBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException("System error: " + Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 创建RestTemplate
     *
     * @return RestTemplate
     */
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    /**
     * 创建ClientHttpRequestFactory
     *
     * @return ClientHttpRequestFactory
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    /**
     * 通过URL获取URI
     *
     * @param url    url
     * @param params 请求参数
     * @return {@code URI}
     */
    private URI getUriByUrl(String url, JSONObject params) {
        String query = "query";
        if (!params.isEmpty()) {
            // 网关针对URL中特殊字符进行加密访问, 这里针对网关未处理特殊字符参数进行转义处理
            if (params.containsKey(query)) {
                String replaceQuery = params.getString(query)
                        .replace("=", "%3D").replace(" ", "%20")
                        .replace("{", "%7B").replace("}", "%7D")
                        .replace("\"", "%22").replace("/", "%2F")
                        .replace("|", "%7C").replace("+", "%2B")
                        .replace("[", "%5B").replace("]", "%5D")
                        .replace("<", "%3C").replace(">", "%3E")
                        .replace("\n", "%20");
                params.put(query, replaceQuery);
            } else {
                params.keySet().forEach(key -> {
                    String decode = URLDecoder.decode(params.getString(key), StandardCharsets.UTF_8);
                    params.put(key, decode);
                });
            }
            url = expandUrl(url, params);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (params.containsKey(query)) {
            return builder.build(true).toUri();
        } else {
            return builder.build().encode().toUri();
        }
    }

    /**
     * URL拼接
     *
     * @param url        请求URL
     * @param jsonObject JSON对象
     * @return 拼接之后的URL
     */
    private String expandUrl(String url, JSONObject jsonObject) {
        HashMap<String, Object> paramMap = new HashMap<>(16);
        StringBuilder stringBuilder = new StringBuilder(url);
        stringBuilder.append("?");

        Set<String> keys = jsonObject.keySet();
        keys.forEach(key -> paramMap.put(key, jsonObject.getString(key)));
        String joinStr = Joiner.on("&").withKeyValueSeparator("=").join(paramMap);
        return stringBuilder.append(joinStr).toString();
    }
}