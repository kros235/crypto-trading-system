package com.trading;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 원하는 비밀번호 입력
        String rawPassword = "John369369!";
        
        // 암호화된 비밀번호 생성
        String hashedPassword = encoder.encode(rawPassword);
        
        System.out.println("=".repeat(80));
        System.out.println("비밀번호 암호화 결과");
        System.out.println("=".repeat(80));
        System.out.println("원본 비밀번호: " + rawPassword);
        System.out.println("암호화된 비밀번호: " + hashedPassword);
        System.out.println("=".repeat(80));
        System.out.println("\n아래 SQL을 실행하여 비밀번호를 업데이트하세요:");
        System.out.println("UPDATE users SET password_hash = '" + hashedPassword + "' WHERE user_id = 'admin';");
    }
}