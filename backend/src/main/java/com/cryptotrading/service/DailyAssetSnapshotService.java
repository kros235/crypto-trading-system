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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional; 

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
     * ⭐ [신규 추가] 초기 불입금액 저장 (첫 매수 직전 KRW 잔고)
     * - 첫 자동매매 시작 시 호출
     * - 이후 불입금액 계산의 기준점으로 사용
     */
    @Transactional
    public void saveInitialDeposit(String userId, BigDecimal initialKrwBalance) {
        LocalDate today = LocalDate.now(KST);

        // 이미 스냅샷이 있으면 저장하지 않음 (중복 방지)
        if (snapshotRepository.existsByUserIdAndSnapshotDate(userId, today)) {
            log.info("초기 불입금액 - 이미 오늘 스냅샷 존재: userId={}", userId);
            return;
        }

        DailyAssetSnapshot snapshot = DailyAssetSnapshot.builder()
                .userId(userId)
                .snapshotDate(today)
                .evaluationAmount(initialKrwBalance)
                .depositAmount(initialKrwBalance)
                .krwBalance(initialKrwBalance)
                .coinEvaluation(BigDecimal.ZERO)
                .profitAmount(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();

        snapshotRepository.save(snapshot);
        log.info("🎯 초기 불입금액 스냅샷 저장 - userId: {}, KRW 잔고: {}원", userId, initialKrwBalance);
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

    /**
     * 업비트 KRW 입출금 내역에서 불입금액 계산
     * 
     * [로직]
     * 1단계: 전체 입출금 내역을 시간순 정렬
     * 2단계: 첫 자동매매 거래 시점 직전까지의 입출금 누적합 = 초기 불입금액
     * 3단계: 첫 거래 시점 이후의 입출금을 초기 불입금액에 가감 = 현재 불입금액
     *
     * [예시]
     * 1/1 입금 +100만원, 1/2 출금 -80만원, 1/3 입금 +5만원, 1/4 출금 -11만원, 1/5 입금 +1만원
     * 첫 거래: 1/5 01:00 AM
     * → 1/5 01:00 이전 누적: 100-80+5-11+1 = 15만원 (초기 불입금액)
     * → 1/5 이후 입출금을 15만원에 가감 = 현재 불입금액
     *
     * @param userId 사용자 ID
     * @return 불입금액, 실패 시 BigDecimal.ZERO
     */
    private BigDecimal calculateNetDepositFromUpbit(String userId) {
        try {
            // 1. 사용자 API 키 조회 및 복호화
            String[] apiKeys = userService.getDecryptedApiKeys(userId);
            if (apiKeys == null || apiKeys.length < 2) {
                log.warn("사용자 API 키 없음 - userId: {}", userId);
                return BigDecimal.ZERO;
            }

            /// 2. 첫 자동매매 거래 시점 조회
            Optional<Transaction> firstTx = transactionRepository.findTopByUserIdOrderByCreatedAtAsc(userId);
            if (firstTx.isEmpty()) {
                log.warn("거래 내역 없음 - userId: {}", userId);
                return BigDecimal.ZERO;
            }
            LocalDateTime firstTradeTime = firstTx.get().getCreatedAt();
            LocalDate firstTradeDate = firstTradeTime.toLocalDate();
            // 업비트 API created_at 형식("2026-01-30T09:00:00+09:00")과 비교하기 위해 KST offset 형식으로 변환
            String firstTradeTimeStr = firstTradeTime.atZone(KST)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            log.info("불입금액 계산 - userId: {}, 첫 거래 시점: {}", userId, firstTradeTimeStr);

            // 3. 업비트 KRW 입출금 내역 전체 조회
            List<UpbitDepositDTO> deposits = upbitApiService.getAllKrwDeposits(apiKeys[0], apiKeys[1]);
            List<UpbitWithdrawDTO> withdraws = upbitApiService.getAllKrwWithdraws(apiKeys[0], apiKeys[1]);

            // ⭐⭐⭐ [신규 추가] 첫 거래일 스냅샷에 초기 불입금액이 저장되어 있으면 사용 ⭐⭐⭐
            // 왜: 입출금 API로는 코인 거래 손익이 반영되지 않아 정확한 초기 자본을 알 수 없음
            //     첫 매수 직전 KRW 잔고(saveInitialDeposit)가 저장되어 있으면 그것이 가장 정확
            Optional<DailyAssetSnapshot> firstDaySnapshot = snapshotRepository
                    .findByUserIdAndSnapshotDate(userId, firstTradeDate);
            if (firstDaySnapshot.isPresent()) {
                BigDecimal savedInitial = firstDaySnapshot.get().getDepositAmount();
                if (savedInitial.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal depositsAfterTrade = deposits.stream()
                            .filter(d -> "ACCEPTED".equals(d.getState()))
                            .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().compareTo(firstTradeTimeStr) >= 0)
                            .map(UpbitDepositDTO::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal withdrawsAfterTrade = withdraws.stream()
                            .filter(w -> "DONE".equals(w.getState()))
                            .filter(w -> w.getCreatedAt() != null && w.getCreatedAt().compareTo(firstTradeTimeStr) >= 0)
                            .map(UpbitWithdrawDTO::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal currentDeposit = savedInitial.add(depositsAfterTrade).subtract(withdrawsAfterTrade);
                    log.info("불입금액 계산 (스냅샷 기준) - userId: {}, 초기: {}원, 이후입금: {}원, 이후출금: {}원, 현재: {}원",
                            userId, savedInitial, depositsAfterTrade, withdrawsAfterTrade, currentDeposit);
                    return currentDeposit;
                }
            }

            // 4. 첫 거래 시점 이전의 입출금 누적합 = 초기 불입금액 (스냅샷이 없는 경우 폴백)
            BigDecimal depositsBefore = deposits.stream()
                    .filter(d -> "ACCEPTED".equals(d.getState()))
                    .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().compareTo(firstTradeTimeStr) < 0)
                    .map(UpbitDepositDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal withdrawsBefore = withdraws.stream()
                    .filter(w -> "DONE".equals(w.getState()))
                    .filter(w -> w.getCreatedAt() != null && w.getCreatedAt().compareTo(firstTradeTimeStr) < 0)
                    .map(UpbitWithdrawDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal initialDeposit = depositsBefore.subtract(withdrawsBefore);

            // 5. 첫 거래 시점 이후의 입출금 가감
            BigDecimal depositsAfter = deposits.stream()
                    .filter(d -> "ACCEPTED".equals(d.getState()))
                    .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().compareTo(firstTradeTimeStr) >= 0)
                    .map(UpbitDepositDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal withdrawsAfter = withdraws.stream()
                    .filter(w -> "DONE".equals(w.getState()))
                    .filter(w -> w.getCreatedAt() != null && w.getCreatedAt().compareTo(firstTradeTimeStr) >= 0)
                    .map(UpbitWithdrawDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 6. 현재 불입금액 = 초기 불입금액 + 이후 입금 - 이후 출금
            BigDecimal currentDeposit = initialDeposit.add(depositsAfter).subtract(withdrawsAfter);

            log.info("불입금액 계산 결과 - userId: {}, 초기 불입금액: {}원, 이후 입금: {}원, 이후 출금: {}원, 현재 불입금액: {}원",
                    userId, initialDeposit, depositsAfter, withdrawsAfter, currentDeposit);

            return currentDeposit;

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