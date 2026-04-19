package com.cryptotrading.service;

import com.cryptotrading.entity.StockAssetSnapshot;
import com.cryptotrading.entity.StockTransaction;
import com.cryptotrading.entity.TransactionStatus;
import com.cryptotrading.repository.StockAssetSnapshotRepository;
import com.cryptotrading.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * ⭐ [수정 Q6] 주식 자산 스냅샷 서비스
 * 코인의 AssetSnapshotService와 동일한 패턴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAssetSnapshotService {

    private final StockAssetSnapshotRepository snapshotRepository;
    private final StockTransactionRepository stockTransactionRepository;

    /**
     * 특정 사용자의 스냅샷 생성/갱신
     * 매도 완료 거래 기반으로 평가금액 계산
     */
    @Transactional
    public void createOrUpdateSnapshot(String userId) {
        try {
            // 초기 자산 (기본값 1,000,000)
            BigDecimal initialAsset = new BigDecimal("1000000");

            // 전체 매도 거래로 누적 수익 계산
            List<StockTransaction> soldTransactions = stockTransactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.SOLD);

            BigDecimal totalProfit = soldTransactions.stream()
                .filter(tx -> tx.getProfitLoss() != null)
                .map(StockTransaction::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal evaluationAmount = initialAsset.add(totalProfit);
            BigDecimal depositAmount = initialAsset;
            BigDecimal profitRate = initialAsset.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(initialAsset, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

            LocalDate today = LocalDate.now();

            // UPSERT (있으면 갱신, 없으면 생성)
            StockAssetSnapshot snapshot = snapshotRepository
                .findByUserIdAndDate(userId, today)
                .orElse(StockAssetSnapshot.builder()
                    .userId(userId)
                    .date(today)
                    .build());

            snapshot.setEvaluationAmount(evaluationAmount);
            snapshot.setDepositAmount(depositAmount);
            snapshot.setProfitAmount(totalProfit);
            snapshot.setProfitRate(profitRate);

            snapshotRepository.save(snapshot);
            log.info("[StockSnapshot] userId={} date={} eval={}", userId, today, evaluationAmount);

        } catch (Exception e) {
            log.error("[StockSnapshot] 스냅샷 생성 실패 userId={}", userId, e);
        }
    }

    /**
     * 기간별 스냅샷 조회
     */
    public List<StockAssetSnapshot> getSnapshots(String userId, String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "7":    startDate = today.minusDays(7); break;
            case "month": startDate = today.withDayOfMonth(1); break;
            case "year":  startDate = today.withDayOfYear(1); break;
            default:      startDate = LocalDate.of(2020, 1, 1); break;
        }

        return snapshotRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, today);
    }

    /**
     * 사용자 지정 기간 스냅샷 조회
     */
    public List<StockAssetSnapshot> getSnapshotsByRange(String userId, LocalDate start, LocalDate end) {
        return snapshotRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end);
    }
}