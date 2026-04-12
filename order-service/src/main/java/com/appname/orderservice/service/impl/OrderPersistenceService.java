package com.appname.orderservice.service.impl;

import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderFilterRequest;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import com.appname.orderservice.entity.Item;
import com.appname.orderservice.entity.Order;
import com.appname.orderservice.entity.OrderItem;
import com.appname.orderservice.entity.OrderStatus;
import com.appname.orderservice.exception.ItemNotFoundException;
import com.appname.orderservice.exception.OrderAlreadyDeletedException;
import com.appname.orderservice.exception.ResourceNotFoundException;
import com.appname.orderservice.repository.ItemRepository;
import com.appname.orderservice.repository.OrderRepository;
import com.appname.orderservice.repository.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Internal service responsible for all database operations on orders.
 *
 * <p>By extracting all DB operations into this separate Spring bean,
 * every call from {@link OrderServiceImpl} goes through the proxy,
 * guaranteeing the transaction is applied correctly.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

  private final OrderRepository orderRepository;
  private final ItemRepository itemRepository;

  /**
   * Creates and persists a new order.
   * Batch-fetches all items in a single query to avoid N+1.
   *
   * @param request the creation request
   * @return the persisted Order entity
   */
  @Transactional
  public Order createOrder(CreateOrderRequest request) {
    List<Long> itemIds = request.getItems().stream().map(i -> i.getItemId()).toList();

    Map<Long, Item> itemMap = itemRepository.findAllByIdIn(itemIds).stream().collect(Collectors.toMap(Item::getId, Function.identity()));

    Order order = Order.builder().userId(request.getUserId()).status(OrderStatus.PENDING).totalPrice(BigDecimal.ZERO).deleted(false).build();

    BigDecimal total = BigDecimal.ZERO;

    for (var itemReq : request.getItems()) {
      Item item = itemMap.get(itemReq.getItemId());
      if (item == null) {
        throw new ItemNotFoundException("Item not found with id: " + itemReq.getItemId());
      }

      OrderItem orderItem = OrderItem.builder().item(item).quantity(itemReq.getQuantity()).build();
      order.addItem(orderItem);

      total = total.add(item.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
    }

    order.setTotalPrice(total);
    Order saved = orderRepository.save(order);
    log.info("Created order id: {} for userId: {}", saved.getId(), request.getUserId());
    return saved;
  }

  /**
   * Loads a single non-deleted order by ID, eagerly fetching its items.
   *
   * @param id the order ID
   * @return the Order entity with items loaded
   * @throws ResourceNotFoundException if not found or soft-deleted
   */
  @Transactional(readOnly = true)
  public Order loadOrderById(Long id) {
    return orderRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
  }

  /**
   * Loads a paginated list of orders matching the filter.
   * Uses the two-query pagination pattern to avoid JOIN FETCH count inflation.
   *
   * @param filter date range and status filters
   * @param pageable pagination and sorting parameters
   * @return page of Order entities with items eagerly loaded
   */
  @Transactional(readOnly = true)
  public Page<Order> loadOrdersPage(OrderFilterRequest filter, Pageable pageable) {
    Specification<Order> spec = Specification
            .where(OrderSpecification.notDeleted())
            .and(OrderSpecification.hasStatuses(filter.getStatuses()))
            .and(OrderSpecification.createdAfter(filter.getCreatedFrom()))
            .and(OrderSpecification.createdBefore(filter.getCreatedTo()));

    Page<Long> idPage = orderRepository.findIdsBySpec(spec, pageable);

    if (idPage.isEmpty()) {
      return idPage.map(id -> null);
    }

    List<Order> orders = orderRepository.findAllWithItemsByIdIn(idPage.getContent());
    return new PageImpl<>(orders, pageable, idPage.getTotalElements());
  }

  /**
   * Loads all non-deleted orders for a given user, eagerly fetching their items.
   *
   * @param userId the user's ID
   * @return list of Order entities
   */
  @Transactional(readOnly = true)
  public List<Order> loadOrdersByUserId(Long userId) {
    return orderRepository.findActiveByUserId(userId);
  }

  /**
   * Updates the status of an existing non-deleted order.
   *
   * @param id      the order ID
   * @param request the update request containing the new status
   * @return the updated Order entity
   * @throws ResourceNotFoundException if not found or soft-deleted
   */
  @Transactional
  public Order updateOrder(Long id, UpdateOrderRequest request) {
    Order order = orderRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
    order.setStatus(request.getStatus());
    Order updated = orderRepository.save(order);
    log.info("Updated order {} to status {}", id, request.getStatus());
    return updated;
  }

  /**
   * Soft-deletes an order by setting its {@code deleted} flag to {@code true}.
   *
   * @param id the order ID
   * @throws ResourceNotFoundException     if not found
   * @throws OrderAlreadyDeletedException  if already soft-deleted
   */
  @Transactional
  public void softDeleteOrder(Long id) {
    Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
    if (order.getDeleted()) {
      throw new OrderAlreadyDeletedException(
              "Order with id " + id + " is already deleted");
    }
    orderRepository.softDeleteById(id);
    log.info("Soft-deleted order with id: {}", id);
  }
}
