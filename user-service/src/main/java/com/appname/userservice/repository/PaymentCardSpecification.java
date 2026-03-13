package com.appname.userservice.repository;

import com.appname.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {
  private PaymentCardSpecification() {}

  public static Specification<PaymentCard> hasHolder(String holder) {
    return (root, query, cb) -> holder == null || holder.isBlank()
            ? cb.conjunction() : cb.like(cb.lower(root.get("holder")), "%" + holder.toLowerCase() + "%");
  }

  public static Specification<PaymentCard> isActive(Boolean active) {
    return (root, query, cb) -> active == null
            ? cb.conjunction() : cb.equal(root.get("active"), active);
  }

  public static Specification<PaymentCard> belongsToUser(Long userId) {
    return (root, query, cb) -> userId == null
            ? cb.conjunction() : cb.equal(root.get("user").get("id"), userId);
  }

}
