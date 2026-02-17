package com.cryptotrading.repository;

import com.cryptotrading.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {

    // 종목별 최근 N일 가격 이력 조회
    @Query("SELECT p FROM StockPriceHistory p WHERE p.stockCode = :stockCode " +
           "ORDER BY p.timestamp DESC LIMIT :limit")
    List<StockPriceHistory> findRecentByStockCode(@Param("stockCode") String stockCode, @Param("limit") int limit);

    // 종목별 기간 내 가격 이력 조회
    List<StockPriceHistory> findByStockCodeAndTimestampBetweenOrderByTimestampAsc(
            String stockCode, LocalDate startDate, LocalDate endDate);

    // 종목별 최신 가격
    Optional<StockPriceHistory> findFirstByStockCodeOrderByTimestampDesc(String stockCode);

    // 종목 + 날짜 조합으로 조회
    Optional<StockPriceHistory> findByStockCodeAndTimestamp(String stockCode, LocalDate timestamp);

    // 종목별 가격 이력 (날짜 오름차순)
    List<StockPriceHistory> findByStockCodeOrderByTimestampAsc(String stockCode);
}