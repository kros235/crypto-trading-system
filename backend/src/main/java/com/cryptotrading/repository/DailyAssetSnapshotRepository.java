package com.cryptotrading.repository;

import com.cryptotrading.entity.DailyAssetSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAssetSnapshotRepository extends JpaRepository<DailyAssetSnapshot, Long> {

    /** 특정 사용자의 특정 날짜 스냅샷 조회 (스냅샷 저장 시 UPSERT용) */
    Optional<DailyAssetSnapshot> findByUserIdAndSnapshotDate(String userId, LocalDate snapshotDate);

    /** 특정 사용자의 기간별 스냅샷 목록 조회 - 차트 데이터용 (날짜 오름차순) */
    @Query("SELECT s FROM DailyAssetSnapshot s WHERE s.userId = :userId " +
           "AND s.snapshotDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.snapshotDate ASC")
    List<DailyAssetSnapshot> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 특정 사용자의 전체 스냅샷 목록 - 전체 투자기간 차트용 (날짜 오름차순) */
    List<DailyAssetSnapshot> findByUserIdOrderBySnapshotDateAsc(String userId);

    /** 특정 사용자의 가장 최근 스냅샷 - 이전 불입금액 조회용 */
    Optional<DailyAssetSnapshot> findTopByUserIdOrderBySnapshotDateDesc(String userId);

    /** 특정 사용자의 특정 날짜 스냅샷 존재 여부 - 마이그레이션 시 중복 방지용 */
    boolean existsByUserIdAndSnapshotDate(String userId, LocalDate snapshotDate);
}