package com.appname.authservice.exception.handler;

import com.appname.authservice.dto.response.ErrorResponse;
import com.appname.authservice.exception.AccountDisabledException;
import com.appname.authservice.exception.DuplicateCredentialsException;
import com.appname.authservice.exception.InvalidCredentialsException;
import com.appname.authservice.exception.InvalidTokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid credentials attempt from: {}", request.getRemoteAddr());
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Invalid token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid Token", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCredentials(
            DuplicateCredentialsException ex, HttpServletRequest request) {
        log.warn("Duplicate credentials: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
            AccountDisabledException ex, HttpServletRequest request) {
        log.warn("Disabled account access attempt: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Account Disabled", ex.getMessage(), request);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {
        log.warn("Expired JWT token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Token Expired", "JWT token has expired", request);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwt(
            MalformedJwtException ex, HttpServletRequest request) {
        log.warn("Malformed JWT token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Malformed Token", "JWT token is malformed", request);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSignature(
            SignatureException ex, HttpServletRequest request) {
        log.warn("Invalid JWT signature: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid Signature", "JWT signature is invalid", request);
    }

    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedJwt(
            UnsupportedJwtException ex, HttpServletRequest request) {
        log.warn("Unsupported JWT token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unsupported Token", "JWT token is unsupported", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied to {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access Denied",
                "You do not have permission to access this resource", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid login or password", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Account Disabled", "This account has been disabled", request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuth(
            InsufficientAuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Full authentication is required to access this resource", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication error: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value()).error("Validation Failed")
                        .message("Request validation failed").path(request.getRequestURI()).timestamp(LocalDateTime.now())
                        .validationErrors(errors).build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder().status(status.value()).error(error).message(message)
                        .path(request.getRequestURI()).timestamp(LocalDateTime.now()).build()
        );
    }

}
