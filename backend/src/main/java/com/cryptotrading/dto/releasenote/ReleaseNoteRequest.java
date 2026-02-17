package com.cryptotrading.dto.releasenote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 릴리즈 노트 생성/수정 요청 DTO
 * Day 30: 공지사항/업데이트 이력 게시판 (2026-01-08)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseNoteRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    @Size(max = 20, message = "카테고리는 20자 이내로 입력해주세요")
    @Builder.Default
    private String category = "GENERAL";
}