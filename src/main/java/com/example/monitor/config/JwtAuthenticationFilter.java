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
            "/monitor/api/auth/refresh"
    );

    @Autowired
    private TokenStore tokenStore;

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
        if (tokenStore.isBlacklisted(token) || !JwtUtil.validToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token invalid or expired\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
