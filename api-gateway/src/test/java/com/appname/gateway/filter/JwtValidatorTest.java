package com.appname.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtValidatorTest {
  private static final String SECRET = "rO9jApAZfCAq3M4TXbJaRwGUCcIvrR9ac3G8nYv0egq4dh623ojwa0pElGqOf0txbTLz6tW7lpL7JNEwLYpkFv";

  private JwtValidator jwtValidator;

  @BeforeEach
  void setUp() {
    jwtValidator = new JwtValidator();
    ReflectionTestUtils.setField(jwtValidator, "jwtSecret", SECRET);
  }

  private String buildToken(long expiryMs, Map<String, Object> extraClaims) {
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    var builder = Jwts.builder().subject("testuser").issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiryMs)).signWith(key);
    extraClaims.forEach(builder::claim);
    return builder.compact();
  }

  @Test
  @DisplayName("isValid - returns true for a valid token")
  void isValid_ValidToken_ReturnsTrue() {
    String token = buildToken(60_000, Map.of("userId", 1L, "role", "ROLE_USER"));
    assertThat(jwtValidator.isValid(token)).isTrue();
  }

  @Test
  @DisplayName("isValid - returns false for an expired token")
  void isValid_ExpiredToken_ReturnsFalse() throws InterruptedException {
    String token = buildToken(1, Map.of("userId", 1L, "role", "ROLE_USER"));
    Thread.sleep(50);
    assertThat(jwtValidator.isValid(token)).isFalse();
  }

  @Test
  @DisplayName("isValid - returns false for garbage string")
  void isValid_Garbage_ReturnsFalse() {
    assertThat(jwtValidator.isValid("not.a.jwt")).isFalse();
  }

  @Test
  @DisplayName("isValid - returns false for null")
  void isValid_Null_ReturnsFalse() {
    assertThat(jwtValidator.isValid(null)).isFalse();
  }

  @Test
  @DisplayName("isValid - returns false for token signed with different secret")
  void isValid_WrongSecret_ReturnsFalse() {
    SecretKey otherKey = Keys.hmacShaKeyFor(
            "qWM7NCzd7LrnAFBydpRTPX6ZFlb6xw8o9W1FvnCYxRv9g0ATkn6A1ngEYp0al7wxOIILgWKdHTWJ955l1i8X5y".getBytes(StandardCharsets.UTF_8));
    String token = Jwts.builder().subject("x").expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(otherKey).compact();
    assertThat(jwtValidator.isValid(token)).isFalse();
  }

  @Test
  @DisplayName("extractUserId - extracts correct userId from token")
  void extractUserId_ReturnsCorrectId() {
    String token = buildToken(60_000, Map.of("userId", 42L, "role", "ROLE_USER"));
    assertThat(jwtValidator.extractUserId(token)).isEqualTo("42");
  }

  @Test
  @DisplayName("extractRole - extracts correct role from token")
  void extractRole_ReturnsCorrectRole() {
    String token = buildToken(60_000, Map.of("userId", 1L, "role", "ROLE_ADMIN"));
    assertThat(jwtValidator.extractRole(token)).isEqualTo("ROLE_ADMIN");
  }

}
