package com.appname.authservice.integration;

import com.appname.authservice.dto.request.LoginRequest;
import com.appname.authservice.dto.request.RefreshTokenRequest;
import com.appname.authservice.dto.request.SaveCredentialsRequest;
import com.appname.authservice.dto.request.ValidateTokenRequest;
import com.appname.authservice.dto.response.ErrorResponse;
import com.appname.authservice.dto.response.TokenResponse;
import com.appname.authservice.dto.response.ValidateTokenResponse;
import com.appname.authservice.entity.RoleName;
import com.appname.authservice.repository.RefreshTokenRepository;
import com.appname.authservice.repository.UserCredentialsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthIntegrationTest extends BaseIntegrationTest {
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserCredentialsRepository credentialsRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  private static final String BASE_URL = "/api/v1/auth";

  private SaveCredentialsRequest buildSaveRequest(Long userId, String login, String password) {
    SaveCredentialsRequest req = new SaveCredentialsRequest();
    req.setUserId(userId);
    req.setLogin(login);
    req.setPassword(password);
    return req;
  }

  private SaveCredentialsRequest buildSaveRequest(
          Long userId, String login, String password, String role) {
    SaveCredentialsRequest req = buildSaveRequest(userId, login, password);
    req.setRole(role);
    return req;
  }

  private TokenResponse loginAndGetTokens(String login, String password) {
    LoginRequest req = new LoginRequest();
    req.setLogin(login);
    req.setPassword(password);
    return restTemplate.postForEntity(
            BASE_URL + "/login", req, TokenResponse.class).getBody();
  }

  private void register(Long userId, String login, String password) {
    restTemplate.postForEntity(BASE_URL + "/credentials",
            buildSaveRequest(userId, login, password), Void.class);
  }

  @Test
  @DisplayName("POST /credentials - saves user credentials and returns 201")
  void saveCredentials_Returns201() {
    ResponseEntity<Void> response = restTemplate.postForEntity(
            BASE_URL + "/credentials",
            buildSaveRequest(1L, "john", "password123"),
            Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(credentialsRepository.existsByLogin("john")).isTrue();
  }

  @Test
  @DisplayName("POST /credentials - password is stored as BCrypt hash, never plain text")
  void saveCredentials_PasswordIsHashed() {
    register(1L, "john", "password123");

    String storedHash = credentialsRepository.findByLogin("john")
            .orElseThrow().getPasswordHash();

    assertThat(storedHash).isNotEqualTo("password123");
    assertThat(storedHash).startsWith("$2a$");
  }

  @Test
  @DisplayName("POST /credentials - two users get different hashes for same password (unique salt)")
  void saveCredentials_SamePasswordDifferentHash() {
    register(1L, "user1", "samepassword");
    register(2L, "user2", "samepassword");

    String hash1 = credentialsRepository.findByLogin("user1").orElseThrow().getPasswordHash();
    String hash2 = credentialsRepository.findByLogin("user2").orElseThrow().getPasswordHash();

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  @DisplayName("POST /credentials - assigns ROLE_USER by default when role is omitted")
  void saveCredentials_DefaultsToRoleUser() {
    register(1L, "john", "password123");

    RoleName role = credentialsRepository.findByLogin("john")
            .orElseThrow().getRole().getName();

    assertThat(role).isEqualTo(RoleName.ROLE_USER);
  }

  @Test
  @DisplayName("POST /credentials - assigns ROLE_ADMIN when role=ADMIN is provided")
  void saveCredentials_AssignsAdminRole() {
    restTemplate.postForEntity(BASE_URL + "/credentials",
            buildSaveRequest(1L, "admin", "password123", "ADMIN"), Void.class);

    RoleName role = credentialsRepository.findByLogin("admin")
            .orElseThrow().getRole().getName();

    assertThat(role).isEqualTo(RoleName.ROLE_ADMIN);
  }

  @Test
  @DisplayName("POST /credentials - returns 409 on duplicate login")
  void saveCredentials_DuplicateLogin_Returns409() {
    register(1L, "john", "password123");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/credentials",
            buildSaveRequest(2L, "john", "otherpassword"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().getMessage()).contains("john");
  }

  @Test
  @DisplayName("POST /credentials - returns 409 on duplicate userId")
  void saveCredentials_DuplicateUserId_Returns409() {
    register(1L, "john", "password123");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/credentials",
            buildSaveRequest(1L, "differentlogin", "password123"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @DisplayName("POST /credentials - returns 400 when password is too short")
  void saveCredentials_ShortPassword_Returns400() {
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/credentials",
            buildSaveRequest(1L, "john", "short"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors()).containsKey("password");
  }

  @Test
  @DisplayName("POST /credentials - returns 400 when userId is missing")
  void saveCredentials_MissingUserId_Returns400() {
    SaveCredentialsRequest req = new SaveCredentialsRequest();
    req.setLogin("john");
    req.setPassword("password123");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/credentials", req, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors()).containsKey("userId");
  }

  @Test
  @DisplayName("POST /login - returns 200 with access and refresh tokens")
  void login_Returns200WithTokens() {
    register(1L, "john", "password123");

    TokenResponse tokens = loginAndGetTokens("john", "password123");

    assertThat(tokens).isNotNull();
    assertThat(tokens.getAccessToken()).isNotBlank();
    assertThat(tokens.getRefreshToken()).isNotBlank();
    assertThat(tokens.getTokenType()).isEqualTo("Bearer");
    assertThat(tokens.getExpiresIn()).isPositive();
  }

  @Test
  @DisplayName("POST /login - access token contains correct userId and role claims")
  void login_AccessTokenContainsCorrectClaims() {
    register(42L, "john", "password123");
    TokenResponse tokens = loginAndGetTokens("john", "password123");

    ValidateTokenRequest validateReq = new ValidateTokenRequest();
    validateReq.setToken(tokens.getAccessToken());
    ValidateTokenResponse validation = restTemplate.postForEntity(
            BASE_URL + "/validate", validateReq, ValidateTokenResponse.class).getBody();

    assertThat(validation.isValid()).isTrue();
    assertThat(validation.getUserId()).isEqualTo(42L);
    assertThat(validation.getLogin()).isEqualTo("john");
    assertThat(validation.getRole()).isEqualTo("ROLE_USER");
  }

  @Test
  @DisplayName("POST /login - admin token contains ROLE_ADMIN claim")
  void login_AdminTokenContainsAdminRole() {
    restTemplate.postForEntity(BASE_URL + "/credentials",
            buildSaveRequest(1L, "admin", "password123", "ADMIN"), Void.class);

    TokenResponse tokens = loginAndGetTokens("admin", "password123");

    ValidateTokenRequest validateReq = new ValidateTokenRequest();
    validateReq.setToken(tokens.getAccessToken());
    ValidateTokenResponse validation = restTemplate.postForEntity(
            BASE_URL + "/validate", validateReq, ValidateTokenResponse.class).getBody();

    assertThat(validation.getRole()).isEqualTo("ROLE_ADMIN");
  }

  @Test
  @DisplayName("POST /login - returns 401 on wrong password")
  void login_WrongPassword_Returns401() {
    register(1L, "john", "password123");

    LoginRequest req = new LoginRequest();
    req.setLogin("john");
    req.setPassword("wrongpassword");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/login", req, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /login - returns 401 on unknown login")
  void login_UnknownLogin_Returns401() {
    LoginRequest req = new LoginRequest();
    req.setLogin("nobody");
    req.setPassword("password123");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/login", req, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /login - re-login revokes all previous refresh tokens")
  void login_ReLoginRevokesOldRefreshTokens() {
    register(1L, "john", "password123");

    TokenResponse firstTokens = loginAndGetTokens("john", "password123");
    loginAndGetTokens("john", "password123");

    RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
    refreshRequest.setRefreshToken(firstTokens.getRefreshToken());

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/refresh", refreshRequest, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /login - returns 400 when login field is blank")
  void login_BlankLogin_Returns400() {
    LoginRequest req = new LoginRequest();
    req.setLogin("");
    req.setPassword("password123");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/login", req, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors()).containsKey("login");
  }

  @Test
  @DisplayName("POST /refresh - returns new token pair and rotates refresh token")
  void refresh_ReturnsNewTokenPair() {
    register(1L, "john", "password123");
    TokenResponse tokens = loginAndGetTokens("john", "password123");

    RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
    refreshRequest.setRefreshToken(tokens.getRefreshToken());

    ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            BASE_URL + "/refresh", refreshRequest, TokenResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getAccessToken()).isNotBlank();
    assertThat(response.getBody().getRefreshToken())
            .isNotBlank()
            .isNotEqualTo(tokens.getRefreshToken());
  }

  @Test
  @DisplayName("POST /refresh - old token cannot be reused after rotation")
  void refresh_OldTokenCannotBeReused() {
    register(1L, "john", "password123");
    TokenResponse tokens = loginAndGetTokens("john", "password123");

    RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
    refreshRequest.setRefreshToken(tokens.getRefreshToken());

    restTemplate.postForEntity(BASE_URL + "/refresh", refreshRequest, TokenResponse.class);

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/refresh", refreshRequest, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody().getMessage()).containsIgnoringCase("revoked");
  }

  @Test
  @DisplayName("POST /refresh - newly rotated refresh token is itself usable")
  void refresh_NewRefreshTokenIsUsable() {
    register(1L, "john", "password123");
    TokenResponse firstTokens = loginAndGetTokens("john", "password123");

    RefreshTokenRequest firstRefresh = new RefreshTokenRequest();
    firstRefresh.setRefreshToken(firstTokens.getRefreshToken());
    TokenResponse secondTokens = restTemplate.postForEntity(
            BASE_URL + "/refresh", firstRefresh, TokenResponse.class).getBody();

    RefreshTokenRequest secondRefresh = new RefreshTokenRequest();
    secondRefresh.setRefreshToken(secondTokens.getRefreshToken());

    ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            BASE_URL + "/refresh", secondRefresh, TokenResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getAccessToken()).isNotBlank();
  }

  @Test
  @DisplayName("POST /refresh - returns 401 for completely invalid token string")
  void refresh_GarbageToken_Returns401() {
    RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
    refreshRequest.setRefreshToken("this.is.garbage");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/refresh", refreshRequest, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /refresh - returns 400 when refresh token field is blank")
  void refresh_BlankToken_Returns400() {
    RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
    refreshRequest.setRefreshToken("");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/refresh", refreshRequest, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors()).containsKey("refreshToken");
  }

  @Test
  @DisplayName("POST /validate - returns valid=true with correct claims for valid access token")
  void validate_ValidAccessToken_ReturnsDetails() {
    register(99L, "john", "password123");
    TokenResponse tokens = loginAndGetTokens("john", "password123");

    ValidateTokenRequest validateRequest = new ValidateTokenRequest();
    validateRequest.setToken(tokens.getAccessToken());

    ResponseEntity<ValidateTokenResponse> response = restTemplate.postForEntity(
            BASE_URL + "/validate", validateRequest, ValidateTokenResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().isValid()).isTrue();
    assertThat(response.getBody().getUserId()).isEqualTo(99L);
    assertThat(response.getBody().getLogin()).isEqualTo("john");
    assertThat(response.getBody().getRole()).isEqualTo("ROLE_USER");
  }

  @Test
  @DisplayName("POST /validate - returns valid=false for garbage token without throwing")
  void validate_GarbageToken_ReturnsValidFalse() {
    ValidateTokenRequest validateRequest = new ValidateTokenRequest();
    validateRequest.setToken("this.is.garbage");

    ResponseEntity<ValidateTokenResponse> response = restTemplate.postForEntity(
            BASE_URL + "/validate", validateRequest, ValidateTokenResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().isValid()).isFalse();
    assertThat(response.getBody().getUserId()).isNull();
  }

  @Test
  @DisplayName("POST /validate - returns 400 when token is blank")
  void validate_BlankToken_Returns400() {
    ValidateTokenRequest validateRequest = new ValidateTokenRequest();
    validateRequest.setToken("");

    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            BASE_URL + "/validate", validateRequest, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors()).containsKey("token");
  }

  @Test
  @DisplayName("Full flow: register -> login -> validate -> refresh -> validate with new token")
  void fullAuthFlow_EndToEnd() {
    ResponseEntity<Void> registerResponse = restTemplate.postForEntity(
            BASE_URL + "/credentials",
            buildSaveRequest(10L, "e2euser", "password123"),
            Void.class);
    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    TokenResponse tokens = loginAndGetTokens("e2euser", "password123");
    assertThat(tokens.getAccessToken()).isNotBlank();

    ValidateTokenRequest validateReq = new ValidateTokenRequest();
    validateReq.setToken(tokens.getAccessToken());
    ValidateTokenResponse validation = restTemplate.postForEntity(
            BASE_URL + "/validate", validateReq, ValidateTokenResponse.class).getBody();
    assertThat(validation.isValid()).isTrue();
    assertThat(validation.getUserId()).isEqualTo(10L);

    RefreshTokenRequest refreshReq = new RefreshTokenRequest();
    refreshReq.setRefreshToken(tokens.getRefreshToken());
    TokenResponse newTokens = restTemplate.postForEntity(
            BASE_URL + "/refresh", refreshReq, TokenResponse.class).getBody();
    assertThat(newTokens.getAccessToken()).isNotBlank();

    ValidateTokenRequest validateNewReq = new ValidateTokenRequest();
    validateNewReq.setToken(newTokens.getAccessToken());
    ValidateTokenResponse newValidation = restTemplate.postForEntity(
            BASE_URL + "/validate", validateNewReq, ValidateTokenResponse.class).getBody();
    assertThat(newValidation.isValid()).isTrue();
    assertThat(newValidation.getUserId()).isEqualTo(10L);
  }
}
