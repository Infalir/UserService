package com.appname.authservice.security;

import com.appname.authservice.entity.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE    = "role";

    private final JwtProperties jwtProperties;

    // Step 6: JWT contains userId and role
    public String generateAccessToken(Long userId, String login, RoleName role) {
        return Jwts.builder()
                .subject(login)
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_ROLE, role.name()
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String login) {
        return Jwts.builder()
                .subject(login)
                // jti (JWT ID) is a UUID nonce — guarantees uniqueness even when two
                // refresh tokens are generated within the same second for the same user.
                // Without this, tokens generated in the same second produce identical
                // strings (same sub + iat + exp) causing a unique constraint violation
                // in the refresh_tokens table.
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String extractLogin(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object userId = parseClaims(token).get(CLAIM_USER_ID);
        if (userId instanceof Integer i) return i.longValue();
        if (userId instanceof Long l) return l;
        throw new JwtException("Invalid userId claim type");
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).get(CLAIM_ROLE);
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpirationMs();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
