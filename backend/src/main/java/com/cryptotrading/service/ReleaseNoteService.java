package com.cryptotrading.service;

import com.cryptotrading.dto.releasenote.ReleaseNoteDTO;
import com.cryptotrading.dto.releasenote.ReleaseNoteRequest;
import com.cryptotrading.entity.ReleaseNote;
import com.cryptotrading.entity.User;
import com.cryptotrading.exception.EntityNotFoundException;
import com.cryptotrading.exception.ErrorCode;  // ⭐ 추가
import com.cryptotrading.repository.ReleaseNoteRepository;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 릴리즈 노트 서비스
 * Day 30: 공지사항/업데이트 이력 게시판 (2026-01-08)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseNoteService {

    private final ReleaseNoteRepository releaseNoteRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 목록 조회 (페이징 + 검색)
     */
    @Transactional(readOnly = true)
    public Page<ReleaseNoteDTO> getReleaseNotes(Pageable pageable, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return releaseNoteRepository.searchByKeyword(keyword.trim(), pageable)
                    .map(ReleaseNoteDTO::fromEntity);
        }
        return releaseNoteRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(ReleaseNoteDTO::fromEntity);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional(readOnly = true)
    public ReleaseNoteDTO getReleaseNote(Long id) {
        ReleaseNote releaseNote = releaseNoteRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "게시글 ID: " + id));  // ⭐ 수정
        return ReleaseNoteDTO.fromEntity(releaseNote);
    }

    /**
     * 최신 게시글 1건 조회 (대시보드용)
     */
    @Transactional(readOnly = true)
    public ReleaseNoteDTO getLatestReleaseNote() {
        return releaseNoteRepository.findFirstByIsDeletedFalseOrderByCreatedAtDesc()
                .map(ReleaseNoteDTO::fromEntity)
                .orElse(null);
    }

    /**
     * 게시글 작성 (관리자 전용)
     */
    @Transactional
    public ReleaseNoteDTO createReleaseNote(String userId, ReleaseNoteRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));  // ⭐ 수정

        ReleaseNote releaseNote = ReleaseNote.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .authorId(userId)
                .authorName(user.getUserId())
                .build();

        ReleaseNote saved = releaseNoteRepository.save(releaseNote);
        log.info("릴리즈 노트 작성: ID={}, 제목={}, 작성자={}", saved.getId(), saved.getTitle(), userId);
        
        return ReleaseNoteDTO.fromEntity(saved);
    }

    /**
     * 게시글 수정 (관리자 전용)
     */
    @Transactional
    public ReleaseNoteDTO updateReleaseNote(Long id, String userId, ReleaseNoteRequest request) {
        ReleaseNote releaseNote = releaseNoteRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "게시글 ID: " + id));  // ⭐ 수정

        releaseNote.setTitle(request.getTitle());
        releaseNote.setContent(request.getContent());

        ReleaseNote updated = releaseNoteRepository.save(releaseNote);
        log.info("릴리즈 노트 수정: ID={}, 제목={}, 수정자={}", updated.getId(), updated.getTitle(), userId);
        
        return ReleaseNoteDTO.fromEntity(updated);
    }

    /**
     * 게시글 삭제 (관리자 전용, Soft Delete)
     */
    @Transactional
    public void deleteReleaseNote(Long id, String userId) {
        ReleaseNote releaseNote = releaseNoteRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "게시글 ID: " + id));  // ⭐ 수정

        releaseNote.setIsDeleted(true);
        releaseNoteRepository.save(releaseNote);
        
        log.info("릴리즈 노트 삭제: ID={}, 제목={}, 삭제자={}", id, releaseNote.getTitle(), userId);
    }
}