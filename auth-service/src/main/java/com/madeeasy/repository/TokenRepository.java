package com.madeeasy.repository;

import com.madeeasy.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {

    @Query("SELECT t FROM Token t WHERE t.user.id = :id AND t.isExpired = false AND t.isRevoked = false")
    List<Token> findAllValidTokens(String id);

    Optional<Token> findByToken(String token);
}
