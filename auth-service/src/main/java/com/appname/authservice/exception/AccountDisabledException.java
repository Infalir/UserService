package com.appname.authservice.exception;

public class AccountDisabledException extends RuntimeException {
  public AccountDisabledException(Long userId) {
    super("Account for user id " + userId + " is disabled");
  }
}