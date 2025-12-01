package com.cryptotrading.repository;

import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.entity.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 사용자별 전체 거래 내역 조회 (페이징)
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // 사용자별 특정 코인 거래 내역 조회
    Page<Transaction> findByUserIdAndCoinSymbolOrderByCreatedAtDesc(
            String userId, String coinSymbol, Pageable pageable);

    // 사용자별 특정 상태의 거래 내역 조회
    Page<Transaction> findByUserIdAndStatusOrderByCreatedAtDesc(
            String userId, TransactionStatus status, Pageable pageable);

    // 사용자별 보유 중인 코인 목록 조회
    List<Transaction> findByUserIdAndStatus(String userId, TransactionStatus status);

    // 특정 코인 보유 수량 조회
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId " +
           "AND t.coinSymbol = :coinSymbol AND t.status = 'HOLDING'")
    long countHoldingsByUserIdAndCoinSymbol(@Param("userId") String userId, 
                                            @Param("coinSymbol") String coinSymbol);

    // 사용자별 일일 거래 금액 합계 조회
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = :type " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate")
    BigDecimal sumDailyTransactionAmount(@Param("userId") String userId,
                                         @Param("type") TransactionType type,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // 사용자별 총 수익/손실 조회
    @Query("SELECT COALESCE(SUM(t.profitLoss), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.status = 'SOLD'")
    BigDecimal sumTotalProfitLoss(@Param("userId") String userId);

    // 날짜 범위별 거래 내역 조회
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserIdAndDateRange(@Param("userId") String userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    // 복합 조건 검색 (코인 + 상태 + 날짜)
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND (:coinSymbol IS NULL OR t.coinSymbol = :coinSymbol) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt < :endDate) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> searchTransactions(@Param("userId") String userId,
                                         @Param("coinSymbol") String coinSymbol,
                                         @Param("status") TransactionStatus status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    // 거래 통계 - 총 매수 건수
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.type = 'BUY'")
    long countBuyTransactions(@Param("userId") String userId);

    // 거래 통계 - 총 매도 건수
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.type = 'SELL'")
    long countSellTransactions(@Param("userId") String userId);

    // 현재 보유 중인 총 투자금액
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.status = 'HOLDING'")
    BigDecimal sumHoldingAmount(@Param("userId") String userId);

    // 오늘 매수 총액 조회
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId AND t.type = 'BUY' " +
           "AND t.createdAt BETWEEN :startOfDay AND :endOfDay")
    BigDecimal sumTodayBuyAmount(@Param("userId") String userId, 
                                  @Param("startOfDay") LocalDateTime startOfDay,
                                  @Param("endOfDay") LocalDateTime endOfDay);
    
    // 특정 종목 보유 건수 조회
    long countByUserIdAndCoinSymbolAndStatus(String userId, String coinSymbol, TransactionStatus status);
    
    // 특정 종목의 보유 거래 조회
    List<Transaction> findByUserIdAndCoinSymbolAndStatus(String userId, String coinSymbol, TransactionStatus status);

    // 특정 기간 내 생성된 거래
    List<Transaction> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    // 특정 기간 내 매도된 거래
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.soldAt BETWEEN :start AND :end")
    List<Transaction> findByUserIdAndSoldAtBetween(@Param("userId") String userId, 
                                                    @Param("start") LocalDateTime start, 
                                                    @Param("end") LocalDateTime end);
}