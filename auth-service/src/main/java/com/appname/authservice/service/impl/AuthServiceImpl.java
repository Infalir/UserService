package com.appname.authservice.service.impl;

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
import com.appname.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserCredentialsRepository credentialsRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void saveCredentials(SaveCredentialsRequest request) {
        if (credentialsRepository.existsByLogin(request.getLogin())) {
            throw new DuplicateCredentialsException(
                    "Login '" + request.getLogin() + "' is already taken");
        }
        if (credentialsRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateCredentialsException(
                    "Credentials for user id " + request.getUserId() + " already exist");
        }

        RoleName roleName = parseRole(request.getRole());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found in DB"));

        UserCredentials credentials = UserCredentials.builder()
                .userId(request.getUserId())
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();

        credentialsRepository.save(credentials);
        log.info("Saved credentials for userId: {}, login: {}, role: {}",
                request.getUserId(), request.getLogin(), roleName);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserCredentials credentials = credentialsRepository.findByLogin(request.getLogin())
                .orElseThrow(InvalidCredentialsException::new);

        if (!credentials.getActive()) {
            throw new AccountDisabledException(credentials.getUserId());
        }

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        refreshTokenRepository.revokeAllByUserCredentialsId(credentials.getId());

        String accessToken  = jwtService.generateAccessToken(
                credentials.getUserId(),
                credentials.getLogin(),
                credentials.getRole().getName()
        );
        String refreshToken = jwtService.generateRefreshToken(credentials.getLogin());

        saveRefreshToken(credentials, refreshToken);

        log.info("User logged in: {}", credentials.getLogin());
        return buildTokenResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!stored.isValid()) {
            throw new InvalidTokenException(
                    stored.getRevoked() ? "Refresh token has been revoked" : "Refresh token has expired");
        }

        if (!jwtService.validateToken(request.getRefreshToken())) {
            throw new InvalidTokenException("Refresh token signature is invalid");
        }

        UserCredentials credentials = stored.getUserCredentials();

        if (!credentials.getActive()) {
            throw new AccountDisabledException(credentials.getUserId());
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken  = jwtService.generateAccessToken(
                credentials.getUserId(),
                credentials.getLogin(),
                credentials.getRole().getName()
        );
        String newRefreshToken = jwtService.generateRefreshToken(credentials.getLogin());

        saveRefreshToken(credentials, newRefreshToken);

        log.info("Tokens refreshed for: {}", credentials.getLogin());
        return buildTokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public ValidateTokenResponse validate(ValidateTokenRequest request) {
        if (!jwtService.validateToken(request.getToken())) {
            return ValidateTokenResponse.builder().valid(false).build();
        }
        return ValidateTokenResponse.builder()
                .valid(true)
                .userId(jwtService.extractUserId(request.getToken()))
                .login(jwtService.extractLogin(request.getToken()))
                .role(jwtService.extractRole(request.getToken()))
                .build();
    }

    private void saveRefreshToken(UserCredentials credentials, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userCredentials(credentials)
                .token(token)
                .revoked(false)
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private TokenResponse buildTokenResponse(String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getRefreshTokenExpirationMs() / 1000)
                .build();
    }

    private RoleName parseRole(String role) {
        if (role == null || role.isBlank()) return RoleName.ROLE_USER;
        try {
            String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
            return RoleName.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role '{}', defaulting to ROLE_USER", role);
            return RoleName.ROLE_USER;
        }
    }

}
