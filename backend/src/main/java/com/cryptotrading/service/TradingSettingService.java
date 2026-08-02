package com.cryptotrading.service;

import com.cryptotrading.dto.TradingSettingDTO;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.repository.TradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSettingService {

    private final TradingSettingRepository tradingSettingRepository;

    @Transactional(readOnly = true)
    public TradingSettingDTO getTradingSetting(String userId) {
        log.info("거래 설정 조회: userId={}", userId);

        Optional<TradingSetting> setting = tradingSettingRepository.findByUserId(userId);

        if (setting.isEmpty()) {
            log.info("거래 설정이 없습니다: userId={}", userId);
            return null; // 예외 던지지 않고 null 반환
        }

        return convertToDTO(setting.get());
    }

    @Transactional
    public TradingSettingDTO createTradingSetting(String userId, TradingSettingDTO dto) {
        if (tradingSettingRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("이미 거래 설정이 존재합니다. 수정 기능을 사용해주세요.");
        }

        TradingSetting setting = TradingSetting.builder()
                .userId(userId)
                .coinSymbols(dto.getCoinSymbols())
                .basePeriod(dto.getBasePeriod())
                .buyThresholdPct(dto.getBuyThresholdPct())
                .sellTargetPct(dto.getSellTargetPct())
                .stopLossPct(dto.getStopLossPct())
                .maxHoldingsPerCoin(dto.getMaxHoldingsPerCoin())
                // ⚠️ dailyLimitAmount는 deprecated - 기존 호환성을 위해 값은 저장하되 사용하지 않음
                .dailyLimitAmount(
                        dto.getDailyLimitAmount() != null ? dto.getDailyLimitAmount() : new BigDecimal("1000000.00"))
                .useAiAnalysis(dto.getUseAiAnalysis())
                .useTrailingStop(dto.getUseTrailingStop())
                .trailingStopPct(dto.getTrailingStopPct())
                .rsiPeriod(dto.getRsiPeriod() != null ? dto.getRsiPeriod() : 14)
                .rsiBuyThreshold(dto.getRsiBuyThreshold() != null ? dto.getRsiBuyThreshold() : 32)
                .rsiSellThreshold(dto.getRsiSellThreshold() != null ? dto.getRsiSellThreshold() : 68)
                .bbPeriod(dto.getBbPeriod() != null ? dto.getBbPeriod() : 20)
                .bbMultiplier(dto.getBbMultiplier() != null ? dto.getBbMultiplier() : 2)
                .volumeThreshold(dto.getVolumeThreshold() != null ? dto.getVolumeThreshold() : 140)
                .dailyTradeLimitPct(dto.getDailyTradeLimitPct() != null ? dto.getDailyTradeLimitPct() : 20)
                .maxPositionPct(dto.getMaxPositionPct() != null ? dto.getMaxPositionPct() : 25)
                .dailyStopLossPct(dto.getDailyStopLossPct() != null ? dto.getDailyStopLossPct() : -5)
                .useMarketTrendFilter(dto.getUseMarketTrendFilter() != null ? dto.getUseMarketTrendFilter() : false)
                .cumulativeLossLimitPct(dto.getCumulativeLossLimitPct() != null ? dto.getCumulativeLossLimitPct() : -10)
                .consecutiveStopLossLimit(
                        dto.getConsecutiveStopLossLimit() != null ? dto.getConsecutiveStopLossLimit() : 3)
                // ⭐⭐⭐ 변경: buyAmountPct → fixedBuyAmount ⭐⭐⭐
                .fixedBuyAmount(dto.getFixedBuyAmount() != null ? dto.getFixedBuyAmount() : new BigDecimal("10000.00"))
                .useDailyLimitRecovery(dto.getUseDailyLimitRecovery() != null ? dto.getUseDailyLimitRecovery() : false)
                // ⭐⭐⭐ 변경: usePerTradeLimit → useRoundRobin ⭐⭐⭐
                .useRoundRobin(dto.getUseRoundRobin() != null ? dto.getUseRoundRobin() : true)
                .additionalDropPct(
                        dto.getAdditionalDropPct() != null ? dto.getAdditionalDropPct() : new BigDecimal("0.5"))
                .useStopLoss(dto.getUseStopLoss() != null ? dto.getUseStopLoss() : true)
	  // ⭐⭐⭐ [신규] Top10 자동 운영 - 기본값 false (명시적으로 켠 사용자만 적용) ⭐⭐⭐
	  .useTop10AutoRebalance(
                dto.getUseTop10AutoRebalance() != null ? dto.getUseTop10AutoRebalance() : false)                
                .build();

        TradingSetting saved = tradingSettingRepository.save(setting);
        log.info("거래 설정 생성 완료: userId={}", userId);

        return convertToDTO(saved);
    }

    @Transactional
    public TradingSettingDTO updateTradingSetting(String userId, TradingSettingDTO dto) {
        TradingSetting setting = tradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("거래 설정을 찾을 수 없습니다"));

        setting.setCoinSymbols(dto.getCoinSymbols());
        setting.setBasePeriod(dto.getBasePeriod());
        setting.setBuyThresholdPct(dto.getBuyThresholdPct());
        setting.setSellTargetPct(dto.getSellTargetPct());
        setting.setStopLossPct(dto.getStopLossPct());
        setting.setMaxHoldingsPerCoin(dto.getMaxHoldingsPerCoin());
        // ⚠️ dailyLimitAmount는 deprecated - 기존 호환성을 위해 값은 저장
        setting.setDailyLimitAmount(dto.getDailyLimitAmount());
        setting.setUseAiAnalysis(dto.getUseAiAnalysis());
        setting.setUseTrailingStop(dto.getUseTrailingStop());
        setting.setTrailingStopPct(dto.getTrailingStopPct());
        setting.setRsiPeriod(dto.getRsiPeriod() != null ? dto.getRsiPeriod() : 14);
        setting.setRsiBuyThreshold(dto.getRsiBuyThreshold() != null ? dto.getRsiBuyThreshold() : 30);
        setting.setRsiSellThreshold(dto.getRsiSellThreshold() != null ? dto.getRsiSellThreshold() : 70);
        setting.setBbPeriod(dto.getBbPeriod() != null ? dto.getBbPeriod() : 20);
        setting.setBbMultiplier(dto.getBbMultiplier() != null ? dto.getBbMultiplier() : 2);
        setting.setVolumeThreshold(dto.getVolumeThreshold() != null ? dto.getVolumeThreshold() : 150);
        setting.setDailyTradeLimitPct(dto.getDailyTradeLimitPct() != null ? dto.getDailyTradeLimitPct() : 20);
        setting.setMaxPositionPct(dto.getMaxPositionPct() != null ? dto.getMaxPositionPct() : 25);
        setting.setDailyStopLossPct(dto.getDailyStopLossPct() != null ? dto.getDailyStopLossPct() : -5);
        setting.setUseMarketTrendFilter(dto.getUseMarketTrendFilter() != null ? dto.getUseMarketTrendFilter() : false);
        setting.setCumulativeLossLimitPct(
                dto.getCumulativeLossLimitPct() != null ? dto.getCumulativeLossLimitPct() : -10);
        setting.setConsecutiveStopLossLimit(
                dto.getConsecutiveStopLossLimit() != null ? dto.getConsecutiveStopLossLimit() : 3);
        // ⭐⭐⭐ 변경: buyAmountPct → fixedBuyAmount ⭐⭐⭐
        setting.setFixedBuyAmount(
                dto.getFixedBuyAmount() != null ? dto.getFixedBuyAmount() : new BigDecimal("10000.00"));
        setting.setUseDailyLimitRecovery(
                dto.getUseDailyLimitRecovery() != null ? dto.getUseDailyLimitRecovery() : false);
        // ⭐⭐⭐ 변경: usePerTradeLimit → useRoundRobin ⭐⭐⭐
        setting.setUseRoundRobin(dto.getUseRoundRobin() != null ? dto.getUseRoundRobin() : true);
        setting.setAdditionalDropPct(
                dto.getAdditionalDropPct() != null ? dto.getAdditionalDropPct() : new BigDecimal("0.5"));
        setting.setUseStopLoss(dto.getUseStopLoss() != null ? dto.getUseStopLoss() : true);

        // ⭐⭐⭐ [신규] Top10 자동 운영 ⭐⭐⭐
       setting.setUseTop10AutoRebalance(
               dto.getUseTop10AutoRebalance() != null ? dto.getUseTop10AutoRebalance() : false);

        TradingSetting updated = tradingSettingRepository.save(setting);
        log.info("거래 설정 수정 완료: userId={}", userId);

        return convertToDTO(updated);
    }

    @Transactional
    public void deleteTradingSetting(String userId) {
        TradingSetting setting = tradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("거래 설정을 찾을 수 없습니다"));

        tradingSettingRepository.delete(setting);
        log.info("거래 설정 삭제 완료: userId={}", userId);
    }

    private TradingSettingDTO convertToDTO(TradingSetting setting) {
        return TradingSettingDTO.builder()
                .id(setting.getId())
                .coinSymbols(setting.getCoinSymbols())
                .basePeriod(setting.getBasePeriod())
                .buyThresholdPct(setting.getBuyThresholdPct())
                .sellTargetPct(setting.getSellTargetPct())
                .stopLossPct(setting.getStopLossPct())
                .maxHoldingsPerCoin(setting.getMaxHoldingsPerCoin())
                // ⚠️ dailyLimitAmount는 deprecated
                .dailyLimitAmount(setting.getDailyLimitAmount())
                .useAiAnalysis(setting.getUseAiAnalysis())
                .useTrailingStop(setting.getUseTrailingStop())
                .trailingStopPct(setting.getTrailingStopPct())
                .rsiPeriod(setting.getRsiPeriod())
                .rsiBuyThreshold(setting.getRsiBuyThreshold())
                .rsiSellThreshold(setting.getRsiSellThreshold())
                .bbPeriod(setting.getBbPeriod())
                .bbMultiplier(setting.getBbMultiplier())
                .volumeThreshold(setting.getVolumeThreshold())
                .dailyTradeLimitPct(setting.getDailyTradeLimitPct())
                .maxPositionPct(setting.getMaxPositionPct())
                .dailyStopLossPct(setting.getDailyStopLossPct())
                .useMarketTrendFilter(setting.getUseMarketTrendFilter())
                .cumulativeLossLimitPct(setting.getCumulativeLossLimitPct())
                .consecutiveStopLossLimit(setting.getConsecutiveStopLossLimit())
                // ⭐⭐⭐ 변경: buyAmountPct → fixedBuyAmount ⭐⭐⭐
                .fixedBuyAmount(setting.getFixedBuyAmount())
                .useDailyLimitRecovery(setting.getUseDailyLimitRecovery())
                // ⭐⭐⭐ 변경: usePerTradeLimit → useRoundRobin ⭐⭐⭐
                .useRoundRobin(setting.getUseRoundRobin())
                .additionalDropPct(setting.getAdditionalDropPct())
                .useStopLoss(setting.getUseStopLoss())
                // ⭐⭐⭐ [신규] Top10 자동 운영 ⭐⭐⭐
                .useTop10AutoRebalance(setting.getUseTop10AutoRebalance())
                .build();
    }
}