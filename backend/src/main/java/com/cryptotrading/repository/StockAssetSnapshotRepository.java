package com.cryptotrading.repository;

import com.cryptotrading.entity.StockAssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ⭐ [수정 Q6] 주식 자산 스냅샷 Repository
 */
@Repository
public interface StockAssetSnapshotRepository extends JpaRepository<StockAssetSnapshot, Long> {

    List<StockAssetSnapshot> findByUserIdAndDateBetweenOrderByDateAsc(
        String userId, LocalDate startDate, LocalDate endDate);

    Optional<StockAssetSnapshot> findByUserIdAndDate(String userId, LocalDate date);

    List<StockAssetSnapshot> findByUserIdOrderByDateAsc(String userId);
}