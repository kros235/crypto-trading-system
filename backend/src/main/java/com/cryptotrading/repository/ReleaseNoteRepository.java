package com.cryptotrading.repository;

import com.cryptotrading.entity.ReleaseNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;      
import org.springframework.data.repository.query.Param;   
import org.springframework.stereotype.Repository;


import java.util.Optional;

/**
 * 릴리즈 노트 Repository
 * Day 30: 공지사항/업데이트 이력 게시판 (2026-01-08)
 */
@Repository
public interface ReleaseNoteRepository extends JpaRepository<ReleaseNote, Long> {

    /**
     * 삭제되지 않은 게시글 목록 조회 (페이징)
     */
    Page<ReleaseNote> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 삭제되지 않은 특정 게시글 조회
     */
    Optional<ReleaseNote> findByIdAndIsDeletedFalse(Long id);

    /**
     * 최신 게시글 1건 조회 (대시보드용)
     */
    Optional<ReleaseNote> findFirstByIsDeletedFalseOrderByCreatedAtDesc();

    // ========== ⭐ Day 48: 카테고리별 조회 메서드 추가 ==========

    /**
     * 특정 카테고리의 최신 게시글 1건 조회 (Phase별 대시보드용)
     */
    Optional<ReleaseNote> findFirstByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(String category);

    /**
     * 카테고리별 게시글 목록 조회 (페이징)
     */
    Page<ReleaseNote> findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(String category, Pageable pageable);

    /**
     * 키워드 + 카테고리 검색
     */
    @Query("SELECT r FROM ReleaseNote r WHERE r.isDeleted = false " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (r.title LIKE %:keyword% OR r.authorName LIKE %:keyword% OR r.content LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    Page<ReleaseNote> searchByKeywordAndCategory(@Param("keyword") String keyword,
                                                  @Param("category") String category,
                                                  Pageable pageable);

    // 검색 기능 추가
    @Query("SELECT r FROM ReleaseNote r WHERE r.isDeleted = false " +
           "AND (r.title LIKE %:keyword% OR r.authorName LIKE %:keyword% OR r.content LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    Page<ReleaseNote> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}