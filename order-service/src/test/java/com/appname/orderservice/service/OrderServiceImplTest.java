package com.appname.orderservice.service;

import com.appname.orderservice.client.UserServiceClient;
import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderFilterRequest;
import com.appname.orderservice.dto.request.OrderItemRequest;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import com.appname.orderservice.dto.response.OrderResponse;
import com.appname.orderservice.dto.response.UserResponse;
import com.appname.orderservice.entity.Item;
import com.appname.orderservice.entity.Order;
import com.appname.orderservice.entity.OrderItem;
import com.appname.orderservice.entity.OrderStatus;
import com.appname.orderservice.exception.ItemNotFoundException;
import com.appname.orderservice.exception.OrderAlreadyDeletedException;
import com.appname.orderservice.exception.ResourceNotFoundException;
import com.appname.orderservice.exception.UserNotFoundException;
import com.appname.orderservice.mapper.OrderMapper;
import com.appname.orderservice.repository.ItemRepository;
import com.appname.orderservice.repository.OrderRepository;
import com.appname.orderservice.service.impl.OrderPersistenceService;
import com.appname.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock private OrderPersistenceService persistenceService;
    @Mock private OrderMapper orderMapper;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderResponse orderResponse;
    private UserResponse userResponse;
    private CreateOrderRequest createRequest;
    private UpdateOrderRequest updateRequest;

    @BeforeEach
    void setUp() {
        order = Order.builder().id(1L).userId(10L).status(OrderStatus.PENDING).totalPrice(new BigDecimal("9.99")).deleted(false).build();

        orderResponse = new OrderResponse();
        orderResponse.setId(1L);
        orderResponse.setStatus(OrderStatus.PENDING);
        orderResponse.setTotalPrice(new BigDecimal("9.99"));

        userResponse = new UserResponse();
        userResponse.setId(10L);
        userResponse.setName("John");
        userResponse.setSurname("Doe");
        userResponse.setEmail("john@example.com");

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setItemId(1L);
        itemReq.setQuantity(1);

        createRequest = new CreateOrderRequest();
        createRequest.setUserId(10L);
        createRequest.setItems(List.of(itemReq));

        updateRequest = new UpdateOrderRequest();
        updateRequest.setStatus(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("createOrder - validates user first, then persists, reuses user in response")
    void createOrder_Success() {
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));
        when(persistenceService.createOrder(createRequest)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(userResponse);

        verify(userServiceClient, times(1)).getUserById(10L);
        verify(persistenceService).createOrder(createRequest);
    }

    @Test
    @DisplayName("createOrder - throws UserNotFoundException when user does not exist")
    void createOrder_UserNotFound_ThrowsException() {
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(createRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("10");

        verify(persistenceService, never()).createOrder(any());
    }

    @Test
    @DisplayName("createOrder - propagates ItemNotFoundException from persistence layer")
    void createOrder_ItemNotFound_ThrowsException() {
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));
        when(persistenceService.createOrder(createRequest))
                .thenThrow(new ItemNotFoundException("Item not found with id: 1"));

        assertThatThrownBy(() -> orderService.createOrder(createRequest))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("getOrderById - loads from DB then fetches user")
    void getOrderById_Success() {
        when(persistenceService.loadOrderById(1L)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        OrderResponse result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(userResponse);

        verify(persistenceService).loadOrderById(1L);
        verify(userServiceClient).getUserById(10L);
    }

    @Test
    @DisplayName("getOrderById - throws ResourceNotFoundException when not found")
    void getOrderById_NotFound_ThrowsException() {
        when(persistenceService.loadOrderById(99L))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getOrderById - user is null when User Service unavailable (circuit open)")
    void getOrderById_UserServiceUnavailable_ReturnsOrderWithNullUser() {
        when(persistenceService.loadOrderById(1L)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.empty());

        OrderResponse result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNull();
    }

    @Test
    @DisplayName("getAllOrders - uses single batch HTTP call for all unique userIds")
    void getAllOrders_ReturnsPaginatedResults() {
        OrderFilterRequest filter = new OrderFilterRequest();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));

        when(persistenceService.loadOrdersPage(filter, pageable)).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUsersByIds(Set.of(10L)))
                .thenReturn(Map.of(10L, userResponse));

        Page<OrderResponse> result = orderService.getAllOrders(filter, pageable);

        assertThat(result.getContent()).hasSize(1);

        verify(userServiceClient, times(1)).getUsersByIds(Set.of(10L));
        verify(userServiceClient, never()).getUserById(anyLong());
    }

    @Test
    @DisplayName("getAllOrders - returns empty page when no orders exist")
    void getAllOrders_EmptyPage() {
        OrderFilterRequest filter = new OrderFilterRequest();
        Pageable pageable = PageRequest.of(0, 10);

        when(persistenceService.loadOrdersPage(filter, pageable)).thenReturn(Page.empty());

        Page<OrderResponse> result = orderService.getAllOrders(filter, pageable);

        assertThat(result.getContent()).isEmpty();

        verify(userServiceClient, never()).getUsersByIds(any());
    }

    @Test
    @DisplayName("getOrdersByUserId - fetches user exactly once regardless of order count")
    void getOrdersByUserId_ReturnsOrders() {
        Order order2 = Order.builder()
                .id(2L).userId(10L).status(OrderStatus.CONFIRMED)
                .totalPrice(new BigDecimal("19.98")).deleted(false).build();
        OrderResponse orderResponse2 = new OrderResponse();
        orderResponse2.setId(2L);

        when(persistenceService.loadOrdersByUserId(10L)).thenReturn(List.of(order, order2));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(orderMapper.toResponse(order2)).thenReturn(orderResponse2);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        List<OrderResponse> result = orderService.getOrdersByUserId(10L);

        assertThat(result).hasSize(2);

        verify(userServiceClient, times(1)).getUserById(10L);
    }

    @Test
    @DisplayName("getOrdersByUserId - returns empty list when user has no orders")
    void getOrdersByUserId_NoOrders_ReturnsEmpty() {
        when(persistenceService.loadOrdersByUserId(99L)).thenReturn(List.of());
        when(userServiceClient.getUserById(99L)).thenReturn(Optional.empty());

        List<OrderResponse> result = orderService.getOrdersByUserId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateOrder - delegates to persistence layer then fetches user")
    void updateOrder_Success() {
        when(persistenceService.updateOrder(1L, updateRequest)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        OrderResponse result = orderService.updateOrder(1L, updateRequest);

        assertThat(result).isNotNull();

        verify(persistenceService).updateOrder(1L, updateRequest);
        verify(userServiceClient).getUserById(10L);
    }

    @Test
    @DisplayName("updateOrder - throws ResourceNotFoundException when order not found")
    void updateOrder_NotFound_ThrowsException() {
        when(persistenceService.updateOrder(99L, updateRequest))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        assertThatThrownBy(() -> orderService.updateOrder(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteOrder - delegates soft-delete to persistence layer")
    void deleteOrder_Success() {
        doNothing().when(persistenceService).softDeleteOrder(1L);

        assertThatCode(() -> orderService.deleteOrder(1L)).doesNotThrowAnyException();

        verify(persistenceService).softDeleteOrder(1L);
    }

    @Test
    @DisplayName("deleteOrder - throws ResourceNotFoundException when not found")
    void deleteOrder_NotFound_ThrowsException() {
        doThrow(new ResourceNotFoundException("Order", 99L))
                .when(persistenceService).softDeleteOrder(99L);

        assertThatThrownBy(() -> orderService.deleteOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteOrder - throws OrderAlreadyDeletedException when already soft-deleted")
    void deleteOrder_AlreadyDeleted_ThrowsException() {
        doThrow(new OrderAlreadyDeletedException("Order with id 1 is already deleted"))
                .when(persistenceService).softDeleteOrder(1L);

        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(OrderAlreadyDeletedException.class);
    }

}
