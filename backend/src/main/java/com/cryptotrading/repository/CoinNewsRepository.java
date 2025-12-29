package com.cryptotrading.repository;

import com.cryptotrading.entity.CoinNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CoinNewsRepository extends JpaRepository<CoinNews, Long> {
    
    /**
     * 특정 코인의 당일 뉴스 조회
     */
    @Query("SELECT n FROM CoinNews n WHERE n.coinSymbol = :symbol " +
           "AND n.publishedAt >= :startOfDay AND n.publishedAt < :endOfDay " +
           "ORDER BY n.publishedAt DESC")
    List<CoinNews> findTodayNewsByCoinSymbol(
            @Param("symbol") String symbol,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
    
    /**
     * 특정 기간 내 특정 코인의 뉴스 조회
     */
    List<CoinNews> findByCoinSymbolAndPublishedAtBetweenOrderByPublishedAtDesc(
            String coinSymbol, LocalDateTime start, LocalDateTime end
    );
    
    /**
     * 중복 체크: 동일 제목 + 출처 존재 여부
     */
    boolean existsByTitleAndSource(String title, String source);
    
    /**
     * 7일 초과 데이터 삭제 (데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM CoinNews n WHERE n.createdAt < :threshold")
    int deleteOldNews(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 코인별 뉴스 수 집계
     */
    @Query("SELECT n.coinSymbol, COUNT(n) FROM CoinNews n " +
           "WHERE n.publishedAt >= :startOfDay AND n.publishedAt < :endOfDay " +
           "GROUP BY n.coinSymbol")
    List<Object[]> countTodayNewsBySymbol(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}