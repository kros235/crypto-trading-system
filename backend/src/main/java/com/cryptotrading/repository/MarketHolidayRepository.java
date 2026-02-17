package com.cryptotrading.repository;

import com.cryptotrading.entity.MarketHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, Long> {

    // 특정 날짜가 휴장일인지 확인
    boolean existsByHolidayDateAndMarket(LocalDate holidayDate, String market);

    // 연도별 휴장일 조회
    @Query("SELECT h FROM MarketHoliday h WHERE YEAR(h.holidayDate) = :year AND h.market = :market " +
           "ORDER BY h.holidayDate ASC")
    List<MarketHoliday> findByYearAndMarket(@Param("year") int year, @Param("market") String market);

    // 기간 내 휴장일 조회
    List<MarketHoliday> findByHolidayDateBetweenAndMarketOrderByHolidayDateAsc(
            LocalDate startDate, LocalDate endDate, String market);

    // 특정 날짜의 휴장일 조회
    Optional<MarketHoliday> findByHolidayDateAndMarket(LocalDate holidayDate, String market);

    // 다음 휴장일 조회
    @Query("SELECT h FROM MarketHoliday h WHERE h.holidayDate > :today AND h.market = :market " +
           "ORDER BY h.holidayDate ASC LIMIT 1")
    Optional<MarketHoliday> findNextHoliday(@Param("today") LocalDate today, @Param("market") String market);
}