package com.cryptotrading.repository;

import com.cryptotrading.entity.TradingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TradingSettingRepository extends JpaRepository<TradingSetting, Long> {
    
    Optional<TradingSetting> findByUserId(String userId);

    // ⭐⭐⭐ [신규] Top10 자동 운영을 켠 사용자만 조회 (매일 04:00 리밸런싱 대상) ⭐⭐⭐
    List<TradingSetting> findByUseTop10AutoRebalanceTrue();
}