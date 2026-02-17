package com.cryptotrading.service;

import com.cryptotrading.dto.profit.DailyAssetSnapshotDTO;
import com.cryptotrading.entity.DailyAssetSnapshot;
import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.Transaction.TransactionStatus;
import com.cryptotrading.entity.User;
import com.cryptotrading.repository.DailyAssetSnapshotRepository;
import com.cryptotrading.repository.TransactionRepository;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import com.cryptotrading.dto.upbit.UpbitDepositDTO;    // ⭐ [추가] 입금 내역 DTO
import com.cryptotrading.dto.upbit.UpbitWithdrawDTO;   // ⭐ [추가] 출금 내역 DTO



@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAssetSnapshotService {

    private final DailyAssetSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RiskManagementService riskManagementService;
    private final UpbitApiService upbitApiService;       // ⭐ [추가] 입출금 내역 조회
    private final UserService userService;               // ⭐ [추가] API 키 복호화

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 특정 사용자의 오늘 자산 스냅샷 생성/갱신
     * - 업비트 API로 현재 총자산(평가금액) 조회
     * - 이전 스냅샷에서 불입금액 이어받기
     * - 수익 금액/수익률 자동 계산
     */
    @Transactional
    public void createDailySnapshot(String userId) {
        LocalDate today = LocalDate.now(KST);

        try {
            // 1. 업비트에서 현재 총자산(평가금액) 조회
            BigDecimal totalAsset = riskManagementService.getDailyTotalAssetSnapshot(userId);
            if (totalAsset == null || totalAsset.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("총자산 조회 실패 또는 0원 - userId: {}", userId);
                return;
            }

            // ⭐⭐⭐ [변경] 업비트 입출금 내역 기반으로 불입금액 계산 ⭐⭐⭐
            // 왜: 기존에는 이전 스냅샷 값을 복사만 하여 예치금 이용료 등 실제 입금/출금이 반영되지 않았음
            // 변경: 업비트 API로 KRW 입금 내역(예치금 이용료 포함) - KRW 출금 내역을 계산하여 정확한 불입금액 산출
            BigDecimal depositAmount = calculateNetDepositFromUpbit(userId);
            if (depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
                // 업비트 API 조회 실패 시 이전 스냅샷 값으로 폴백
                depositAmount = getLatestDepositAmount(userId);
                log.warn("업비트 입출금 내역 조회 실패, 이전 스냅샷 불입금액 사용: {}원", depositAmount);
            }

            // 3. 수익 계산
            BigDecimal profitAmount = totalAsset.subtract(depositAmount);
            BigDecimal profitRate = depositAmount.compareTo(BigDecimal.ZERO) > 0
                    ? profitAmount.divide(depositAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            // 4. 스냅샷 저장 (이미 있으면 갱신, 없으면 신규 생성)
            DailyAssetSnapshot snapshot = snapshotRepository
                    .findByUserIdAndSnapshotDate(userId, today)
                    .orElse(DailyAssetSnapshot.builder()
                            .userId(userId)
                            .snapshotDate(today)
                            .build());

            snapshot.setEvaluationAmount(totalAsset);
            snapshot.setDepositAmount(depositAmount);
            snapshot.setKrwBalance(BigDecimal.ZERO);
            snapshot.setCoinEvaluation(BigDecimal.ZERO);
            snapshot.setProfitAmount(profitAmount);
            snapshot.setProfitRate(profitRate);

            snapshotRepository.save(snapshot);

            log.info("일별 자산 스냅샷 저장 - userId: {}, date: {}, 평가: {}원, 불입: {}원, 수익: {}원 ({}%)",
                    userId, today, totalAsset, depositAmount, profitAmount, profitRate);

        } catch (Exception e) {
            log.error("일별 자산 스냅샷 생성 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 모든 활성 사용자의 스냅샷 생성 (스케줄러에서 호출)
     */
    @Transactional
    public void createAllUsersSnapshot() {
        List<User> activeUsers = userRepository.findByIsActive(true);
        log.info("=== 일별 자산 스냅샷 생성 시작: 활성 사용자 {}명 ===", activeUsers.size());

        for (User user : activeUsers) {
            try {
                createDailySnapshot(user.getUserId());
            } catch (Exception e) {
                log.error("사용자 스냅샷 생성 실패 - userId: {}", user.getUserId(), e);
            }
        }

        log.info("=== 일별 자산 스냅샷 생성 완료 ===");
    }

    /**
     * 불입금액 수동 설정
     * - 사용자가 입금/출금 시 불입금액을 직접 업데이트
     * - 오늘 스냅샷이 있으면 수익 재계산
     */
    @Transactional
    public void updateDepositAmount(String userId, BigDecimal depositAmount) {
        LocalDate today = LocalDate.now(KST);

        DailyAssetSnapshot snapshot = snapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .orElse(DailyAssetSnapshot.builder()
                        .userId(userId)
                        .snapshotDate(today)
                        .evaluationAmount(BigDecimal.ZERO)
                        .krwBalance(BigDecimal.ZERO)
                        .coinEvaluation(BigDecimal.ZERO)
                        .profitAmount(BigDecimal.ZERO)
                        .profitRate(BigDecimal.ZERO)
                        .build());

        snapshot.setDepositAmount(depositAmount);

        // 평가금액이 있으면 수익 재계산
        if (snapshot.getEvaluationAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitAmount = snapshot.getEvaluationAmount().subtract(depositAmount);
            BigDecimal profitRate = depositAmount.compareTo(BigDecimal.ZERO) > 0
                    ? profitAmount.divide(depositAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            snapshot.setProfitAmount(profitAmount);
            snapshot.setProfitRate(profitRate);
        }

        snapshotRepository.save(snapshot);
        log.info("불입금액 업데이트 - userId: {}, depositAmount: {}원", userId, depositAmount);
    }

    /**
     * 기간별 스냅샷 조회 (프론트엔드 차트용)
     * @param period "7" / "month" / "year" / "all"
     */
    public List<DailyAssetSnapshotDTO> getSnapshots(String userId, String period) {
        LocalDate endDate = LocalDate.now(KST);
        LocalDate startDate;

        switch (period) {
            case "7":
                startDate = endDate.minusDays(7);
                break;
            case "month":
                startDate = endDate.withDayOfMonth(1);
                break;
            case "year":
                startDate = endDate.withDayOfYear(1);
                break;
            case "all":
            default:
                return snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId)
                        .stream().map(this::toDTO).collect(Collectors.toList());
        }

        return snapshotRepository.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 사용자 지정 기간 스냅샷 조회
     */
    public List<DailyAssetSnapshotDTO> getSnapshotsByCustomRange(
            String userId, LocalDate startDate, LocalDate endDate) {
        return snapshotRepository.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 과거 스냅샷 마이그레이션 (기존 거래 이력 기반)
     * - 기존 SOLD 거래의 profitLoss를 누적하여 과거 스냅샷 생성
     * - 이미 존재하는 날짜는 건너뜀
     */
    @Transactional
    public void migrateHistoricalSnapshots(String userId, BigDecimal initialDeposit) {
        List<Transaction> soldTxs = transactionRepository
                .findByUserIdAndStatus(userId, TransactionStatus.SOLD);

        if (soldTxs.isEmpty()) {
            log.info("마이그레이션 대상 거래 없음 - userId: {}", userId);
            return;
        }

        // 날짜 오름차순 정렬
        soldTxs.sort((a, b) -> {
            LocalDate dateA = (a.getSoldAt() != null ? a.getSoldAt() : a.getCreatedAt()).toLocalDate();
            LocalDate dateB = (b.getSoldAt() != null ? b.getSoldAt() : b.getCreatedAt()).toLocalDate();
            return dateA.compareTo(dateB);
        });

        // 날짜별 누적 잔액 계산
        BigDecimal runningBalance = initialDeposit;
        Map<LocalDate, BigDecimal> dailyBalance = new TreeMap<>();

        for (Transaction tx : soldTxs) {
            LocalDate txDate = (tx.getSoldAt() != null ? tx.getSoldAt() : tx.getCreatedAt()).toLocalDate();
            if (tx.getProfitLoss() != null) {
                runningBalance = runningBalance.add(tx.getProfitLoss());
            }
            dailyBalance.put(txDate, runningBalance);
        }

        // 모든 날짜에 대해 스냅샷 생성
        LocalDate firstDate = dailyBalance.keySet().iterator().next();
        LocalDate today = LocalDate.now(KST);
        BigDecimal currentBalance = initialDeposit;

        int count = 0;
        for (LocalDate date = firstDate; !date.isAfter(today); date = date.plusDays(1)) {
            if (dailyBalance.containsKey(date)) {
                currentBalance = dailyBalance.get(date);
            }

            if (!snapshotRepository.existsByUserIdAndSnapshotDate(userId, date)) {
                BigDecimal profitAmount = currentBalance.subtract(initialDeposit);
                BigDecimal profitRate = initialDeposit.compareTo(BigDecimal.ZERO) > 0
                        ? profitAmount.divide(initialDeposit, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;

                snapshotRepository.save(DailyAssetSnapshot.builder()
                        .userId(userId)
                        .snapshotDate(date)
                        .evaluationAmount(currentBalance)
                        .depositAmount(initialDeposit)
                        .krwBalance(BigDecimal.ZERO)
                        .coinEvaluation(BigDecimal.ZERO)
                        .profitAmount(profitAmount)
                        .profitRate(profitRate)
                        .build());
                count++;
            }
        }

        log.info("과거 스냅샷 마이그레이션 완료 - userId: {}, 생성: {}건, 기간: {} ~ {}",
                userId, count, firstDate, today);
    }

    // ===== Private Helper =====

    // ⭐⭐⭐ [신규 추가] 업비트 입출금 내역 기반 순 불입금액 계산 ⭐⭐⭐
    // 왜: 예치금 이용료를 포함한 실제 KRW 입금 총액에서 출금 총액을 차감하여 정확한 불입금액 산출
    // 공식: 순 불입금액 = 총 KRW 입금액(일반입금 + 예치금이용료) - 총 KRW 출금액
    /**
     * 업비트 KRW 입출금 내역에서 순 불입금액 계산
     * - 입금: 일반 입금 + 예치금 이용료 (GET /v1/deposits, state=ACCEPTED)
     * - 출금: 완료된 출금 (GET /v1/withdraws, state=DONE)
     * - API 호출 실패 시 BigDecimal.ZERO 반환 (호출자에서 폴백 처리)
     *
     * @param userId 사용자 ID
     * @return 순 불입금액 (입금 - 출금), 실패 시 BigDecimal.ZERO
     */
    private BigDecimal calculateNetDepositFromUpbit(String userId) {
        try {
            // 1. 사용자 API 키 조회 및 복호화
            String[] apiKeys = userService.getDecryptedApiKeys(userId);
            if (apiKeys == null || apiKeys.length < 2) {
                log.warn("사용자 API 키 없음 - userId: {}", userId);
                return BigDecimal.ZERO;
            }

            // 2. 업비트 KRW 입금 내역 전체 조회
            List<UpbitDepositDTO> deposits = upbitApiService.getAllKrwDeposits(apiKeys[0], apiKeys[1]);

            // 3. 업비트 KRW 출금 내역 전체 조회
            List<UpbitWithdrawDTO> withdraws = upbitApiService.getAllKrwWithdraws(apiKeys[0], apiKeys[1]);

            // 4. 완료된 입금 금액 합산 (예치금 이용료 포함)
            BigDecimal totalDeposit = deposits.stream()
                    .filter(d -> "ACCEPTED".equals(d.getState()))
                    .map(UpbitDepositDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 5. 완료된 출금 금액 합산
            BigDecimal totalWithdraw = withdraws.stream()
                    .filter(w -> "DONE".equals(w.getState()))
                    .map(UpbitWithdrawDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 6. 순 불입금액 = 입금 - 출금
            BigDecimal netDeposit = totalDeposit.subtract(totalWithdraw);

            log.info("업비트 KRW 입출금 계산 - userId: {}, 입금: {}원({}건), 출금: {}원({}건), 순 불입금액: {}원",
                    userId, totalDeposit, deposits.size(), totalWithdraw, withdraws.size(), netDeposit);

            return netDeposit;

        } catch (Exception e) {
            log.error("업비트 입출금 내역 기반 불입금액 계산 실패 - userId: {}, error: {}",
                    userId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 가장 최근 스냅샷의 불입금액 조회 (없으면 기본값 1,000,000원)
     */
    private BigDecimal getLatestDepositAmount(String userId) {
        return snapshotRepository.findTopByUserIdOrderBySnapshotDateDesc(userId)
                .map(DailyAssetSnapshot::getDepositAmount)
                .orElse(new BigDecimal("1000000"));
    }

    /**
     * Entity → DTO 변환
     */
    private DailyAssetSnapshotDTO toDTO(DailyAssetSnapshot entity) {
        return DailyAssetSnapshotDTO.builder()
                .date(entity.getSnapshotDate().toString())
                .evaluationAmount(entity.getEvaluationAmount())
                .depositAmount(entity.getDepositAmount())
                .profitAmount(entity.getProfitAmount())
                .profitRate(entity.getProfitRate())
                .krwBalance(entity.getKrwBalance())
                .coinEvaluation(entity.getCoinEvaluation())
                .build();
    }
}