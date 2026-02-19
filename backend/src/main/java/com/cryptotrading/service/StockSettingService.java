package com.cryptotrading.service;

import com.cryptotrading.dto.stock.StockTradingSettingDTO;
import com.cryptotrading.entity.StockTradingSetting;
import com.cryptotrading.repository.StockTradingSettingRepository;
import com.cryptotrading.util.EncryptionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockSettingService {

    private final StockTradingSettingRepository settingRepository;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 주식 거래 설정 조회
     */
    public StockTradingSettingDTO getSettings(String userId) {
        Optional<StockTradingSetting> setting = settingRepository.findByUserId(userId);
        if (setting.isEmpty()) {
            return null;
        }
        return convertToDTO(setting.get());
    }

    /**
     * 주식 거래 설정 생성
     */
    @Transactional
    public StockTradingSettingDTO createSettings(String userId, StockTradingSettingDTO dto) {
        // 이미 설정이 있는지 확인
        if (settingRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("이미 주식 거래 설정이 존재합니다. 수정을 이용해주세요.");
        }

        StockTradingSetting setting = new StockTradingSetting();
        setting.setUserId(userId);
        applyDTOToEntity(dto, setting);

        // KIS API 키 암호화 저장
        encryptAndSetKisApiKeys(dto, setting);

        settingRepository.save(setting);
        log.info("주식 거래 설정 생성 - userId: {}", userId);
        return convertToDTO(setting);
    }

    /**
     * 주식 거래 설정 수정
     */
    @Transactional
    public StockTradingSettingDTO updateSettings(String userId, StockTradingSettingDTO dto) {
        StockTradingSetting setting = settingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정을 찾을 수 없습니다."));

        applyDTOToEntity(dto, setting);

        // KIS API 키가 전달된 경우에만 업데이트 (빈값이면 기존 유지)
        if (dto.getKisAppKey() != null && !dto.getKisAppKey().isBlank()) {
            encryptAndSetKisApiKeys(dto, setting);
        }

        settingRepository.save(setting);
        log.info("주식 거래 설정 수정 - userId: {}", userId);
        return convertToDTO(setting);
    }

    /**
     * 주식 거래 설정 삭제
     */
    @Transactional
    public void deleteSettings(String userId) {
        StockTradingSetting setting = settingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정을 찾을 수 없습니다."));
        settingRepository.delete(setting);
        log.info("주식 거래 설정 삭제 - userId: {}", userId);
    }

    /**
     * KIS API 키 등록 여부 확인
     */
    public boolean hasKisApiKey(String userId) {
        Optional<StockTradingSetting> setting = settingRepository.findByUserId(userId);
        return setting.isPresent() && 
               setting.get().getKisAppKeyEncrypted() != null && 
               !setting.get().getKisAppKeyEncrypted().isBlank();
    }

    /**
     * KIS API 키만 별도 업데이트
     */
    @Transactional
    public void updateKisApiKeys(String userId, String appKey, String appSecret, String accountNo) {
        StockTradingSetting setting = settingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정을 먼저 생성해주세요."));

        try {
            setting.setKisAppKeyEncrypted(encryptionUtil.encrypt(appKey));
            setting.setKisAppSecretEncrypted(encryptionUtil.encrypt(appSecret));
            setting.setKisAccountNoEncrypted(encryptionUtil.encrypt(accountNo));
            settingRepository.save(setting);
            log.info("KIS API 키 업데이트 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error("KIS API 키 암호화 실패 - userId: {}", userId, e);
            throw new RuntimeException("API 키 저장에 실패했습니다.");
        }
    }

    /**
     * KIS API 키 삭제
     */
    @Transactional
    public void deleteKisApiKeys(String userId) {
        StockTradingSetting setting = settingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("주식 거래 설정을 찾을 수 없습니다."));
        setting.setKisAppKeyEncrypted(null);
        setting.setKisAppSecretEncrypted(null);
        setting.setKisAccountNoEncrypted(null);
        settingRepository.save(setting);
        log.info("KIS API 키 삭제 완료 - userId: {}", userId);
    }

    // ===== Private Helper Methods =====

    private void applyDTOToEntity(StockTradingSettingDTO dto, StockTradingSetting entity) {
        try {
            entity.setStockCodes(objectMapper.writeValueAsString(dto.getStockCodes()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("종목 코드 직렬화 실패", e);
        }
        entity.setBasePeriod(dto.getBasePeriod() != null ? dto.getBasePeriod() : 20);
        entity.setBuyThresholdPct(dto.getBuyThresholdPct() != null ? dto.getBuyThresholdPct() : new BigDecimal("-3.00"));
        entity.setSellTargetPct(dto.getSellTargetPct() != null ? dto.getSellTargetPct() : new BigDecimal("2.50"));
        entity.setStopLossPct(dto.getStopLossPct() != null ? dto.getStopLossPct() : new BigDecimal("-5.00"));
        entity.setMaxHoldingsPerStock(dto.getMaxHoldingsPerStock() != null ? dto.getMaxHoldingsPerStock() : 3);
        entity.setDailyLimitAmount(dto.getDailyLimitAmount() != null ? dto.getDailyLimitAmount() : new BigDecimal("1000000.00"));
        entity.setUseTrailingStop(dto.getUseTrailingStop() != null ? dto.getUseTrailingStop() : true);
        entity.setTrailingStopPct(dto.getTrailingStopPct() != null ? dto.getTrailingStopPct() : new BigDecimal("-2.50"));
        
        // 기술적 지표
        entity.setRsiPeriod(dto.getRsiPeriod() != null ? dto.getRsiPeriod() : 14);
        entity.setRsiBuyThreshold(dto.getRsiBuyThreshold() != null ? dto.getRsiBuyThreshold() : 35);
        entity.setRsiSellThreshold(dto.getRsiSellThreshold() != null ? dto.getRsiSellThreshold() : 65);
        entity.setBbPeriod(dto.getBbPeriod() != null ? dto.getBbPeriod() : 20);
        entity.setBbMultiplier(dto.getBbMultiplier() != null ? dto.getBbMultiplier() : 2);
        entity.setVolumeThreshold(dto.getVolumeThreshold() != null ? dto.getVolumeThreshold() : 120);
        
        // 리스크 관리
        entity.setDailyTradeLimitPct(dto.getDailyTradeLimitPct() != null ? dto.getDailyTradeLimitPct() : 20);
        entity.setMaxPositionPct(dto.getMaxPositionPct() != null ? dto.getMaxPositionPct() : 25);
        entity.setDailyStopLossPct(dto.getDailyStopLossPct() != null ? dto.getDailyStopLossPct() : -5);
        entity.setUseMarketTrendFilter(dto.getUseMarketTrendFilter() != null ? dto.getUseMarketTrendFilter() : false);
        entity.setCumulativeLossLimitPct(dto.getCumulativeLossLimitPct() != null ? dto.getCumulativeLossLimitPct() : -10);
        entity.setConsecutiveStopLossLimit(dto.getConsecutiveStopLossLimit() != null ? dto.getConsecutiveStopLossLimit() : 3);
        entity.setFixedBuyAmount(dto.getFixedBuyAmount() != null ? dto.getFixedBuyAmount() : new BigDecimal("100000.00"));
        entity.setUseDailyLimitRecovery(dto.getUseDailyLimitRecovery() != null ? dto.getUseDailyLimitRecovery() : false);
        entity.setUseRoundRobin(dto.getUseRoundRobin() != null ? dto.getUseRoundRobin() : true);
        
        // Phase 2 전용
        entity.setMaxHoldingDays(dto.getMaxHoldingDays() != null ? dto.getMaxHoldingDays() : 20);
        entity.setKisMockMode(dto.getKisMockMode() != null ? dto.getKisMockMode() : true);
    }

    private void encryptAndSetKisApiKeys(StockTradingSettingDTO dto, StockTradingSetting entity) {
        try {
            if (dto.getKisAppKey() != null && !dto.getKisAppKey().isBlank()) {
                entity.setKisAppKeyEncrypted(encryptionUtil.encrypt(dto.getKisAppKey()));
            }
            if (dto.getKisAppSecret() != null && !dto.getKisAppSecret().isBlank()) {
                entity.setKisAppSecretEncrypted(encryptionUtil.encrypt(dto.getKisAppSecret()));
            }
            if (dto.getKisAccountNo() != null && !dto.getKisAccountNo().isBlank()) {
                entity.setKisAccountNoEncrypted(encryptionUtil.encrypt(dto.getKisAccountNo()));
            }
        } catch (Exception e) {
            log.error("KIS API 키 암호화 실패", e);
            throw new RuntimeException("API 키 암호화에 실패했습니다.");
        }
    }

    private StockTradingSettingDTO convertToDTO(StockTradingSetting entity) {
        List<String> stockCodes;
        try {
            stockCodes = objectMapper.readValue(entity.getStockCodes(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            stockCodes = List.of();
        }

        return StockTradingSettingDTO.builder()
                .stockCodes(stockCodes)
                .basePeriod(entity.getBasePeriod())
                .buyThresholdPct(entity.getBuyThresholdPct())
                .sellTargetPct(entity.getSellTargetPct())
                .stopLossPct(entity.getStopLossPct())
                .maxHoldingsPerStock(entity.getMaxHoldingsPerStock())
                .dailyLimitAmount(entity.getDailyLimitAmount())
                .useTrailingStop(entity.getUseTrailingStop())
                .trailingStopPct(entity.getTrailingStopPct())
                .rsiPeriod(entity.getRsiPeriod())
                .rsiBuyThreshold(entity.getRsiBuyThreshold())
                .rsiSellThreshold(entity.getRsiSellThreshold())
                .bbPeriod(entity.getBbPeriod())
                .bbMultiplier(entity.getBbMultiplier())
                .volumeThreshold(entity.getVolumeThreshold())
                .dailyTradeLimitPct(entity.getDailyTradeLimitPct())
                .maxPositionPct(entity.getMaxPositionPct())
                .dailyStopLossPct(entity.getDailyStopLossPct())
                .useMarketTrendFilter(entity.getUseMarketTrendFilter())
                .cumulativeLossLimitPct(entity.getCumulativeLossLimitPct())
                .consecutiveStopLossLimit(entity.getConsecutiveStopLossLimit())
                .fixedBuyAmount(entity.getFixedBuyAmount())
                .useDailyLimitRecovery(entity.getUseDailyLimitRecovery())
                .useRoundRobin(entity.getUseRoundRobin())
                .maxHoldingDays(entity.getMaxHoldingDays())
                .kisMockMode(entity.getKisMockMode())
                // API 키는 노출하지 않음 (등록 여부만)
                .hasKisApiKey(entity.getKisAppKeyEncrypted() != null && 
                             !entity.getKisAppKeyEncrypted().isBlank())
                .build();
    }
}