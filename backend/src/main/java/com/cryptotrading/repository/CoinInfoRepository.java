package com.cryptotrading.repository;

import com.cryptotrading.entity.CoinInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;  

@Repository
public interface CoinInfoRepository extends JpaRepository<CoinInfo, String> {
    
    @Query("SELECT c FROM CoinInfo c WHERE c.isActive = :isActive " +
           "ORDER BY CASE WHEN c.marketCapRank IS NULL THEN 1 ELSE 0 END ASC, " +
           "c.marketCapRank ASC")
    List<CoinInfo> findByIsActiveOrderByMarketCapRank(@Param("isActive") Boolean isActive);
    
    List<CoinInfo> findBySymbolIn(List<String> symbols);

    // 심볼로 코인 정보 조회 (코인별 수익 분석용)
    Optional<CoinInfo> findBySymbol(String symbol);
}