package com.cryptotrading.controller;

import com.cryptotrading.dto.common.ApiResponse;
import com.cryptotrading.dto.releasenote.ReleaseNoteDTO;
import com.cryptotrading.dto.releasenote.ReleaseNoteRequest;
import com.cryptotrading.service.ReleaseNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 릴리즈 노트 컨트롤러
 * Day 30: 공지사항/업데이트 이력 게시판 (2026-01-08)
 */
@RestController
@RequestMapping("/api/release-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Release Notes", description = "릴리즈 노트 API")
public class ReleaseNoteController {

    private final ReleaseNoteService releaseNoteService;

    /**
     * 게시글 목록 조회 (페이징)
     * 모든 사용자 접근 가능
     */
     @GetMapping
    @Operation(summary = "목록 조회", description = "릴리즈 노트 목록을 페이징으로 조회합니다")
    public ResponseEntity<Page<ReleaseNoteDTO>> getReleaseNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
    
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(releaseNoteService.getReleaseNotes(pageable, keyword, category));
    }

    /**
     * 게시글 상세 조회
     * 모든 사용자 접근 가능
     */
    @GetMapping("/{id}")
    @Operation(summary = "상세 조회", description = "특정 릴리즈 노트를 조회합니다")
    public ResponseEntity<ReleaseNoteDTO> getReleaseNote(@PathVariable Long id) {
        return ResponseEntity.ok(releaseNoteService.getReleaseNote(id));
    }

    /**
     * 최신 게시글 1건 조회 (대시보드용)
     * 모든 사용자 접근 가능
     */
    @GetMapping("/latest")
    @Operation(summary = "최신 1건 조회", description = "가장 최근 릴리즈 노트를 조회합니다 (대시보드용)")
    public ResponseEntity<ReleaseNoteDTO> getLatestReleaseNote(
            @RequestParam(required = false) String category) {
        ReleaseNoteDTO latest = releaseNoteService.getLatestReleaseNote(category);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * 게시글 작성 (관리자 전용)
     */
    @PostMapping
    @Operation(summary = "게시글 작성", description = "새 릴리즈 노트를 작성합니다 (관리자 전용)")
    public ResponseEntity<ReleaseNoteDTO> createReleaseNote(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReleaseNoteRequest request) {
        
        return ResponseEntity.ok(releaseNoteService.createReleaseNote(userId, request));
    }

    /**
     * 게시글 수정 (관리자 전용)
     */
    @PutMapping("/{id}")
    @Operation(summary = "게시글 수정", description = "릴리즈 노트를 수정합니다 (관리자 전용)")
    public ResponseEntity<ReleaseNoteDTO> updateReleaseNote(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReleaseNoteRequest request) {
        
        return ResponseEntity.ok(releaseNoteService.updateReleaseNote(id, userId, request));
    }

    /**
     * 게시글 삭제 (관리자 전용, Soft Delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제", description = "릴리즈 노트를 삭제합니다 (관리자 전용)")
    public ResponseEntity<ApiResponse<Void>> deleteReleaseNote(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {
        
        releaseNoteService.deleteReleaseNote(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "게시글이 삭제되었습니다."));
    }
}