package com.cryptotrading.repository;

import com.cryptotrading.entity.StockTradingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTradingSettingRepository extends JpaRepository<StockTradingSetting, Long> {

    Optional<StockTradingSetting> findByUserId(String userId);

    List<StockTradingSetting> findAll();

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}