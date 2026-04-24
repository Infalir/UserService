package com.appname.paymentservice.exception.handler;

import com.appname.paymentservice.dto.response.ErrorResponse;
import com.appname.paymentservice.exception.PaymentNotFoundException;
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

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
    log.warn("Payment not found: {}", ex.getMessage());
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String field = ((FieldError) error).getField();
      errors.put(field, error.getDefaultMessage());
    });
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
                    .error("Validation Failed").message("Request validation failed").path(request.getRequestURI())
                    .timestamp(LocalDateTime.now()).validationErrors(errors).build()
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error: ", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status).body(
            ErrorResponse.builder().status(status.value())
                    .error(status.getReasonPhrase()).message(message).path(request.getRequestURI())
                    .timestamp(LocalDateTime.now()).build()
    );
  }

}