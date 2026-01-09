package com.cryptotrading.config;

import com.cryptotrading.config.security.CustomAccessDeniedHandler;  
import com.cryptotrading.config.security.CustomAuthenticationEntryPoint; 
import com.cryptotrading.filter.JwtAuthenticationFilter;
import com.cryptotrading.filter.RateLimitFilter;
import com.cryptotrading.filter.RequestLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint; 
    private final CustomAccessDeniedHandler accessDeniedHandler; 
    private final RequestLoggingFilter requestLoggingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers("/api/health", "/api/health/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
	  .requestMatchers("/api/upbit/test/**").permitAll() 
	  // 코인 정보 조회는 인증 필요 없음
	  .requestMatchers("/api/coins/active").permitAll()
	  .requestMatchers("/api/coins/*/price").permitAll()
	  .requestMatchers("/api/coins/prices").permitAll()
	  // Swagger UI 경로 허용
	  .requestMatchers("/swagger-ui/**").permitAll()
	  .requestMatchers("/swagger-ui.html").permitAll()
	  .requestMatchers("/v3/api-docs/**").permitAll()
	  .requestMatchers("/swagger-resources/**").permitAll()
	  .requestMatchers("/webjars/**").permitAll()
	  // 코인 정보 업데이트는 관리자만
	  .requestMatchers("/api/coins/update").hasRole("ADMIN")

	  // 사용자 프로필 API - 인증된 사용자만
	  .requestMatchers("/api/user/**").authenticated()
    
	  // 거래 설정 API - 인증된 사용자만
	  .requestMatchers("/api/trading-settings/**").authenticated()

	  .requestMatchers("/api/transactions/**").authenticated()

                // Actuator 엔드포인트
                .requestMatchers("/actuator/**").permitAll()

                .requestMatchers("/api/bot/**").authenticated()

                .requestMatchers("/api/notifications/**").authenticated()

	  .requestMatchers("/api/backtest/**").authenticated()

	  .requestMatchers("/api/admin/**").hasRole("ADMIN")
	  .requestMatchers("/api/notifications/email/**").authenticated()

	  .requestMatchers("/api/news/today/**").permitAll()     // 당일 뉴스 조회 - 공개
	  .requestMatchers("/api/news/analysis/status").permitAll()  // AI 상태 확인 - 공개
	  .requestMatchers("/api/news/**").authenticated()       // 나머지 뉴스 API - 인증 필요

   	  // 릴리즈 노트 API 권한 설정
	  .requestMatchers(HttpMethod.GET, "/api/release-notes/**").authenticated()  // 조회는 인증된 사용자
	  .requestMatchers(HttpMethod.POST, "/api/release-notes/**").hasRole("ADMIN") // 작성은 관리자만
	  .requestMatchers(HttpMethod.PUT, "/api/release-notes/**").hasRole("ADMIN")  // 수정은 관리자만
	  .requestMatchers(HttpMethod.DELETE, "/api/release-notes/**").hasRole("ADMIN") // 삭제는 관리자만

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            // CORS 필터를 Security 필터 체인 최상단에 배치
            // 401/403 에러 응답에도 CORS 헤더가 포함되어 브라우저에서 에러 메시지 읽기 가능
            .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requestLoggingFilter, SecurityContextHolderFilter.class);  
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost", 
            "http://localhost:80", 
            "http://localhost:3000",
            "http://158.179.161.29",
            "http://crypto-trading-dev.duckdns.org",
            "https://crypto-trading-dev.duckdns.org",
            "http://crypto-trading-prd.duckdns.org",
            "https://crypto-trading-prd.duckdns.org"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}