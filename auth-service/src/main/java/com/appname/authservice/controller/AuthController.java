package com.appname.authservice.controller;

import com.appname.authservice.dto.request.LoginRequest;
import com.appname.authservice.dto.request.RefreshTokenRequest;
import com.appname.authservice.dto.request.SaveCredentialsRequest;
import com.appname.authservice.dto.request.ValidateTokenRequest;
import com.appname.authservice.dto.response.TokenResponse;
import com.appname.authservice.dto.response.ValidateTokenResponse;
import com.appname.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/credentials")
    public ResponseEntity<Void> saveCredentials(@Valid @RequestBody SaveCredentialsRequest request) {
        authService.saveCredentials(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validate(request));
    }

}
