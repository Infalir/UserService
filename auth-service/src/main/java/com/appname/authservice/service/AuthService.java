package com.appname.authservice.service;

import com.appname.authservice.dto.request.LoginRequest;
import com.appname.authservice.dto.request.RefreshTokenRequest;
import com.appname.authservice.dto.request.SaveCredentialsRequest;
import com.appname.authservice.dto.request.ValidateTokenRequest;
import com.appname.authservice.dto.response.TokenResponse;
import com.appname.authservice.dto.response.ValidateTokenResponse;

public interface AuthService {
    void saveCredentials(SaveCredentialsRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshTokenRequest request);
    ValidateTokenResponse validate(ValidateTokenRequest request);

}
