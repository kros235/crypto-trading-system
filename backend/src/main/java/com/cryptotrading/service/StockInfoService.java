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

    // ──────────────────────────────────────────────────────────
    // ⭐ [Day 60 추가] 다중 종목 현재가 일괄 조회
    // ──────────────────────────────────────────────────────────
    /**
     * 여러 종목의 현재가/변동률을 일괄 조회하여 반환
     *
     * - 내부적으로 KisApiService.getCurrentPrice(userId, stockCode)를 N번 호출
     * - KIS API 키 미등록 사용자나 일부 종목 조회 실패 시에도 graceful degradation
     *   (실패한 종목은 currentPrice 등 필드가 null인 객체로 반환)
     * - 종목 목록(StockListView) / 보유 자산(StockHoldingsView) 등에서 사용
     *
     * @param userId     사용자 ID (KIS API 인증용)
     * @param stockCodes 조회할 종목코드 목록
     * @return 종목별 가격 정보 리스트 (요청 순서 유지)
     */
    public List<com.cryptotrading.dto.stock.StockPriceDTO> getPricesForStocks(
            String userId, List<String> stockCodes) {

        if (stockCodes == null || stockCodes.isEmpty()) {
            return List.of();
        }

        return stockCodes.stream()
                .map(code -> {
                    try {
                        com.cryptotrading.dto.kis.KisQuoteDTO.CurrentPrice quote =
                                kisApiService.getCurrentPrice(userId, code);
                        return com.cryptotrading.dto.stock.StockPriceDTO.fromKisQuote(code, quote);
                    } catch (Exception e) {
                        log.warn("[종목 가격 조회 실패] stockCode={}, error={}", code, e.getMessage());
                        // 실패해도 stockCode만 채워서 반환 (프론트에서 '-' 표시)
                        return com.cryptotrading.dto.stock.StockPriceDTO.builder()
                                .stockCode(code)
                                .build();
                    }
                })
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