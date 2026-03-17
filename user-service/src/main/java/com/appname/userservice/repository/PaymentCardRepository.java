package com.appname.userservice.repository;

import com.appname.userservice.entity.PaymentCard;
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
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
  List<PaymentCard> findByUserId(Long userId);
  Page<PaymentCard> findByUserId(Long userId, Pageable pageable);
  boolean existsByNumber(String number);
  Optional<PaymentCard> findByNumber(String number);

  @Query("SELECT COUNT(c) FROM PaymentCard c WHERE c.user.id = :userId AND c.active = true")
  long countActiveCardsByUserId(@Param("userId") Long userId);

  @Query("SELECT COUNT(c) FROM PaymentCard c WHERE c.user.id = :userId")
  long countCardsByUserId(@Param("userId") Long userId);

  @Modifying
  @Query(value = "UPDATE payment_cards SET active = :active WHERE id = :id", nativeQuery = true)
  int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);

  Optional<PaymentCard> findByIdAndUserId(Long id, Long userId);

}
