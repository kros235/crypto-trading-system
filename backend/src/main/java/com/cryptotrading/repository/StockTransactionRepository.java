package com.cryptotrading.repository;

import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    // 사용자별 보유 중인 거래 조회
    List<StockTransaction> findByUserIdAndStatus(String userId, TransactionStatus status);

    // 사용자별 종목별 보유 건수
    long countByUserIdAndStockCodeAndStatus(String userId, String stockCode, TransactionStatus status);

    // 사용자별 거래 내역 (페이징)
    Page<StockTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // 사용자별 기간 내 매도 거래
    @Query("SELECT t FROM StockTransaction t WHERE t.userId = :userId AND t.status = 'SOLD' " +
           "AND t.soldAt BETWEEN :startDate AND :endDate ORDER BY t.soldAt DESC")
    List<StockTransaction> findSoldTransactionsByDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 사용자별 당일 매수 건수
    @Query("SELECT COUNT(t) FROM StockTransaction t WHERE t.userId = :userId AND t.type = 'BUY' " +
           "AND t.createdAt >= :startOfDay")
    long countTodayBuyTransactions(@Param("userId") String userId, @Param("startOfDay") LocalDateTime startOfDay);

    // 사용자별 당일 매수 금액 합계
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM StockTransaction t WHERE t.userId = :userId " +
           "AND t.type = 'BUY' AND t.createdAt >= :startOfDay")
    java.math.BigDecimal sumTodayBuyAmount(@Param("userId") String userId, @Param("startOfDay") LocalDateTime startOfDay);

    // 종목별 연속 손절 횟수
    @Query("SELECT COUNT(t) FROM StockTransaction t WHERE t.userId = :userId AND t.stockCode = :stockCode " +
           "AND t.status = 'SOLD' AND t.profitLoss < 0 AND t.soldAt >= :since ORDER BY t.soldAt DESC")
    long countConsecutiveStopLosses(@Param("userId") String userId, @Param("stockCode") String stockCode,
                                     @Param("since") LocalDateTime since);
}