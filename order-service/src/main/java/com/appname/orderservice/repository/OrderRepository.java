package com.appname.orderservice.repository;

import com.appname.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Optional<Order> findByIdAndDeletedFalse(Long id);
    Page<Order> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.deleted = false")
    List<Order> findActiveByUserId(@Param("userId") Long userId);



    @Query("SELECT o.id FROM Order o WHERE o.deleted = false")
    Page<Long> findIdsBySpec(Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    @Query("SELECT o FROM Order o WHERE o.id IN :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);


    @Modifying
    @Query(value = "UPDATE orders SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    int softDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE orders SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND deleted = false", nativeQuery = true)
    int softDeleteByUserId(@Param("userId") Long userId);

}
