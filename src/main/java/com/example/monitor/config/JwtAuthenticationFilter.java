package com.example.monitor.config;

import com.example.monitor.util.JwtUtil;
import com.example.monitor.util.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST = Arrays.asList(
            "/monitor/api/auth/login",
            "/monitor/api/auth/register",
            "/monitor/api/auth/refresh",
            "/monitor/api/auth/password"
    );

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (WHITELIST.stream().anyMatch(path::equals)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Missing or invalid Authorization header\",\"data\":null}");
            return;
        }

        String token = authorization.substring(7);
        if (tokenStore.isBlacklisted(token) || !jwtUtil.validToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token invalid or expired\",\"data\":null}");
            return;
        }

        // 检查token类型，必须是access token
        String tokenType = jwtUtil.getTokenType(token);
        if (!"access".equals(tokenType)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid token type\",\"data\":null}");
            return;
        }

        // 将用户信息注入请求上下文
        Integer userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", username);

        // 检查access token是否即将过期（5分钟内），添加响应头提示
        if (jwtUtil.isTokenExpiringSoon(token, 300)) { // 5分钟缓冲
            response.setHeader("X-Token-Expiring-Soon", "true");
        }

        filterChain.doFilter(request, response);
    }
}
