package com.appname.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_name_surname", columnList = "name, surname"),
                @Index(name = "idx_users_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "surname", nullable = false, length = 100)
  private String surname;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<PaymentCard> paymentCards = new ArrayList<>();

  public void addCard(PaymentCard card) {
    paymentCards.add(card);
    card.setUser(this);
  }

  public void removeCard(PaymentCard card) {
    paymentCards.remove(card);
    card.setUser(null);
  }

}