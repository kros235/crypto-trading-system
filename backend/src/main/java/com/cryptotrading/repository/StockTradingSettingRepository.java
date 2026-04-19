package com.cryptotrading.repository;

import com.cryptotrading.entity.StockTradingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTradingSettingRepository extends JpaRepository<StockTradingSetting, Long> {

    Optional<StockTradingSetting> findByUserId(String userId);

    // ⭐ [추가] 중복 데이터 방어: 최신 1건만 조회
    Optional<StockTradingSetting> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    // ⭐ [추가] 중복 존재 여부 카운트 (NonUniqueResultException 방지)
    long countByUserId(String userId);

    List<StockTradingSetting> findAll();

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);

    // ⭐ [추가] 중복 데이터 전체 삭제
    void deleteAllByUserId(String userId);
}