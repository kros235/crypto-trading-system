package com.cryptotrading.repository;
import com.cryptotrading.entity.StockInfo;
import com.cryptotrading.entity.StockInfo.EtfType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockInfoRepository extends JpaRepository<StockInfo, String> {
    List<StockInfo> findByIsActiveTrue();
    List<StockInfo> findByEtfType(EtfType etfType);
    List<StockInfo> findByMarket(String market);
    List<StockInfo> findByIsActiveTrueOrderByStockCodeAsc();
    List<StockInfo> findByStockCodeIn(List<String> stockCodes);

    // ⭐ Day 51 추가: StockInfoService에서 사용하는 쿼리 메서드
    List<StockInfo> findByStockNameContaining(String keyword);
    List<StockInfo> findByEtfTypeAndIsActiveTrue(EtfType etfType);  // ⭐ String → EtfType 변경
}