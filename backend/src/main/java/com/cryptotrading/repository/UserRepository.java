package com.cryptotrading.repository;

import com.cryptotrading.entity.User;
import com.cryptotrading.entity.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);  
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUserId(String userId);
    
    boolean existsByEmail(String email);

    List<User> findByIsActive(Boolean isActive);

    List<User> findByRole(UserRole role);
  
    List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
}