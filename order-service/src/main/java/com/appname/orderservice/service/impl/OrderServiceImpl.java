package com.appname.orderservice.service.impl;

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
import com.appname.orderservice.repository.OrderSpecification;
import com.appname.orderservice.service.OrderService;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderPersistenceService persistenceService;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {

        UserResponse user = userServiceClient.getUserById(request.getUserId()).orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        Order saved = persistenceService.createOrder(request);

        OrderResponse response = orderMapper.toResponse(saved);
        response.setUser(user);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = persistenceService.loadOrderById(id);

        UserResponse user = userServiceClient.getUserById(order.getUserId()).orElse(null);
        OrderResponse response = orderMapper.toResponse(order);
        response.setUser(user);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderFilterRequest filter, Pageable pageable) {

        Page<Order> page = persistenceService.loadOrdersPage(filter, pageable);

        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> uniqueUserIds = page.getContent().stream().map(Order::getUserId).collect(Collectors.toSet());
        Map<Long, UserResponse> userMap = userServiceClient.getUsersByIds(uniqueUserIds);

        List<OrderResponse> responses = page.getContent().stream().map(order -> {
                    OrderResponse response = orderMapper.toResponse(order);
                    response.setUser(userMap.get(order.getUserId()));
                    return response;
                }).toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = persistenceService.loadOrdersByUserId(userId);

        UserResponse user = userServiceClient.getUserById(userId).orElse(null);

        return orders.stream().map(order -> {
                    OrderResponse response = orderMapper.toResponse(order);
                    response.setUser(user);
                    return response;
                }).toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order updated = persistenceService.updateOrder(id, request);

        UserResponse user = userServiceClient.getUserById(updated.getUserId()).orElse(null);
        OrderResponse response = orderMapper.toResponse(updated);
        response.setUser(user);
        return response;
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        persistenceService.softDeleteOrder(id);
    }


}
