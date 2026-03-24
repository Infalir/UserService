package com.appname.userservice.exception.handler;

import com.appname.userservice.dto.response.ErrorResponse;
import com.appname.userservice.exception.CardLimitExceededException;
import com.appname.userservice.exception.DuplicateResourceException;
import com.appname.userservice.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value()).error(HttpStatus.NOT_FOUND.getReasonPhrase()).message(ex.getMessage())
                    .path(request.getRequestURI()).timestamp(LocalDateTime.now()).build()
    );
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
            .status(HttpStatus.CONFLICT.value()).error(HttpStatus.CONFLICT.getReasonPhrase()).message(ex.getMessage())
                    .path(request.getRequestURI()).timestamp(LocalDateTime.now()).build()
    );
  }

  @ExceptionHandler(CardLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleCardLimitExceeded(CardLimitExceededException ex, HttpServletRequest request) {
    log.warn("Card limit exceeded: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse.builder()
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value()).error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                    .message(ex.getMessage()).path(request.getRequestURI()).timestamp(LocalDateTime.now()).build()
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String field = ((FieldError) error).getField();
      String message = error.getDefaultMessage();
      errors.put(field, message);
    });
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value()).error("Validation Failed").message("Request validation failed")
                    .path(request.getRequestURI()).timestamp(LocalDateTime.now()).validationErrors(errors).build()
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error: ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .message("An unexpected error occurred").path(request.getRequestURI()).timestamp(LocalDateTime.now())
                    .build()
    );
  }

}
