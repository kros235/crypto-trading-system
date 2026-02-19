package com.cryptotrading.service;

import com.cryptotrading.dto.stock.StockInfoDTO;
import com.cryptotrading.entity.StockInfo;
import com.cryptotrading.repository.StockInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockInfoService {

    private final StockInfoRepository stockInfoRepository;
    private final KisApiService kisApiService;

    /**
     * 활성화된 종목 목록 조회
     */
    public List<StockInfoDTO> getActiveStocks() {
        return stockInfoRepository.findByIsActiveTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 전체 종목 목록 조회
     */
    public List<StockInfoDTO> getAllStocks() {
        return stockInfoRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 종목 코드로 조회
     */
    public StockInfoDTO getStockByCode(String stockCode) {
        StockInfo stock = stockInfoRepository.findById(stockCode)
                .orElseThrow(() -> new RuntimeException("종목을 찾을 수 없습니다: " + stockCode));
        return convertToDTO(stock);
    }

    /**
     * 종목 추가 (KIS API 종목 검색 후 사용자가 선택하여 추가)
     */
    @Transactional
    public StockInfoDTO addStock(StockInfoDTO dto) {
        // 이미 등록된 종목인지 확인
        if (stockInfoRepository.existsById(dto.getStockCode())) {
            // 이미 존재하면 활성화만 처리
            StockInfo existing = stockInfoRepository.findById(dto.getStockCode()).get();
            existing.setIsActive(true);
            stockInfoRepository.save(existing);
            log.info("기존 종목 활성화: {} ({})", dto.getStockName(), dto.getStockCode());
            return convertToDTO(existing);
        }

        StockInfo stock = StockInfo.builder()
                .stockCode(dto.getStockCode())
                .stockName(dto.getStockName())
                .market(dto.getMarket() != null ? dto.getMarket() : "KRX")
                .etfType(dto.getEtfType() != null ? StockInfo.EtfType.valueOf(dto.getEtfType()) : StockInfo.EtfType.NORMAL)
                .underlyingIndex(dto.getUnderlyingIndex())
                .expenseRatio(dto.getExpenseRatio() != null ? 
                    java.math.BigDecimal.valueOf(dto.getExpenseRatio()) : null)
                .isActive(true)
                .build();

        stockInfoRepository.save(stock);
        log.info("신규 종목 등록: {} ({})", dto.getStockName(), dto.getStockCode());
        return convertToDTO(stock);
    }

    /**
     * 종목 비활성화 (삭제 대신 비활성화)
     */
    @Transactional
    public void deactivateStock(String stockCode) {
        StockInfo stock = stockInfoRepository.findById(stockCode)
                .orElseThrow(() -> new RuntimeException("종목을 찾을 수 없습니다: " + stockCode));
        stock.setIsActive(false);
        stockInfoRepository.save(stock);
        log.info("종목 비활성화: {} ({})", stock.getStockName(), stockCode);
    }

    /**
     * 종목 활성화
     */
    @Transactional
    public void activateStock(String stockCode) {
        StockInfo stock = stockInfoRepository.findById(stockCode)
                .orElseThrow(() -> new RuntimeException("종목을 찾을 수 없습니다: " + stockCode));
        stock.setIsActive(true);
        stockInfoRepository.save(stock);
        log.info("종목 활성화: {} ({})", stock.getStockName(), stockCode);
    }

    /**
     * KIS API로 종목 검색 (사용자가 종목 추가 시 검색용)
     * - KIS API의 종목 마스터 조회 기능 활용
     */
    public List<StockInfoDTO> searchStocks(String keyword) {
        try {
            return kisApiService.searchStocks(keyword);
        } catch (Exception e) {
            log.error("KIS API 종목 검색 실패: {}", e.getMessage());
            // API 실패 시 로컬 DB에서 검색
            return stockInfoRepository.findByStockNameContaining(keyword)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    /**
     * ETF 유형별 조회
     */
    public List<StockInfoDTO> getStocksByEtfType(String etfType) {
        // ⭐ String → EtfType enum 변환
        StockInfo.EtfType type;
        try {
            type = StockInfo.EtfType.valueOf(etfType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 ETF 유형: {}", etfType);
            return List.of();
        }
        return stockInfoRepository.findByEtfTypeAndIsActiveTrue(type)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Entity → DTO 변환
    private StockInfoDTO convertToDTO(StockInfo entity) {
        return StockInfoDTO.builder()
                .stockCode(entity.getStockCode())
                .stockName(entity.getStockName())
                .market(entity.getMarket())
                .etfType(entity.getEtfType() != null ? entity.getEtfType().name() : null)
                .underlyingIndex(entity.getUnderlyingIndex())
                .expenseRatio(entity.getExpenseRatio() != null ? 
                    entity.getExpenseRatio().doubleValue() : null)
                .isActive(entity.getIsActive())
                .build();
    }
}