package com.appname.authservice.repository;

import com.appname.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Revoke all active refresh tokens for a user on logout or re-login
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userCredentials.id = :credentialsId AND rt.revoked = false")
    int revokeAllByUserCredentialsId(@Param("credentialsId") Long credentialsId);
}
