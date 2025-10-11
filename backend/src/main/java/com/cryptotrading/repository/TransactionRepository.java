package com.cryptotrading.repository;

import com.cryptotrading.entity.Transaction;
import com.cryptotrading.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUserIdAndStatus(String userId, TransactionStatus status);
    
    List<Transaction> findByUserIdAndCoinSymbolAndStatus(
        String userId, String coinSymbol, TransactionStatus status
    );
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId " +
           "AND t.coinSymbol = :coinSymbol AND t.status = 'HOLDING'")
    int countHoldingsByUserIdAndCoinSymbol(
        @Param("userId") String userId, 
        @Param("coinSymbol") String coinSymbol
    );
    
    List<Transaction> findByUserIdAndCreatedAtBetween(
        String userId, LocalDateTime start, LocalDateTime end
    );
}