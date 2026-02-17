package com.cryptotrading.dto.releasenote;

import com.cryptotrading.entity.ReleaseNote;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 릴리즈 노트 응답 DTO
 * Day 30: 공지사항/업데이트 이력 게시판 (2026-01-08)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseNoteDTO {
    
    private Long id;
    private String title;
    private String content;
    private String category;
    private String authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static ReleaseNoteDTO fromEntity(ReleaseNote entity) {
        return ReleaseNoteDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
	  .category(entity.getCategory())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthorName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}