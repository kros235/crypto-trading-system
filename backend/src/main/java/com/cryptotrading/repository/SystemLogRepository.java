package com.cryptotrading.repository;

import com.cryptotrading.entity.LogLevel;
import com.cryptotrading.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    List<SystemLog> findByUserIdAndTimestampBetween(
        String userId, LocalDateTime start, LocalDateTime end
    );
    
    List<SystemLog> findByLevelAndTimestampAfter(
        LogLevel level, LocalDateTime after
    );
}