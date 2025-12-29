package com.cryptotrading.service;

import com.cryptotrading.dto.news.CoinNewsDTO;
import com.cryptotrading.dto.news.RssNewsItem;
import com.cryptotrading.entity.CoinNews;
import com.cryptotrading.repository.CoinNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCollectorService {
    
    private final CoinNewsRepository coinNewsRepository;
    
    // RSS Feed URL 목록
    private static final Map<String, String> RSS_FEEDS = Map.of(
    	"CoinTelegraph", "https://cointelegraph.com/rss",
	"Bitcoin_Magazine", "https://bitcoinmagazine.com/feed",
	"Decrypt", "https://decrypt.co/feed"  			
	// CoinDesk 제거 (RSS 형식 변경으로 파싱 불가)
	// Reuters 제외 (암호화폐 전용 무료 RSS 미제공)
    );
    
    // 코인 키워드 매핑 (심볼 → 검색 키워드)
    private static final Map<String, List<String>> COIN_KEYWORDS = Map.of(
        "KRW-BTC", Arrays.asList("bitcoin", "btc", "비트코인"),
        "KRW-ETH", Arrays.asList("ethereum", "eth", "이더리움"),
        "KRW-XRP", Arrays.asList("ripple", "xrp", "리플"),
        "KRW-SOL", Arrays.asList("solana", "sol", "솔라나"),
        "KRW-DOGE", Arrays.asList("dogecoin", "doge", "도지코인"),
        "KRW-ADA", Arrays.asList("cardano", "ada", "카르다노"),
        "KRW-AVAX", Arrays.asList("avalanche", "avax", "아발란체"),
        "KRW-DOT", Arrays.asList("polkadot", "dot", "폴카닷"),
        "KRW-MATIC", Arrays.asList("polygon", "matic", "폴리곤"),
        "KRW-LINK", Arrays.asList("chainlink", "link", "체인링크")
    );
    
    // KST 시간대
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    
    /**
     * 모든 RSS Feed에서 뉴스 수집
     */
    @Transactional
    public List<CoinNewsDTO> collectAllNews(List<String> targetCoins) {
        log.info("뉴스 수집 시작 - 대상 코인: {}", targetCoins);
        List<CoinNewsDTO> collectedNews = new ArrayList<>();
        
        for (Map.Entry<String, String> feed : RSS_FEEDS.entrySet()) {
            try {
                List<RssNewsItem> items = fetchRssFeed(feed.getKey(), feed.getValue());
                log.info("{} 에서 {} 개 뉴스 항목 수신", feed.getKey(), items.size());
                
                // 당일 뉴스만 필터링 및 코인 매칭
                List<CoinNews> relevantNews = filterAndMatchNews(items, targetCoins);
                
                // 중복 제거 후 저장
                for (CoinNews news : relevantNews) {
                    if (!coinNewsRepository.existsByTitleAndSource(news.getTitle(), news.getSource())) {
                        CoinNews saved = coinNewsRepository.save(news);
                        collectedNews.add(CoinNewsDTO.fromEntity(saved));
                        log.debug("뉴스 저장: [{}] {}", news.getCoinSymbol(), news.getTitle());
                    }
                }
            } catch (Exception e) {
                log.error("{} RSS Feed 수집 실패: {}", feed.getKey(), e.getMessage());
            }
        }
        
        log.info("뉴스 수집 완료 - 총 {} 건 저장", collectedNews.size());
        return collectedNews;
    }
    
    /**
     * RSS Feed 파싱
     */
    private List<RssNewsItem> fetchRssFeed(String sourceName, String feedUrl) throws Exception {
        List<RssNewsItem> items = new ArrayList<>();
        
        URL url = new URL(feedUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "CryptoTradingBot/1.0");
        
        try (InputStream is = conn.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE 방지
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            
            NodeList itemNodes = doc.getElementsByTagName("item");
            
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element item = (Element) itemNodes.item(i);
                
                RssNewsItem newsItem = RssNewsItem.builder()
                        .title(getElementText(item, "title"))
                        .description(cleanHtml(getElementText(item, "description")))
                        .link(getElementText(item, "link"))
                        .pubDate(parseRssDate(getElementText(item, "pubDate")))
                        .source(sourceName)
                        .build();
                
                items.add(newsItem);
            }
        }
        
        return items;
    }
    
    /**
     * 당일 뉴스 필터링 및 코인 매칭
     */
    private List<CoinNews> filterAndMatchNews(List<RssNewsItem> items, List<String> targetCoins) {
        List<CoinNews> result = new ArrayList<>();
        
        // 오늘 날짜 범위 (KST 기준)
        LocalDate today = LocalDate.now(KST);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        for (RssNewsItem item : items) {
            // 당일 뉴스만 필터링
            if (item.getPubDate() == null || 
                item.getPubDate().isBefore(startOfDay) || 
                item.getPubDate().isAfter(endOfDay)) {
                continue;
            }
            
            // 코인 매칭
            String matchedCoin = matchCoin(item, targetCoins);
            if (matchedCoin != null) {
                CoinNews news = CoinNews.builder()
                        .coinSymbol(matchedCoin)
                        .title(item.getTitle())
                        .summary(truncateSummary(item.getDescription(), 500))
                        .source(item.getSource())
                        .sourceUrl(item.getLink())
                        .publishedAt(item.getPubDate())
                        .collectedAt(LocalDateTime.now())
                        .build();
                result.add(news);
            }
        }
        
        return result;
    }
    
    /**
     * 뉴스 제목/본문에서 코인 매칭
     */
    private String matchCoin(RssNewsItem item, List<String> targetCoins) {
        String searchText = (item.getTitle() + " " + item.getDescription()).toLowerCase();
        
        for (String coinSymbol : targetCoins) {
            List<String> keywords = COIN_KEYWORDS.get(coinSymbol);
            if (keywords == null) continue;
            
            for (String keyword : keywords) {
                // 단어 경계를 고려한 매칭
                String pattern = "\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b";
                if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(searchText).find()) {
                    return coinSymbol;
                }
            }
        }
        
        return null;
    }
    
    /**
     * RSS 날짜 파싱 (다양한 형식 지원)
     */
    private LocalDateTime parseRssDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        // 일반적인 RSS 날짜 형식들
        List<DateTimeFormatter> formatters = Arrays.asList(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        );
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(dateStr.trim(), formatter);
                return zdt.withZoneSameInstant(KST).toLocalDateTime();
            } catch (Exception ignored) {}
        }
        
        log.warn("날짜 파싱 실패: {}", dateStr);
        return null;
    }
    
    /**
     * 특정 코인의 당일 뉴스 조회
     */
    public List<CoinNewsDTO> getTodayNews(String coinSymbol) {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        return coinNewsRepository.findTodayNewsByCoinSymbol(coinSymbol, startOfDay, endOfDay)
                .stream()
                .map(CoinNewsDTO::fromEntity)
                .toList();
    }
    
    /**
     * 7일 초과 뉴스 데이터 삭제
     */
    @Transactional
    public int cleanupOldNews() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = coinNewsRepository.deleteOldNews(threshold);
        log.info("오래된 뉴스 {} 건 삭제됨", deleted);
        return deleted;
    }
    
    // === 유틸리티 메서드 ===
    
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
    
    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    private String truncateSummary(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}