package com.cryptotrading.service;

import com.cryptotrading.entity.CoinInfo;
import com.cryptotrading.entity.TradingSetting;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.CoinInfoRepository;
import com.cryptotrading.repository.TradingSettingRepository;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ⭐⭐⭐ [신규] Top10 자동 운영 서비스 ⭐⭐⭐
 * - 매일 04:00 KST, 시가총액 순위 갱신 직후 CoinInfoService에서 호출됨
 * - use_top10_auto_rebalance = true 인 사용자만 대상
 * - coinSymbols(거래 종목)만 변경하고, 나머지 거래 설정(매수/매도 조건 등)은 절대 건드리지 않음
 * - 편출된 종목의 기존 보유 포지션은 강제 매도하지 않음 (매도 로직은 coinSymbols와 무관하게
 *   HOLDING 상태 트랜잭션 기준으로 동작하므로 별도 처리 불필요)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Top10RebalanceService {

    private final CoinInfoRepository coinInfoRepository;
    private final TradingSettingRepository tradingSettingRepository;
    private final UserRepository userRepository;
    private final DiscordBotService discordBotService;
    private final EmailService emailService;

    @Transactional
    public void rebalanceAllUsers() {
        log.info("========== Top10 자동 리밸런싱 시작 ==========");

        // 1. 활성 코인 중 market_cap_rank 순 정렬 후 상위 10개 심볼 추출
        List<CoinInfo> activeCoins = coinInfoRepository.findByIsActiveOrderByMarketCapRank(true);
        List<String> top10 = activeCoins.stream()
                .filter(c -> c.getMarketCapRank() != null)
                .limit(10)
                .map(CoinInfo::getSymbol)
                .collect(Collectors.toList());

        if (top10.isEmpty()) {
            log.warn("Top10 리밸런싱 스킵: 시가총액 순위 데이터가 없습니다");
            return;
        }

        // 2. Top10 자동 운영을 켠 사용자만 조회
        List<TradingSetting> targets = tradingSettingRepository.findByUseTop10AutoRebalanceTrue();
        log.info("Top10 자동 운영 대상 사용자: {}명", targets.size());

        for (TradingSetting setting : targets) {
            try {
                rebalanceOne(setting, top10);
            } catch (Exception e) {
                // 한 사용자 실패가 다른 사용자 처리에 영향 주지 않도록 개별 격리
                log.error("Top10 리밸런싱 실패: userId={}, error={}", setting.getUserId(), e.getMessage());
            }
        }

        log.info("========== Top10 자동 리밸런싱 완료 ==========");
    }

    private void rebalanceOne(TradingSetting setting, List<String> top10) {
        List<String> before = setting.getCoinSymbols() != null ? setting.getCoinSymbols() : List.of();

        // 변경 없으면 저장/알림 스킵 (불필요한 updated_at 갱신 방지)
        if (new HashSet<>(before).equals(new HashSet<>(top10))) {
            return;
        }

        List<String> added = top10.stream().filter(s -> !before.contains(s)).collect(Collectors.toList());
        List<String> removed = before.stream().filter(s -> !top10.contains(s)).collect(Collectors.toList());

        // ⭐ coinSymbols만 교체, 나머지 필드는 전혀 손대지 않음
        setting.setCoinSymbols(top10);
        tradingSettingRepository.save(setting);

        log.info("Top10 리밸런싱 완료: userId={}, 편입={}, 편출={}", setting.getUserId(), added, removed);

        notifyRebalance(setting.getUserId(), added, removed);
    }

    private void notifyRebalance(String userId, List<String> added, List<String> removed) {
        String subject = "🔄 Top10 코인 자동 편입/편출 안내";
        StringBuilder msg = new StringBuilder();
        msg.append("시가총액 상위 10개 코인 기준으로 거래 종목이 자동 갱신되었습니다.\n\n");
        if (!added.isEmpty()) {
            msg.append("✅ 편입: ").append(String.join(", ", added)).append("\n");
        }
        if (!removed.isEmpty()) {
            msg.append("❌ 편출: ").append(String.join(", ", removed)).append("\n\n");
            msg.append("⚠️ 편출된 종목의 기존 보유분은 강제 매도되지 않습니다. ");
            msg.append("목표수익/손절 조건 충족 시에만 자동 매도되며, 재편입 전까지 신규 매수만 중단됩니다.");
        }

        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) return;

        if (user.getDiscordUserId() != null && !user.getDiscordUserId().isBlank()) {
            discordBotService.sendSystemAlertDM(user.getDiscordUserId(), subject, msg.toString());
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendSystemAlert(user.getEmail(), subject, msg.toString().replace("\n", "<br/>"));
        }
    }
}