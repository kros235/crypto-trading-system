package com.cryptotrading.repository;

import com.cryptotrading.entity.CoinInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;  

@Repository
public interface CoinInfoRepository extends JpaRepository<CoinInfo, String> {
    
    List<CoinInfo> findByIsActiveOrderByMarketCapRank(Boolean isActive);
    
    List<CoinInfo> findBySymbolIn(List<String> symbols);

    // 심볼로 코인 정보 조회 (코인별 수익 분석용)
    Optional<CoinInfo> findBySymbol(String symbol);
}