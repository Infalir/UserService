package com.appname.authservice.exception;

public class DuplicateCredentialsException extends RuntimeException {
  public DuplicateCredentialsException(String message) {
    super(message);
  }

}
