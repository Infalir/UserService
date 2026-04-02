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

/**
 * REST controller exposing the authentication API for the auth-service.
 *
 * <p>All endpoints are publicly accessible (no JWT required) since this
 * controller handles the initial authentication flow. Base path:
 * {@code /api/v1/auth}.</p>
 *
 * <p>Security exceptions (401, 403) are handled by
 * {@link com.appname.authservice.exception.handler.SecurityExceptionHandler}
 * at the filter level, and business/validation exceptions are handled by
 * {@link com.appname.authservice.exception.handler.GlobalExceptionHandler}
 * at the controller advice level.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Saves hashed credentials for a newly registered user.
     *
     * <p>Intended to be called by user-service immediately after creating
     * a new {@code User} record, to register the user's login and password
     * in the auth database. The password is hashed with BCrypt before storage
     * — the plain-text value is never persisted.</p>
     *
     * @param request the credentials payload containing {@code userId},
     *                {@code login}, {@code password}, and optionally {@code role}
     *                ({@code ROLE_USER} assigned by default if omitted)
     * @return {@code 201 Created} with an empty body on success
     *
     * @apiNote {@code POST /api/v1/auth/credentials}
     *
     * <p><b>Error responses:</b></p>
     * <ul>
     *   <li>{@code 400 Bad Request} — validation failure (blank login,
     *       password under 8 characters, missing userId)</li>
     *   <li>{@code 409 Conflict} — login or userId already exists</li>
     * </ul>
     */
    @PostMapping("/credentials")
    public ResponseEntity<Void> saveCredentials(@Valid @RequestBody SaveCredentialsRequest request) {
        authService.saveCredentials(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Authenticates a user and returns a JWT access and refresh token pair.
     *
     * <p>Verifies the provided login and password against the stored BCrypt
     * hash. On success, revokes all previously active refresh tokens for the
     * user and issues a fresh token pair. The access token contains
     * {@code userId} and {@code role} claims used for authorization in
     * downstream services.</p>
     *
     * @param request the login payload containing {@code login} and {@code password}
     * @return {@code 200 OK} with a {@link TokenResponse} containing
     *         {@code accessToken}, {@code refreshToken}, {@code tokenType}
     *         ({@code "Bearer"}), and {@code expiresIn} (seconds)
     *
     * @apiNote {@code POST /api/v1/auth/login}
     *
     * <p><b>Error responses:</b></p>
     * <ul>
     *   <li>{@code 400 Bad Request} — blank login or password</li>
     *   <li>{@code 401 Unauthorized} — login not found or password mismatch</li>
     *   <li>{@code 403 Forbidden} — account is inactive</li>
     * </ul>
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Issues a new JWT token pair in exchange for a valid refresh token.
     *
     * <p>Implements one-time-use refresh token rotation: the submitted refresh
     * token is immediately revoked after use, and a brand new refresh token
     * (with a unique {@code jti} UUID claim) is issued alongside a new access
     * token. Any attempt to reuse the old refresh token after rotation will
     * be rejected with {@code 401}.</p>
     *
     * @param request the refresh payload containing the {@code refreshToken} string
     * @return {@code 200 OK} with a {@link TokenResponse} containing a new
     *         {@code accessToken} and a new {@code refreshToken}
     *
     * @apiNote {@code POST /api/v1/auth/refresh}
     *
     * <p><b>Error responses:</b></p>
     * <ul>
     *   <li>{@code 400 Bad Request} — blank refresh token field</li>
     *   <li>{@code 401 Unauthorized} — token not found, revoked, expired,
     *       or signature invalid</li>
     *   <li>{@code 403 Forbidden} — associated account has been deactivated</li>
     * </ul>
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Validates a JWT access token and returns its embedded claims.
     *
     * <p>This endpoint never throws an exception for an invalid token. If the
     * token is invalid, expired, or malformed, it returns {@code 200 OK} with
     * {@code valid = false} and all other fields {@code null}. This design
     * allows callers to safely validate tokens without needing to handle
     * authentication exceptions.</p>
     *
     * <p>Note: user-service performs local JWT validation using the shared
     * secret rather than calling this endpoint on every request, to avoid
     * a synchronous network hop per request. This endpoint is intended for
     * services that do not hold the shared secret.</p>
     *
     * @param request the validation payload containing the JWT {@code token} string
     * @return {@code 200 OK} with a {@link ValidateTokenResponse} containing
     *         {@code valid = true} and the extracted {@code userId}, {@code login},
     *         and {@code role} claims if the token is valid; or
     *         {@code valid = false} with all other fields {@code null} if invalid
     *
     * @apiNote {@code POST /api/v1/auth/validate}
     *
     * <p><b>Error responses:</b></p>
     * <ul>
     *   <li>{@code 400 Bad Request} — blank token field</li>
     * </ul>
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(authService.validate(request));
    }

}
