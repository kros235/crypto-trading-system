package com.cryptotrading.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000; // 1초 이상이면 슬로우

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            // 정적 리소스 제외
            if (!requestUri.contains("/static/") && !requestUri.contains("/assets/")) {
                if (duration >= SLOW_REQUEST_THRESHOLD_MS) {
                    log.warn("[SLOW] {} {} - {}ms (status: {})", method, requestUri, duration, status);
                } else if (log.isDebugEnabled()) {
                    log.debug("{} {} - {}ms (status: {})", method, requestUri, duration, status);
                }
            }
        }
    }
}