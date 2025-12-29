package com.cryptotrading.dto.news;

import com.cryptotrading.entity.CoinNews;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinNewsDTO {
    
    private Long id;
    private String coinSymbol;
    private String title;
    private String summary;
    private String source;
    private String sourceUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime collectedAt;
    
    public static CoinNewsDTO fromEntity(CoinNews entity) {
        return CoinNewsDTO.builder()
                .id(entity.getId())
                .coinSymbol(entity.getCoinSymbol())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .source(entity.getSource())
                .sourceUrl(entity.getSourceUrl())
                .publishedAt(entity.getPublishedAt())
                .collectedAt(entity.getCollectedAt())
                .build();
    }
    
    public CoinNews toEntity() {
        return CoinNews.builder()
                .coinSymbol(coinSymbol)
                .title(title)
                .summary(summary)
                .source(source)
                .sourceUrl(sourceUrl)
                .publishedAt(publishedAt)
                .collectedAt(collectedAt != null ? collectedAt : LocalDateTime.now())
                .build();
    }
}