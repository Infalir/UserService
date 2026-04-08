package com.appname.orderservice.service.impl;

import com.appname.orderservice.client.UserServiceClient;
import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderFilterRequest;
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
import com.appname.orderservice.repository.OrderSpecification;
import com.appname.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder().userId(request.getUserId()).status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.ZERO).deleted(false).build();

        BigDecimal total = BigDecimal.ZERO;

        for (var itemReq : request.getItems()) {
            Item item = itemRepository.findById(itemReq.getItemId()).orElseThrow(() -> new ItemNotFoundException(
                            "Item not found with id: " + itemReq.getItemId()));
            OrderItem orderItem = OrderItem.builder().item(item).quantity(itemReq.getQuantity()).build();

            order.addItem(orderItem);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setTotalPrice(total);
        Order saved = orderRepository.save(order);
        log.info("Created order with id: {} for userId: {}", saved.getId(), request.getUserId());

        return enrichOne(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findActiveOrderOrThrow(id);
        return enrichOne(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderFilterRequest filter, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.notDeleted())
                .and(OrderSpecification.hasStatuses(filter.getStatuses()))
                .and(OrderSpecification.createdAfter(filter.getCreatedFrom()))
                .and(OrderSpecification.createdBefore(filter.getCreatedTo()));

        Page<Order> page = orderRepository.findAll(spec, pageable);

        Map<Long, UserResponse> userCache = fetchUsersForOrders(page.getContent());

        return page.map(order -> {
            OrderResponse response = orderMapper.toResponse(order);
            response.setUser(userCache.get(order.getUserId()));
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findActiveByUserId(userId);

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
        Order order = findActiveOrderOrThrow(id);
        order.setStatus(request.getStatus());
        Order updated = orderRepository.save(order);
        log.info("Updated order {} to status {}", id, request.getStatus());
        return enrichOne(updated);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (order.getDeleted()) {
            throw new OrderAlreadyDeletedException("Order with id " + id + " is already deleted");
        }

        orderRepository.softDeleteById(id);
        log.info("Soft-deleted order with id: {}", id);
    }

    private Order findActiveOrderOrThrow(Long id) {
        return orderRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    /**
     * Enriches a single Order with user info — one User Service call.
     * Used for single-order endpoints (create, getById, update).
     */
    private OrderResponse enrichOne(Order order) {
        OrderResponse response = orderMapper.toResponse(order);
        userServiceClient.getUserById(order.getUserId()).ifPresent(response::setUser);
        return response;
    }

    /**
     * Fetches users for a collection of orders in the minimum number of
     * User Service calls — one call per unique userId rather than one per order.
     *
     * @param orders the list of orders to fetch users for
     * @return a map of userId to UserResponse (absent users are not in the map)
     */
    private Map<Long, UserResponse> fetchUsersForOrders(List<Order> orders) {
        Set<Long> uniqueUserIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());

        return uniqueUserIds.stream()
                .map(userId -> userServiceClient.getUserById(userId)
                        .map(user -> Map.entry(userId, user)).orElse(null))
                .filter(entry -> entry != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
