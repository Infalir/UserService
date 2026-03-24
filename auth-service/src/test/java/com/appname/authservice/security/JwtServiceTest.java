package com.appname.authservice.security;

import com.appname.authservice.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

  // 64-character secret — well above the 32-byte minimum for HMAC-SHA256
  private static final String TEST_SECRET =
          "rO9jApAZfCAq3M4TXbJaRwGUCcIvrR9ac3G8nYv0egq4dh623ojwa0pElGqOf0txbTLz6tW7lpL7JNEwLYpkFv";
  private static final long ACCESS_EXPIRY_MS  = 60_000L;       // 1 minute — plenty of headroom
  private static final long REFRESH_EXPIRY_MS = 604_800_000L;  // 7 days

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    // Instantiate directly — no Mockito needed, JwtService is pure logic
    JwtProperties props = new JwtProperties();
    props.setSecret(TEST_SECRET);
    props.setAccessTokenExpirationMs(ACCESS_EXPIRY_MS);
    props.setRefreshTokenExpirationMs(REFRESH_EXPIRY_MS);
    jwtService = new JwtService(props);
  }

  // ─── generateAccessToken ──────────────────────────────────────────────────

  @Test
  @DisplayName("generateAccessToken - produces a non-blank JWT string with three parts")
  void generateAccessToken_ProducesToken() {
    String token = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);
    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  @DisplayName("generateAccessToken - token is valid immediately after creation")
  void generateAccessToken_IsValidAfterCreation() {
    String token = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);
    assertThat(jwtService.validateToken(token)).isTrue();
  }

  @Test
  @DisplayName("generateAccessToken - extracts correct login from subject claim")
  void generateAccessToken_ExtractsCorrectLogin() {
    String token = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);
    assertThat(jwtService.extractLogin(token)).isEqualTo("john");
  }

  @Test
  @DisplayName("generateAccessToken - extracts correct userId from claims")
  void generateAccessToken_ExtractsCorrectUserId() {
    String token = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);
    assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
  }

  @Test
  @DisplayName("generateAccessToken - extracts correct ROLE_USER from claims")
  void generateAccessToken_ExtractsCorrectRole() {
    String token = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);
    assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_USER");
  }

  @Test
  @DisplayName("generateAccessToken - extracts correct ROLE_ADMIN from claims")
  void generateAccessToken_AdminRoleEmbedded() {
    String token = jwtService.generateAccessToken(1L, "admin", RoleName.ROLE_ADMIN);
    assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
  }

  @Test
  @DisplayName("generateAccessToken - two tokens for the same user are each individually valid")
  void generateAccessToken_TwoTokensAreEachValid() {
    // JWT iat claim has second-level precision — two tokens generated within
    // the same second will have identical payloads and therefore identical
    // signatures. Testing inequality is not reliable. Instead we assert that
    // each token is independently valid and carries the correct claims.
    String token1 = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);
    String token2 = jwtService.generateAccessToken(42L, "john", RoleName.ROLE_USER);

    assertThat(jwtService.validateToken(token1)).isTrue();
    assertThat(jwtService.validateToken(token2)).isTrue();
    assertThat(jwtService.extractUserId(token1)).isEqualTo(42L);
    assertThat(jwtService.extractUserId(token2)).isEqualTo(42L);
    assertThat(jwtService.extractLogin(token1)).isEqualTo("john");
    assertThat(jwtService.extractLogin(token2)).isEqualTo("john");
  }

  // ─── generateRefreshToken ─────────────────────────────────────────────────

  @Test
  @DisplayName("generateRefreshToken - produces a non-blank JWT string with three parts")
  void generateRefreshToken_ProducesToken() {
    String token = jwtService.generateRefreshToken("john");
    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  @DisplayName("generateRefreshToken - token is valid immediately after creation")
  void generateRefreshToken_IsValidAfterCreation() {
    String token = jwtService.generateRefreshToken("john");
    assertThat(jwtService.validateToken(token)).isTrue();
  }

  @Test
  @DisplayName("generateRefreshToken - extracts correct login from subject claim")
  void generateRefreshToken_ExtractsCorrectLogin() {
    String token = jwtService.generateRefreshToken("john");
    assertThat(jwtService.extractLogin(token)).isEqualTo("john");
  }

  @Test
  @DisplayName("generateRefreshToken - does not contain userId claim (throws on extraction)")
  void generateRefreshToken_NoUserIdClaim() {
    String token = jwtService.generateRefreshToken("john");
    // Refresh tokens intentionally have no userId claim
    assertThatThrownBy(() -> jwtService.extractUserId(token))
            .isInstanceOf(Exception.class);
  }

  // ─── validateToken ────────────────────────────────────────────────────────

  @Test
  @DisplayName("validateToken - returns false for completely invalid string")
  void validateToken_InvalidString_ReturnsFalse() {
    assertThat(jwtService.validateToken("this.is.garbage")).isFalse();
  }

  @Test
  @DisplayName("validateToken - returns false for empty string")
  void validateToken_EmptyString_ReturnsFalse() {
    assertThat(jwtService.validateToken("")).isFalse();
  }

  @Test
  @DisplayName("validateToken - returns false for null")
  void validateToken_Null_ReturnsFalse() {
    assertThat(jwtService.validateToken(null)).isFalse();
  }

  @Test
  @DisplayName("validateToken - returns false for token signed with a different secret")
  void validateToken_WrongSecret_ReturnsFalse() {
    JwtProperties otherProps = new JwtProperties();
    otherProps.setSecret("completely-different-secret-key-64-chars-long-do-not-use-in-prod!!");
    otherProps.setAccessTokenExpirationMs(ACCESS_EXPIRY_MS);
    otherProps.setRefreshTokenExpirationMs(REFRESH_EXPIRY_MS);
    JwtService otherService = new JwtService(otherProps);

    String tokenFromOtherService = otherService.generateAccessToken(
            1L, "john", RoleName.ROLE_USER);

    assertThat(jwtService.validateToken(tokenFromOtherService)).isFalse();
  }

  @Test
  @DisplayName("validateToken - returns false for an expired token")
  void validateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
    JwtProperties shortLivedProps = new JwtProperties();
    shortLivedProps.setSecret(TEST_SECRET);
    shortLivedProps.setAccessTokenExpirationMs(1L);   // expires in 1ms
    shortLivedProps.setRefreshTokenExpirationMs(REFRESH_EXPIRY_MS);
    JwtService shortLivedService = new JwtService(shortLivedProps);

    String expiredToken = shortLivedService.generateAccessToken(
            1L, "john", RoleName.ROLE_USER);

    Thread.sleep(50); // ensure expiry

    assertThat(jwtService.validateToken(expiredToken)).isFalse();
  }

  // ─── getRefreshTokenExpirationMs ──────────────────────────────────────────

  @Test
  @DisplayName("getRefreshTokenExpirationMs - returns the configured value")
  void getRefreshTokenExpirationMs_ReturnsConfiguredValue() {
    assertThat(jwtService.getRefreshTokenExpirationMs()).isEqualTo(REFRESH_EXPIRY_MS);
  }
}