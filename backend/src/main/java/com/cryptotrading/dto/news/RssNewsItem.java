package com.cryptotrading.dto.news;

import lombok.*;
import java.time.LocalDateTime;

/**
 * RSS Feed에서 파싱한 뉴스 아이템
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssNewsItem {
    
    private String title;
    private String description;
    private String link;
    private LocalDateTime pubDate;
    private String source;
    
    // 코인 관련 여부 판별용
    private boolean relevant;
    private String matchedCoinSymbol;
}