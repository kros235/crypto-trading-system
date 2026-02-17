package com.cryptotrading.repository;

import com.cryptotrading.entity.StockDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDailySummaryRepository extends JpaRepository<StockDailySummary, Long> {

    Optional<StockDailySummary> findByUserIdAndDate(String userId, LocalDate date);

    List<StockDailySummary> findByUserIdAndDateBetweenOrderByDateAsc(
            String userId, LocalDate startDate, LocalDate endDate);

    List<StockDailySummary> findByUserIdOrderByDateDesc(String userId);

    boolean existsByUserIdAndDate(String userId, LocalDate date);
}