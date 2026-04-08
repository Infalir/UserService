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
import com.appname.orderservice.mapper.OrderMapper;
import com.appname.orderservice.repository.ItemRepository;
import com.appname.orderservice.repository.OrderRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Item item;
    private Order order;
    private OrderResponse orderResponse;
    private UserResponse userResponse;
    private CreateOrderRequest createRequest;
    private UpdateOrderRequest updateRequest;

    @BeforeEach
    void setUp() {
        item = Item.builder().id(1L).name("Widget").price(new BigDecimal("9.99")).build();

        order = Order.builder().id(1L).userId(10L).status(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("9.99")).deleted(false).build();

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
    @DisplayName("createOrder - success returns enriched order response")
    void createOrder_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        OrderResponse result = orderService.createOrder(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getUser()).isEqualTo(userResponse);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder - total price is calculated from item price * quantity")
    void createOrder_CalculatesTotalPrice() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setItemId(1L);
        itemReq.setQuantity(3);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setUserId(10L);
        req.setItems(List.of(itemReq));

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(argThat(o -> o.getTotalPrice().compareTo(new BigDecimal("29.97")) == 0))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.empty());

        orderService.createOrder(req);

        verify(orderRepository).save(argThat(o -> o.getTotalPrice().compareTo(new BigDecimal("29.97")) == 0));
    }

    @Test
    @DisplayName("createOrder - throws ItemNotFoundException when item does not exist")
    void createOrder_ItemNotFound_ThrowsException() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(createRequest))
                .isInstanceOf(ItemNotFoundException.class).hasMessageContaining("1");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder - user field is null when User Service unavailable (circuit open)")
    void createOrder_UserServiceUnavailable_ReturnsOrderWithNullUser() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.empty());

        OrderResponse result = orderService.createOrder(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNull();
    }

    @Test
    @DisplayName("getOrderById - success returns enriched order")
    void getOrderById_Success() {
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        OrderResponse result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(userResponse);
    }

    @Test
    @DisplayName("getOrderById - throws ResourceNotFoundException when not found or deleted")
    void getOrderById_NotFound_ThrowsException() {
        when(orderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("99");
    }

    @Test
    @DisplayName("getAllOrders - returns paginated results enriched with user info")
    void getAllOrders_ReturnsPaginatedResults() {
        OrderFilterRequest filter = new OrderFilterRequest();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        Page<OrderResponse> result = orderService.getAllOrders(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userServiceClient, times(1)).getUserById(10L);
    }

    @Test
    @DisplayName("getAllOrders - returns empty page when no orders exist")
    void getAllOrders_EmptyPage() {
        OrderFilterRequest filter = new OrderFilterRequest();
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<OrderResponse> result = orderService.getAllOrders(filter, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getOrdersByUserId - returns all active orders for user")
    void getOrdersByUserId_ReturnsOrders() {
        when(orderRepository.findActiveByUserId(10L)).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        List<OrderResponse> result = orderService.getOrdersByUserId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(userResponse);
    }

    @Test
    @DisplayName("getOrdersByUserId - returns empty list when user has no orders")
    void getOrdersByUserId_NoOrders_ReturnsEmpty() {
        when(orderRepository.findActiveByUserId(99L)).thenReturn(List.of());

        List<OrderResponse> result = orderService.getOrdersByUserId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("updateOrder - updates status and returns enriched response")
    void updateOrder_Success() {
        when(orderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserById(10L)).thenReturn(Optional.of(userResponse));

        OrderResponse result = orderService.updateOrder(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("updateOrder - throws ResourceNotFoundException when order not found")
    void updateOrder_NotFound_ThrowsException() {
        when(orderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteOrder - soft-deletes order successfully")
    void deleteOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.softDeleteById(1L)).thenReturn(1);

        assertThatCode(() -> orderService.deleteOrder(1L)).doesNotThrowAnyException();

        verify(orderRepository).softDeleteById(1L);
    }

    @Test
    @DisplayName("deleteOrder - throws ResourceNotFoundException when not found")
    void deleteOrder_NotFound_ThrowsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).softDeleteById(any());
    }

    @Test
    @DisplayName("deleteOrder - throws OrderAlreadyDeletedException when already soft-deleted")
    void deleteOrder_AlreadyDeleted_ThrowsException() {
        order.setDeleted(true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(OrderAlreadyDeletedException.class)
                .hasMessageContaining("1");

        verify(orderRepository, never()).softDeleteById(any());
    }
}
