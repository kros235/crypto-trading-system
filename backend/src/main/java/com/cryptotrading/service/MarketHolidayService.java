package com.cryptotrading.service;

import com.cryptotrading.entity.MarketHoliday;
import com.cryptotrading.repository.MarketHolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 한국 증시 휴장일 관리 서비스
 *
 * 휴장일 판단 순서:
 * 1. 주말(토/일) → 자동 휴장
 * 2. market_holiday 테이블 등록 여부 → DB 조회
 *
 * 휴장일 등록 방법:
 * - 관리자가 수동 등록 (AdminController 또는 직접 DB)
 * - 향후 공공데이터 API 연동 확장 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketHolidayService {

    private final MarketHolidayRepository marketHolidayRepository;

    // =========================================================
    // 1. 휴장일 판단
    // =========================================================

    /**
     * 특정 날짜가 휴장일인지 확인 (주말 포함)
     */
    @Cacheable(value = "marketHoliday", key = "#date.toString()")
    public boolean isHoliday(LocalDate date) {
        // 1. 주말 체크
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return true;
        }
        // 2. DB 휴장일 체크
        return marketHolidayRepository.existsByHolidayDate(date);
    }

    /**
     * 특정 날짜가 거래일인지 확인 (isHoliday 반대)
     */
    public boolean isTradingDay(LocalDate date) {
        return !isHoliday(date);
    }

    /**
     * 특정 월의 휴장일 목록 조회 (달력 표시용)
     */
    public List<MarketHoliday> getHolidaysByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return marketHolidayRepository.findByHolidayDateBetween(
                ym.atDay(1), ym.atEndOfMonth());
    }

    /**
     * 특정 기간 휴장일 목록 조회
     */
    public List<MarketHoliday> getHolidaysBetween(LocalDate from, LocalDate to) {
        return marketHolidayRepository.findByHolidayDateBetween(from, to);
    }

    /**
     * 오늘부터 N일 이내 휴장일 목록 (전일 알림용)
     */
    public List<MarketHoliday> getUpcomingHolidays(int days) {
        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusDays(days);
        return marketHolidayRepository.findByHolidayDateBetween(from, to);
    }

    // =========================================================
    // 2. 거래일 계산
    // =========================================================

    /**
     * 특정 날짜로부터 N 거래일 후 날짜 계산
     * (레버리지 ETF 보유기간 만료일 계산에 활용)
     */
    public LocalDate addTradingDays(LocalDate from, int tradingDays) {
        LocalDate date = from;
        int count = 0;
        while (count < tradingDays) {
            date = date.plusDays(1);
            if (isTradingDay(date)) {
                count++;
            }
        }
        return date;
    }

    /**
     * 두 날짜 사이의 거래일 수 계산
     * (보유기간 holdingDays 계산에 활용)
     */
    public int countTradingDays(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate date = from.plusDays(1); // from 당일 미포함
        while (!date.isAfter(to)) {
            if (isTradingDay(date)) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    /**
     * 다음 거래일 조회
     */
    public LocalDate getNextTradingDay(LocalDate from) {
        LocalDate date = from.plusDays(1);
        while (isHoliday(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    // =========================================================
    // 3. 휴장일 CRUD (관리자)
    // =========================================================

    /**
     * 휴장일 등록
     */
    @Transactional
    @CacheEvict(value = "marketHoliday", key = "#holiday.holidayDate.toString()")
    public MarketHoliday addHoliday(MarketHoliday holiday) {
        if (marketHolidayRepository.existsByHolidayDate(holiday.getHolidayDate())) {
            throw new IllegalArgumentException(
                    "이미 등록된 휴장일입니다: " + holiday.getHolidayDate());
        }
        MarketHoliday saved = marketHolidayRepository.save(holiday);
        log.info("휴장일 등록 - date:{} name:{}", saved.getHolidayDate(), saved.getHolidayName());
        return saved;
    }

    /**
     * 휴장일 일괄 등록 (연간 등록용)
     */
    @Transactional
    public List<MarketHoliday> addHolidays(List<MarketHoliday> holidays) {
        List<MarketHoliday> newHolidays = holidays.stream()
                .filter(h -> !marketHolidayRepository.existsByHolidayDate(h.getHolidayDate()))
                .toList();
        List<MarketHoliday> saved = marketHolidayRepository.saveAll(newHolidays);
        log.info("휴장일 일괄 등록 - {}건 / 중복 제외 {}건",
                holidays.size(), holidays.size() - saved.size());
        return saved;
    }

    /**
     * 휴장일 삭제
     */
    @Transactional
    @CacheEvict(value = "marketHoliday", allEntries = true)
    public void deleteHoliday(Long id) {
        marketHolidayRepository.deleteById(id);
        log.info("휴장일 삭제 - id:{}", id);
    }

    /**
     * 전체 휴장일 목록 조회 (관리자)
     */
    public List<MarketHoliday> getAllHolidays() {
        return marketHolidayRepository.findAllByOrderByHolidayDateAsc();
    }

    /**
     * 연도별 휴장일 목록
     */
    public List<MarketHoliday> getHolidaysByYear(int year) {
        return marketHolidayRepository.findByHolidayDateBetween(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));
    }
}