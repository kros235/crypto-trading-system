package com.cryptotrading.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Crypto Trading System");
        response.put("version", "1.0.0");
        response.put("profile", System.getProperty("spring.profiles.active", "dev"));

        // Database 연결 체크
        Map<String, String> dbStatus = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            dbStatus.put("status", "UP");
            dbStatus.put("database", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            dbStatus.put("status", "DOWN");
            dbStatus.put("error", e.getMessage());
        }
        response.put("database", dbStatus);

        // Redis 연결 체크
        Map<String, String> redisStatus = new HashMap<>();
        try {
            redisConnectionFactory.getConnection().ping();
            redisStatus.put("status", "UP");
        } catch (Exception e) {
            redisStatus.put("status", "DOWN");
            redisStatus.put("error", e.getMessage());
        }
        response.put("redis", redisStatus);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/simple")
    public ResponseEntity<String> simpleHealth() {
        return ResponseEntity.ok("OK");
    }
}