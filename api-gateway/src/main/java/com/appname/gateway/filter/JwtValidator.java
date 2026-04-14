package com.appname.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtValidator {

  @Value("${jwt.secret}")
  private String jwtSecret;

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT: {}", e.getMessage());
      return false;
    }
  }

  public Claims extractClaims(String token) {
    return parseClaims(token);
  }

  public String extractUserId(String token) {
    Object userId = parseClaims(token).get("userId");
    if (userId instanceof Integer i) return String.valueOf(i.longValue());
    if (userId instanceof Long l) return String.valueOf(l);
    return String.valueOf(userId);
  }

  public String extractRole(String token) {
    return (String) parseClaims(token).get("role");
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }
}
