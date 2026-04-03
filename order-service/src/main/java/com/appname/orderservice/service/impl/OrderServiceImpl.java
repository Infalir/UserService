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
import java.util.Optional;

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

        return enrich(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findActiveOrderOrThrow(id);
        return enrich(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderFilterRequest filter, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.notDeleted())
                .and(OrderSpecification.hasStatuses(filter.getStatuses()))
                .and(OrderSpecification.createdAfter(filter.getCreatedFrom()))
                .and(OrderSpecification.createdBefore(filter.getCreatedTo()));

        return orderRepository.findAll(spec, pageable).map(this::enrich);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findActiveByUserId(userId).stream().map(this::enrich).toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order order = findActiveOrderOrThrow(id);
        order.setStatus(request.getStatus());
        Order updated = orderRepository.save(order);
        log.info("Updated order {} to status {}", id, request.getStatus());
        return enrich(updated);
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
     * Enriches an Order entity with user info from User Service.
     * If User Service is unavailable (circuit open), user field is null.
     */
    private OrderResponse enrich(Order order) {
        OrderResponse response = orderMapper.toResponse(order);
        Optional<UserResponse> user = userServiceClient.getUserById(order.getUserId());
        user.ifPresent(response::setUser);
        return response;
    }

}
