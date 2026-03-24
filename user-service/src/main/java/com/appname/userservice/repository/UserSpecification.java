package com.appname.userservice.repository;

import com.appname.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
  private UserSpecification() {}

  public static Specification<User> hasName(String name) {
    return (root, query, cb) -> name == null || name.isBlank()
            ? cb.conjunction() : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
  }

  public static Specification<User> hasSurname(String surname) {
    return (root, query, cb) -> surname == null || surname.isBlank()
            ? cb.conjunction() : cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
  }

  public static Specification<User> isActive(Boolean active) {
    return (root, query, cb) -> active == null
            ? cb.conjunction() : cb.equal(root.get("active"), active);
  }

}
