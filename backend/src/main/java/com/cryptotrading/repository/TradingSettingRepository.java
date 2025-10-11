package com.cryptotrading.repository;

import com.cryptotrading.entity.TradingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradingSettingRepository extends JpaRepository<TradingSetting, Long> {
    
    Optional<TradingSetting> findByUserId(String userId);
}