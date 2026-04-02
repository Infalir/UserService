package com.appname.authservice.service;

import com.appname.authservice.dto.request.LoginRequest;
import com.appname.authservice.dto.request.RefreshTokenRequest;
import com.appname.authservice.dto.request.SaveCredentialsRequest;
import com.appname.authservice.dto.request.ValidateTokenRequest;
import com.appname.authservice.dto.response.TokenResponse;
import com.appname.authservice.dto.response.ValidateTokenResponse;
/**
 * Service interface defining the authentication contract for the auth-service.
 *
 * <p>Handles all aspects of user authentication including credential storage,
 * JWT token issuance, token rotation, and token validation. All tokens are
 * signed with HMAC-SHA512 using a shared secret that must match across all
 * services that perform local JWT validation (e.g. user-service).</p>
 *
 * <p>Access tokens are short-lived (default 15 minutes) and contain
 * {@code userId} and {@code role} claims. Refresh tokens are long-lived
 * (default 7 days), contain a unique {@code jti} claim to prevent duplicate
 * key collisions, and follow a one-time-use rotation policy.</p>
 */
public interface AuthService {
    /**
     * Saves hashed credentials for a newly registered user.
     *
     * <p>The plain-text password is hashed using BCrypt with strength 12,
     * which automatically generates a unique random salt per call. The salt
     * is embedded in the resulting hash string — no separate salt storage
     * is required.</p>
     *
     * <p>This method is intended to be called by user-service immediately
     * after creating a new user record, passing the user's chosen login,
     * password, and the role to assign ({@code ROLE_USER} by default).</p>
     *
     * @param request the credentials to save, containing {@code userId},
     *                {@code login}, {@code password}, and optionally {@code role}
     * @throws com.appname.authservice.exception.DuplicateCredentialsException
     *         if the login or userId already exists in the credentials store
     */
    void saveCredentials(SaveCredentialsRequest request);

    /**
     * Authenticates a user with their login and password and issues a JWT token pair.
     *
     * <p>On successful authentication, all previously issued refresh tokens for
     * this user are revoked before issuing a new pair. This ensures only one
     * active session exists at a time and invalidates any tokens from previous
     * logins.</p>
     *
     * <p>The returned access token contains {@code userId}, {@code login}, and
     * {@code role} claims. The refresh token contains a unique {@code jti} UUID
     * claim to guarantee uniqueness even when generated within the same second.</p>
     *
     * @param request the login credentials containing {@code login} and {@code password}
     * @return a {@link TokenResponse} containing the access token, refresh token,
     *         token type ({@code "Bearer"}), and expiration duration in seconds
     * @throws com.appname.authservice.exception.InvalidCredentialsException
     *         if the login does not exist or the password does not match
     * @throws com.appname.authservice.exception.AccountDisabledException
     *         if the account associated with the login is inactive
     */
    TokenResponse login(LoginRequest request);

    /**
     * Exchanges a valid refresh token for a new access and refresh token pair.
     *
     * <p>Implements one-time-use refresh token rotation: the provided refresh
     * token is immediately marked as {@code revoked = true} in the database
     * before the new pair is issued. Any attempt to reuse a revoked token
     * (replay attack) will be rejected.</p>
     *
     * <p>The refresh token is validated against three criteria in order:
     * it must exist in the database, it must not be revoked, and it must not
     * be expired. JWT signature integrity is also verified before issuing
     * new tokens.</p>
     *
     * @param request the refresh token request containing the {@code refreshToken} string
     * @return a {@link TokenResponse} containing a fresh access token, a new
     *         refresh token, token type, and expiration duration in seconds
     * @throws com.appname.authservice.exception.InvalidTokenException
     *         if the refresh token is not found, has been revoked, has expired,
     *         or has an invalid signature
     * @throws com.appname.authservice.exception.AccountDisabledException
     *         if the account associated with the token has been deactivated
     *         since the token was issued
     */
    TokenResponse refresh(RefreshTokenRequest request);

    /**
     * Validates an access token and extracts its claims if valid.
     *
     * <p>This method never throws an exception for an invalid token — it always
     * returns a response with {@code valid = false} instead. This design allows
     * callers (e.g. an API gateway or another microservice) to safely call this
     * endpoint without needing to handle authentication exceptions.</p>
     *
     * <p>Note: user-service performs local JWT validation using the shared secret
     * rather than calling this endpoint on every request, to avoid the network
     * overhead of a synchronous call per request. This endpoint is provided for
     * services that do not have access to the shared secret.</p>
     *
     * @param request the validation request containing the JWT {@code token} string
     * @return a {@link ValidateTokenResponse} with {@code valid = true} and
     *         populated {@code userId}, {@code login}, and {@code role} fields
     *         if the token is valid; or {@code valid = false} with all other
     *         fields {@code null} if the token is invalid or expired
     */
    ValidateTokenResponse validate(ValidateTokenRequest request);

}
