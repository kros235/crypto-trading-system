package com.cryptotrading.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    // Rate Limit 설정
    private static final int MAX_REQUESTS_PER_MINUTE = 60;  // 분당 최대 요청 수
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 정적 리소스 및 헬스체크는 제외
        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = RATE_LIMIT_PREFIX + clientIp;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            if (currentCount == 1) {
                // 첫 요청이면 TTL 설정
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            // 헤더에 Rate Limit 정보 추가
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", 
                    String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));

            if (currentCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit 초과: IP={}, 요청수={}", clientIp, currentCount);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"success\":false,\"error\":{\"code\":\"C006\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}}");
                return;
            }

        } catch (Exception e) {
            // Redis 오류 시에도 요청은 통과시킴 (가용성 우선)
            log.warn("Rate limit 체크 실패 (Redis 오류): {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit 제외 경로
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/api/health") ||
               path.startsWith("/static/") ||
               path.startsWith("/assets/") ||
               path.endsWith(".js") ||
               path.endsWith(".css") ||
               path.endsWith(".ico");
    }

    /**
     * 클라이언트 IP 추출 (프록시 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}