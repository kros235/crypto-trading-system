package com.cryptotrading.repository;

import com.cryptotrading.entity.CoinInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinInfoRepository extends JpaRepository<CoinInfo, String> {
    
    List<CoinInfo> findByIsActiveOrderByMarketCapRank(Boolean isActive);
    
    List<CoinInfo> findBySymbolIn(List<String> symbols);
}