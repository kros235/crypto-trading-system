package com.cryptotrading.repository;

import com.cryptotrading.entity.CoinNewsAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoinNewsAnalysisRepository extends JpaRepository<CoinNewsAnalysis, Long> {
    
    /**
     * 사용자 + 코인 + 날짜로 분석 결과 조회
     */
    Optional<CoinNewsAnalysis> findByUserIdAndCoinSymbolAndAnalysisDate(
            String userId, String coinSymbol, LocalDate analysisDate
    );
    
    /**
     * 사용자의 당일 전체 분석 결과 조회
     */
    List<CoinNewsAnalysis> findByUserIdAndAnalysisDateOrderByCoinSymbol(
            String userId, LocalDate analysisDate
    );
    
    /**
     * 특정 코인의 최근 분석 결과 조회 (히스토리)
     */
    List<CoinNewsAnalysis> findByUserIdAndCoinSymbolOrderByAnalysisDateDesc(
            String userId, String coinSymbol
    );
    
    /**
     * 가중치가 0이 아닌 분석 결과 조회 (알림 발송용)
     */
    @Query("SELECT a FROM CoinNewsAnalysis a WHERE a.userId = :userId " +
           "AND a.analysisDate = :date AND a.weightAdjustment != 0")
    List<CoinNewsAnalysis> findSignificantAnalysis(
            @Param("userId") String userId, 
            @Param("date") LocalDate date
    );
    
    /**
     * 일일 초기화: 전날 가중치 리셋 (00:00 KST 실행)
     */
    @Modifying
    @Query("UPDATE CoinNewsAnalysis a SET a.weightAdjustment = 0, " +
           "a.sentiment = 'NEUTRAL', a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.analysisDate < :today")
    int resetPreviousDayWeights(@Param("today") LocalDate today);
    
    /**
     * 7일 초과 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM CoinNewsAnalysis a WHERE a.createdAt < :threshold")
    int deleteOldAnalysis(@Param("threshold") LocalDateTime threshold);

     /**
     * 특정 일자의 모든 가중치를 0으로 초기화 (관리자 수동 초기화용)
     */
    @Modifying
    @Query("UPDATE CoinNewsAnalysis a SET a.weightAdjustment = 0 WHERE a.analysisDate = :date")
    int resetWeightsByDate(@Param("date") LocalDate date);
}