package com.appname.orderservice.repository;

import com.appname.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByIdAndDeletedFalse(Long id);
    Page<Order> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.deleted = false")
    List<Order> findActiveByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE orders SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE orders SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND deleted = false",
            nativeQuery = true)
    int softDeleteByUserId(@Param("userId") Long userId);

}
