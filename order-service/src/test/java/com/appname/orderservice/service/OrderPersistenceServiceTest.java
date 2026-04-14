package com.appname.orderservice.service;

import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderItemRequest;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import com.appname.orderservice.entity.Item;
import com.appname.orderservice.entity.Order;
import com.appname.orderservice.entity.OrderStatus;
import com.appname.orderservice.exception.ItemNotFoundException;
import com.appname.orderservice.exception.OrderAlreadyDeletedException;
import com.appname.orderservice.exception.ResourceNotFoundException;
import com.appname.orderservice.repository.ItemRepository;
import com.appname.orderservice.repository.OrderRepository;
import com.appname.orderservice.service.impl.OrderPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {
  @Mock private OrderRepository orderRepository;
  @Mock private ItemRepository itemRepository;

  @InjectMocks
  private OrderPersistenceService persistenceService;

  private Item item;
  private Order order;
  private CreateOrderRequest createRequest;

  @BeforeEach
  void setUp() {
    item = Item.builder().id(1L).name("Widget").price(new BigDecimal("9.99")).build();

    order = Order.builder().id(1L).userId(10L).status(OrderStatus.PENDING).totalPrice(new BigDecimal("9.99")).deleted(false).build();

    OrderItemRequest itemReq = new OrderItemRequest();
    itemReq.setItemId(1L);
    itemReq.setQuantity(1);

    createRequest = new CreateOrderRequest();
    createRequest.setUserId(10L);
    createRequest.setItems(List.of(itemReq));
  }

  @Test
  @DisplayName("createOrder - batch-fetches items and calculates total correctly")
  void createOrder_Success() {
    when(itemRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(item));
    when(orderRepository.save(any(Order.class))).thenReturn(order);

    Order result = persistenceService.createOrder(createRequest);

    assertThat(result).isNotNull();
    verify(itemRepository).findAllByIdIn(List.of(1L));
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  @DisplayName("createOrder - total price calculated correctly for multiple items")
  void createOrder_CalculatesCorrectTotal() {
    OrderItemRequest itemReq = new OrderItemRequest();
    itemReq.setItemId(1L);
    itemReq.setQuantity(3);
    createRequest.setItems(List.of(itemReq));

    when(itemRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(item));
    when(orderRepository.save(argThat(o ->
            o.getTotalPrice().compareTo(new BigDecimal("29.97")) == 0
    ))).thenReturn(order);

    persistenceService.createOrder(createRequest);

    verify(orderRepository).save(argThat(o -> o.getTotalPrice().compareTo(new BigDecimal("29.97")) == 0));
  }

  @Test
  @DisplayName("createOrder - throws ItemNotFoundException for unknown item id")
  void createOrder_ItemNotFound() {
    when(itemRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of());

    assertThatThrownBy(() -> persistenceService.createOrder(createRequest))
            .isInstanceOf(ItemNotFoundException.class)
            .hasMessageContaining("1");

    verify(orderRepository, never()).save(any());
  }

  @Test
  @DisplayName("loadOrderById - returns order when found")
  void loadOrderById_Success() {
    when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));

    Order result = persistenceService.loadOrderById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("loadOrderById - throws ResourceNotFoundException when not found")
  void loadOrderById_NotFound() {
    when(orderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> persistenceService.loadOrderById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");

  }

  @Test
  @DisplayName("updateOrder - sets new status and saves")
  void updateOrder_Success() {
    UpdateOrderRequest req = new UpdateOrderRequest();
    req.setStatus(OrderStatus.CONFIRMED);

    when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    Order result = persistenceService.updateOrder(1L, req);

    assertThat(result).isNotNull();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    verify(orderRepository).save(order);
  }

  @Test
  @DisplayName("softDeleteOrder - calls softDeleteById for active order")
  void softDeleteOrder_Success() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderRepository.softDeleteById(1L)).thenReturn(1);

    assertThatCode(() -> persistenceService.softDeleteOrder(1L))
            .doesNotThrowAnyException();

    verify(orderRepository).softDeleteById(1L);
  }

  @Test
  @DisplayName("softDeleteOrder - throws ResourceNotFoundException when not found")
  void softDeleteOrder_NotFound() {
    when(orderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> persistenceService.softDeleteOrder(99L))
            .isInstanceOf(ResourceNotFoundException.class);

    verify(orderRepository, never()).softDeleteById(any());
  }

  @Test
  @DisplayName("softDeleteOrder - throws OrderAlreadyDeletedException when already deleted")
  void softDeleteOrder_AlreadyDeleted() {
    order.setDeleted(true);
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> persistenceService.softDeleteOrder(1L))
            .isInstanceOf(OrderAlreadyDeletedException.class)
            .hasMessageContaining("1");

    verify(orderRepository, never()).softDeleteById(any());
  }

}