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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

import com.cryptotrading.dto.upbit.UpbitDepositDTO; // ⭐ [추가] 입금 내역 DTO
import com.cryptotrading.dto.upbit.UpbitWithdrawDTO; // ⭐ [추가] 출금 내역 DTO

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAssetSnapshotService {

    private final DailyAssetSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RiskManagementService riskManagementService;
    private final UpbitApiService upbitApiService; // ⭐ [추가] 입출금 내역 조회
    private final UserService userService; // ⭐ [추가] API 키 복호화

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 특정 사용자의 오늘 자산 스냅샷 생성/갱신
     * - 업비트 API로 현재 총자산(평가금액) 조회
     * - 이전 스냅샷에서 불입금액 이어받기
     * - 수익 금액/수익률 자동 계산
     */
    // ⭐⭐⭐ [버그 수정] REQUIRED → REQUIRES_NEW ⭐⭐⭐
    // 왜: 이 메서드가 호출자(예: TransactionService.sellTransaction())와 같은 물리 트랜잭션을
    //     공유하고 있었음. 내부에서 호출하는 UserService.getDecryptedApiKeys()도 @Transactional이라,
    //     API 키가 없는 사용자(테스트 계정 등)에서 예외가 나면 이 메서드 자체는 try/catch로
    //     삼켜서 정상 종료하더라도, 공유 중인 물리 트랜잭션에는 이미 "rollback-only" 표시가
    //     남아버림 → 호출자(sellTransaction 등)가 커밋하려는 순간 UnexpectedRollbackException 발생
    //     → 스냅샷과 무관한 매도 자체가 통째로 롤백되는 문제.
    //     REQUIRES_NEW로 바꾸면 이 메서드는 독립된 새 물리 트랜잭션에서 실행되므로,
    //     내부에서 무슨 예외가 나든 호출자의 트랜잭션에는 전혀 영향을 주지 않음.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDailySnapshot(String userId) {
        LocalDate today = LocalDate.now(KST);

        try {
            // ⭐⭐⭐ [변경] 캐시 우회 → 업비트 API 직접 호출로 최신 평가금액 조회 ⭐⭐⭐
            // 왜: getDailyTotalAssetSnapshot()은 일일 한도 계산의 안정성을 위해 하루 중 첫 호출 값을 캐시함.
            //     그러나 23:59 스냅샷 저장 시에는 "현재 시점의 정확한 평가금액"이 필요.
            //     캐시된 값은 당일 새벽/아침에 봇이 호출한 시점의 값이므로,
            //     하루 중 발생한 입금/출금/코인 가격 변동이 반영되지 않아 실제 잔고와 차이 발생.
            BigDecimal totalAsset = riskManagementService.fetchTotalAssetFromUpbit(userId);
            if (totalAsset == null || totalAsset.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("총자산 조회 실패 또는 0원 - userId: {}", userId);
                return;
            }

            // ⭐⭐⭐ [변경] 업비트 입출금 내역 기반으로 불입금액 계산 ⭐⭐⭐
            // 왜: 기존에는 이전 스냅샷 값을 복사만 하여 예치금 이용료 등 실제 입금/출금이 반영되지 않았음
            // 변경: 업비트 API로 KRW 입금 내역(예치금 이용료 포함) - KRW 출금 내역을 계산하여 정확한 불입금액 산출

            // ⭐ [삭제된 로직] 이전에 거래 내역이 없으면 스냅샷 자동 생성을 건너뛰도록 했으나,
            // 실 거래가 없어도(관망 중이어도) 매일매일의 자산 변동 추이를 차트에 그리기 위해 제거함.
            // int txCount = transactionRepository.countByUserId(userId);
            // if (txCount == 0) {
            // log.info("거래 내역 없음, 스냅샷 생성 건너뜀 - userId: {}", userId);
            // return;
            // }

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
     * ⭐⭐⭐ [수정] @Transactional 제거 ⭐⭐⭐
     * 수정 이유: @Transactional이 걸린 상태에서 일부 사용자(API 키 없는 admin 등)에서
     * 예외가 발생하면, 이미 정상 저장된 다른 사용자의 스냅샷까지 전부 롤백됨.
     * 각 사용자의 createDailySnapshot()은 자체 @Transactional을 갖고 있으므로
     * 상위 트랜잭션 없이 독립적으로 커밋/롤백 처리됨.
     */
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

        // ⭐ 기존 스냅샷이 있으면 초기 불입금액만 덮어쓰기 (23:59 스케줄러가 먼저 만든 경우 대비)
        // 왜: 거래 0건인 사용자도 23:59 스케줄러에서 스냅샷이 생성될 수 있고,
        // 이때 폴백 기본값(1,000,000원)이 들어갈 수 있음.
        // 첫 매수 직전 실제 KRW 잔고가 가장 정확하므로 덮어쓰기해야 함.
        Optional<DailyAssetSnapshot> existing = snapshotRepository
                .findByUserIdAndSnapshotDate(userId, today);

        if (existing.isPresent()) {
            DailyAssetSnapshot snapshot = existing.get();
            snapshot.setDepositAmount(initialKrwBalance);
            snapshot.setKrwBalance(initialKrwBalance);
            // 평가금액이 있으면 수익 재계산
            if (snapshot.getEvaluationAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profitAmount = snapshot.getEvaluationAmount().subtract(initialKrwBalance);
                BigDecimal profitRate = initialKrwBalance.compareTo(BigDecimal.ZERO) > 0
                        ? profitAmount.divide(initialKrwBalance, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;
                snapshot.setProfitAmount(profitAmount);
                snapshot.setProfitRate(profitRate);
            }
            snapshotRepository.save(snapshot);
            log.info("🎯 초기 불입금액 스냅샷 갱신 - userId: {}, KRW 잔고: {}원 (기존 스냅샷 덮어쓰기)", userId, initialKrwBalance);
        } else {
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
            log.info("🎯 초기 불입금액 스냅샷 저장 - userId: {}, KRW 잔고: {}원 (신규 생성)", userId, initialKrwBalance);
        }
    }

    /**
     * 기간별 스냅샷 조회 (프론트엔드 차트용)
     * 중간에 빠진 날짜(Gap)가 있으면 전일 데이터를 복사하여 차트가 끊기지 않도록 보완.
     * 
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
                List<DailyAssetSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId);
                if (allSnapshots.isEmpty())
                    return Collections.emptyList();
                startDate = allSnapshots.get(0).getSnapshotDate();
                break;
        }

        List<DailyAssetSnapshot> rawSnapshots = snapshotRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        return fillSnapshotGaps(rawSnapshots, startDate, endDate);
    }

    /**
     * 사용자 지정 기간 스냅샷 조회 (Gap 채우기 포함)
     */
    public List<DailyAssetSnapshotDTO> getSnapshotsByCustomRange(
            String userId, LocalDate startDate, LocalDate endDate) {
        List<DailyAssetSnapshot> rawSnapshots = snapshotRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        return fillSnapshotGaps(rawSnapshots, startDate, endDate);
    }

    /**
     * 누락된 날짜(Gap)를 전일 데이터로 채우는 유틸리티 메서드
     */
    private List<DailyAssetSnapshotDTO> fillSnapshotGaps(List<DailyAssetSnapshot> rawSnapshots, LocalDate start,
            LocalDate end) {
        if (rawSnapshots.isEmpty())
            return Collections.emptyList();

        Map<LocalDate, DailyAssetSnapshotDTO> map = new HashMap<>();
        for (DailyAssetSnapshot s : rawSnapshots) {
            map.put(s.getSnapshotDate(), toDTO(s));
        }

        List<DailyAssetSnapshotDTO> result = new ArrayList<>();
        DailyAssetSnapshotDTO lastKnown = null;

        // start 날짜부터 end 날짜까지 순회
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (map.containsKey(d)) {
                lastKnown = map.get(d);
                result.add(lastKnown);
            } else if (lastKnown != null) {
                // 데이터 없는 날 = 직전 데이터 복사본 생성 (날짜만 변경)
                DailyAssetSnapshotDTO filled = DailyAssetSnapshotDTO.builder()
                        .date(d.toString())
                        .evaluationAmount(lastKnown.getEvaluationAmount())
                        .depositAmount(lastKnown.getDepositAmount())
                        .profitAmount(lastKnown.getProfitAmount())
                        .profitRate(lastKnown.getProfitRate())
                        .krwBalance(lastKnown.getKrwBalance())
                        .coinEvaluation(lastKnown.getCoinEvaluation())
                        .build();
                result.add(filled);
            }
        }
        return result;
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

            /// 2. 시작 기준점(시간) 잡기
            // 거래(Transaction) 내역이 있는지 확인. 없다면 가장 오래된 AssetSnapshot 생성 일을 기준으로 삼음.
            LocalDateTime referenceTime;
            LocalDate referenceDate;

            Optional<Transaction> firstTx = transactionRepository.findTopByUserIdOrderByCreatedAtAsc(userId);
            if (firstTx.isPresent()) {
                referenceTime = firstTx.get().getCreatedAt();
                referenceDate = referenceTime.toLocalDate();
            } else {
                // 거래 내역이 없으면 가장 오래된 스냅샷부터 입출금 추적
                List<DailyAssetSnapshot> allSnapshots = snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId);
                if (!allSnapshots.isEmpty()) {
                    referenceDate = allSnapshots.get(0).getSnapshotDate();
                    referenceTime = referenceDate.atStartOfDay();
                } else {
                    // 스냅샷도 없고 거래 내역도 없으면 입출금을 추적할 기준점이 없음
                    log.warn("거래 내역 및 스냅샷 없음 - userId: {}", userId);
                    return BigDecimal.ZERO;
                }
            }

            // 업비트 API created_at 형식("2026-01-30T09:00:00+09:00")과 비교하기 위해 KST offset 형식으로
            // 변환
            String referenceTimeStr = referenceTime.atZone(KST)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            log.info("불입금액 계산 기준점 - userId: {}, 시간: {}", userId, referenceTimeStr);

            // 3. 업비트 KRW 입출금 내역 전체 조회
            List<UpbitDepositDTO> deposits = upbitApiService.getAllKrwDeposits(apiKeys[0], apiKeys[1]);
            List<UpbitWithdrawDTO> withdraws = upbitApiService.getAllKrwWithdraws(apiKeys[0], apiKeys[1]);

            // ⭐⭐⭐ [신규 추가] 기준일 스냅샷에 초기 불입금액이 저장되어 있으면 사용 ⭐⭐⭐
            Optional<DailyAssetSnapshot> firstDaySnapshot = snapshotRepository
                    .findByUserIdAndSnapshotDate(userId, referenceDate);
            if (firstDaySnapshot.isPresent()) {
                BigDecimal savedInitial = firstDaySnapshot.get().getDepositAmount();
                if (savedInitial.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal depositsAfterTrade = deposits.stream()
                            // 예치금 이용료가 DONE 등 다른 상태코드일 수 있으므로 포괄수용 하도록 조건 완화 (혹은 ACCEPTED/DONE 확인)
                            .filter(d -> "ACCEPTED".equals(d.getState()) || "DONE".equals(d.getState()))
                            .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().compareTo(referenceTimeStr) >= 0)
                            .map(UpbitDepositDTO::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal withdrawsAfterTrade = withdraws.stream()
                            .filter(w -> "DONE".equals(w.getState()))
                            .filter(w -> w.getCreatedAt() != null && w.getCreatedAt().compareTo(referenceTimeStr) >= 0)
                            .map(UpbitWithdrawDTO::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal currentDeposit = savedInitial.add(depositsAfterTrade).subtract(withdrawsAfterTrade);
                    log.info("불입금액 계산 (스냅샷 기준) - userId: {}, 초기: {}원, 이후입금: {}원, 이후출금: {}원, 현재: {}원",
                            userId, savedInitial, depositsAfterTrade, withdrawsAfterTrade, currentDeposit);
                    return currentDeposit;
                }
            }

            // 4. 기준일 이전의 입출금 누적합 = 초기 불입금액 (스냅샷이 없는 경우 폴백)
            BigDecimal depositsBefore = deposits.stream()
                    .filter(d -> "ACCEPTED".equals(d.getState()) || "DONE".equals(d.getState()))
                    .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().compareTo(referenceTimeStr) < 0)
                    .map(UpbitDepositDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal withdrawsBefore = withdraws.stream()
                    .filter(w -> "DONE".equals(w.getState()))
                    .filter(w -> w.getCreatedAt() != null && w.getCreatedAt().compareTo(referenceTimeStr) < 0)
                    .map(UpbitWithdrawDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal initialDeposit = depositsBefore.subtract(withdrawsBefore);

            // 5. 기준일 이후의 입출금 가감
            BigDecimal depositsAfter = deposits.stream()
                    .filter(d -> "ACCEPTED".equals(d.getState()) || "DONE".equals(d.getState()))
                    .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().compareTo(referenceTimeStr) >= 0)
                    .map(UpbitDepositDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal withdrawsAfter = withdraws.stream()
                    .filter(w -> "DONE".equals(w.getState()))
                    .filter(w -> w.getCreatedAt() != null && w.getCreatedAt().compareTo(referenceTimeStr) >= 0)
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