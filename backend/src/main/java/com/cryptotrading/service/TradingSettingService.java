package com.cryptotrading.service;

import com.cryptotrading.dto.TradingSettingDTO;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.repository.TradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingSettingService {

    private final TradingSettingRepository tradingSettingRepository;

    @Transactional(readOnly = true)
    public TradingSettingDTO getTradingSetting(String userId) {
        TradingSetting setting = tradingSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("거래 설정을 찾을 수 없습니다"));

        return convertToDTO(setting);
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
                .dailyLimitAmount(dto.getDailyLimitAmount())
                .useAiAnalysis(dto.getUseAiAnalysis())
                .useTrailingStop(dto.getUseTrailingStop())
                .trailingStopPct(dto.getTrailingStopPct())
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
        setting.setDailyLimitAmount(dto.getDailyLimitAmount());
        setting.setUseAiAnalysis(dto.getUseAiAnalysis());
        setting.setUseTrailingStop(dto.getUseTrailingStop());
        setting.setTrailingStopPct(dto.getTrailingStopPct());

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
                .dailyLimitAmount(setting.getDailyLimitAmount())
                .useAiAnalysis(setting.getUseAiAnalysis())
                .useTrailingStop(setting.getUseTrailingStop())
                .trailingStopPct(setting.getTrailingStopPct())
                .build();
    }
}