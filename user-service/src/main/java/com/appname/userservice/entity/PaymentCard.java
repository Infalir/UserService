package com.appname.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "payment_cards",
        indexes = {
                @Index(name = "idx_payment_cards_user_id", columnList = "user_id"),
                @Index(name = "idx_payment_cards_number", columnList = "number"),
                @Index(name = "idx_payment_cards_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCard extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false,
          foreignKey = @ForeignKey(name = "fk_payment_cards_user"))
  private User user;

  @Column(name = "number", nullable = false, unique = true, length = 19)
  private String number;

  @Column(name = "holder", nullable = false, length = 100)
  private String holder;

  @Column(name = "expiration_date", nullable = false)
  private LocalDate expirationDate;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

}