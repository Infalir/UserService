package com.appname.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items",
        indexes = {
            @Index(name = "idx_order_items_order_id", columnList = "order_id"),
            @Index(name = "idx_order_items_item_id", columnList = "item_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_item"))
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

}
