package com.appname.orderservice.repository;

import com.appname.orderservice.entity.Order;
import com.appname.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecification {
  private OrderSpecification() {}

  public static Specification<Order> notDeleted() {
    return (root, query, cb) -> cb.equal(root.get("deleted"), false);
  }

  public static Specification<Order> hasUserId(Long userId) {
    return (root, query, cb) -> userId == null
            ? cb.conjunction() : cb.equal(root.get("userId"), userId);
  }

  public static Specification<Order> hasStatuses(List<OrderStatus> statuses) {
    return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction() : root.get("status").in(statuses);
  }

  public static Specification<Order> createdAfter(LocalDateTime from) {
    return (root, query, cb) -> from == null
            ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  public static Specification<Order> createdBefore(LocalDateTime to) {
    return (root, query, cb) -> to == null
            ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }

}