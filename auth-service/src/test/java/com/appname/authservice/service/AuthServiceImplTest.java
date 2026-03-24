package com.appname.authservice.service;

import com.appname.authservice.dto.request.LoginRequest;
import com.appname.authservice.dto.request.RefreshTokenRequest;
import com.appname.authservice.dto.request.SaveCredentialsRequest;
import com.appname.authservice.dto.request.ValidateTokenRequest;
import com.appname.authservice.dto.response.TokenResponse;
import com.appname.authservice.dto.response.ValidateTokenResponse;
import com.appname.authservice.entity.RefreshToken;
import com.appname.authservice.entity.Role;
import com.appname.authservice.entity.RoleName;
import com.appname.authservice.entity.UserCredentials;
import com.appname.authservice.exception.AccountDisabledException;
import com.appname.authservice.exception.DuplicateCredentialsException;
import com.appname.authservice.exception.InvalidCredentialsException;
import com.appname.authservice.exception.InvalidTokenException;
import com.appname.authservice.repository.RefreshTokenRepository;
import com.appname.authservice.repository.RoleRepository;
import com.appname.authservice.repository.UserCredentialsRepository;
import com.appname.authservice.security.JwtService;
import com.appname.authservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
  @Mock private UserCredentialsRepository credentialsRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private JwtService jwtService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthServiceImpl authService;

  private Role userRole;
  private Role adminRole;
  private UserCredentials credentials;
  private RefreshToken validRefreshToken;

  @BeforeEach
  void setUp() {
    userRole  = Role.builder().id(2L).name(RoleName.ROLE_USER).build();
    adminRole = Role.builder().id(1L).name(RoleName.ROLE_ADMIN).build();

    credentials = UserCredentials.builder()
            .id(1L).userId(100L).login("john")
            .passwordHash("$2a$12$hashedpassword")
            .role(userRole).active(true).build();

    validRefreshToken = RefreshToken.builder()
            .id(1L).userCredentials(credentials)
            .token("valid-refresh-token")
            .revoked(false)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
  }

  @Test
  @DisplayName("saveCredentials - success with default ROLE_USER")
  void saveCredentials_Success_DefaultRole() {
    SaveCredentialsRequest request = new SaveCredentialsRequest();
    request.setUserId(100L);
    request.setLogin("john");
    request.setPassword("password123");

    when(credentialsRepository.existsByLogin("john")).thenReturn(false);
    when(credentialsRepository.existsByUserId(100L)).thenReturn(false);
    when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
    when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");

    assertThatCode(() -> authService.saveCredentials(request)).doesNotThrowAnyException();
    verify(credentialsRepository).save(any(UserCredentials.class));
  }

  @Test
  @DisplayName("saveCredentials - success with ROLE_ADMIN")
  void saveCredentials_Success_AdminRole() {
    SaveCredentialsRequest request = new SaveCredentialsRequest();
    request.setUserId(1L);
    request.setLogin("admin");
    request.setPassword("adminpass123");
    request.setRole("ADMIN");

    when(credentialsRepository.existsByLogin("admin")).thenReturn(false);
    when(credentialsRepository.existsByUserId(1L)).thenReturn(false);
    when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
    when(passwordEncoder.encode("adminpass123")).thenReturn("$2a$12$adminhashed");

    assertThatCode(() -> authService.saveCredentials(request)).doesNotThrowAnyException();
    verify(credentialsRepository).save(argThat(c ->
            c.getRole().getName() == RoleName.ROLE_ADMIN));
  }

  @Test
  @DisplayName("saveCredentials - throws DuplicateCredentialsException when login already exists")
  void saveCredentials_DuplicateLogin_ThrowsException() {
    SaveCredentialsRequest request = new SaveCredentialsRequest();
    request.setUserId(100L);
    request.setLogin("john");
    request.setPassword("password123");

    when(credentialsRepository.existsByLogin("john")).thenReturn(true);

    assertThatThrownBy(() -> authService.saveCredentials(request))
            .isInstanceOf(DuplicateCredentialsException.class)
            .hasMessageContaining("john");

    verify(credentialsRepository, never()).save(any());
  }

  @Test
  @DisplayName("saveCredentials - throws DuplicateCredentialsException when userId already exists")
  void saveCredentials_DuplicateUserId_ThrowsException() {
    SaveCredentialsRequest request = new SaveCredentialsRequest();
    request.setUserId(100L);
    request.setLogin("newlogin");
    request.setPassword("password123");

    when(credentialsRepository.existsByLogin("newlogin")).thenReturn(false);
    when(credentialsRepository.existsByUserId(100L)).thenReturn(true);

    assertThatThrownBy(() -> authService.saveCredentials(request))
            .isInstanceOf(DuplicateCredentialsException.class)
            .hasMessageContaining("100");

    verify(credentialsRepository, never()).save(any());
  }

  @Test
  @DisplayName("login - success returns token pair")
  void login_Success() {
    LoginRequest request = new LoginRequest();
    request.setLogin("john");
    request.setPassword("password123");

    when(credentialsRepository.findByLogin("john")).thenReturn(Optional.of(credentials));
    when(passwordEncoder.matches("password123", credentials.getPasswordHash())).thenReturn(true);
    when(jwtService.generateAccessToken(100L, "john", RoleName.ROLE_USER))
            .thenReturn("access-token");
    when(jwtService.generateRefreshToken("john")).thenReturn("refresh-token");
    when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

    TokenResponse result = authService.login(request);

    assertThat(result.getAccessToken()).isEqualTo("access-token");
    assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
    assertThat(result.getTokenType()).isEqualTo("Bearer");
    verify(refreshTokenRepository).revokeAllByUserCredentialsId(1L);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("login - throws InvalidCredentialsException when login not found")
  void login_LoginNotFound_ThrowsException() {
    LoginRequest request = new LoginRequest();
    request.setLogin("unknown");
    request.setPassword("password123");

    when(credentialsRepository.findByLogin("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("login - throws InvalidCredentialsException when password wrong")
  void login_WrongPassword_ThrowsException() {
    LoginRequest request = new LoginRequest();
    request.setLogin("john");
    request.setPassword("wrongpassword");

    when(credentialsRepository.findByLogin("john")).thenReturn(Optional.of(credentials));
    when(passwordEncoder.matches("wrongpassword", credentials.getPasswordHash()))
            .thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(InvalidCredentialsException.class);

    verify(jwtService, never()).generateAccessToken(any(), any(), any());
  }

  @Test
  @DisplayName("login - throws AccountDisabledException when account inactive")
  void login_AccountDisabled_ThrowsException() {
    credentials.setActive(false);
    LoginRequest request = new LoginRequest();
    request.setLogin("john");
    request.setPassword("password123");

    when(credentialsRepository.findByLogin("john")).thenReturn(Optional.of(credentials));

    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AccountDisabledException.class)
            .hasMessageContaining("100");
  }

  @Test
  @DisplayName("refresh - success rotates refresh token")
  void refresh_Success() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("valid-refresh-token");

    when(refreshTokenRepository.findByToken("valid-refresh-token"))
            .thenReturn(Optional.of(validRefreshToken));
    when(jwtService.validateToken("valid-refresh-token")).thenReturn(true);
    when(jwtService.generateAccessToken(100L, "john", RoleName.ROLE_USER))
            .thenReturn("new-access-token");
    when(jwtService.generateRefreshToken("john")).thenReturn("new-refresh-token");
    when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

    TokenResponse result = authService.refresh(request);

    assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
    // Old token must be revoked
    assertThat(validRefreshToken.getRevoked()).isTrue();
  }

  @Test
  @DisplayName("refresh - throws InvalidTokenException when token not found")
  void refresh_TokenNotFound_ThrowsException() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("nonexistent");

    when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh(request))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("not found");
  }

  @Test
  @DisplayName("refresh - throws InvalidTokenException when token already revoked")
  void refresh_RevokedToken_ThrowsException() {
    validRefreshToken.setRevoked(true);
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("valid-refresh-token");

    when(refreshTokenRepository.findByToken("valid-refresh-token"))
            .thenReturn(Optional.of(validRefreshToken));

    assertThatThrownBy(() -> authService.refresh(request))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("revoked");
  }

  @Test
  @DisplayName("refresh - throws InvalidTokenException when token expired")
  void refresh_ExpiredToken_ThrowsException() {
    validRefreshToken.setExpiresAt(LocalDateTime.now().minusDays(1));
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken("valid-refresh-token");

    when(refreshTokenRepository.findByToken("valid-refresh-token"))
            .thenReturn(Optional.of(validRefreshToken));

    assertThatThrownBy(() -> authService.refresh(request))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("expired");
  }

  @Test
  @DisplayName("validate - returns valid response with claims for valid token")
  void validate_ValidToken_ReturnsDetails() {
    ValidateTokenRequest request = new ValidateTokenRequest();
    request.setToken("valid-access-token");

    when(jwtService.validateToken("valid-access-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-access-token")).thenReturn(100L);
    when(jwtService.extractLogin("valid-access-token")).thenReturn("john");
    when(jwtService.extractRole("valid-access-token")).thenReturn("ROLE_USER");

    ValidateTokenResponse result = authService.validate(request);

    assertThat(result.isValid()).isTrue();
    assertThat(result.getUserId()).isEqualTo(100L);
    assertThat(result.getLogin()).isEqualTo("john");
    assertThat(result.getRole()).isEqualTo("ROLE_USER");
  }

  @Test
  @DisplayName("validate - returns invalid response for invalid token")
  void validate_InvalidToken_ReturnsInvalid() {
    ValidateTokenRequest request = new ValidateTokenRequest();
    request.setToken("bad-token");

    when(jwtService.validateToken("bad-token")).thenReturn(false);

    ValidateTokenResponse result = authService.validate(request);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getUserId()).isNull();
  }

}
