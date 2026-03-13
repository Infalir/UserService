package com.appname.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
  public CardLimitExceededException(Long userId) {
    super("User with id " + userId + " has reached the maximum limit of 5 payment cards");
  }

}