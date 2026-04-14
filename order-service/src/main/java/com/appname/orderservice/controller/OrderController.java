package com.appname.orderservice.controller;

import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderFilterRequest;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import com.appname.orderservice.dto.response.OrderResponse;
import com.appname.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing the Order Service API.
 *
 * <p>Base path: {@code /api/v1/orders}. All read responses include enriched
 * user info fetched from User Service. If User Service is unavailable, the
 * {@code user} field in the response will be {@code null} — order data is
 * always returned regardless of User Service availability.</p>
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * Creates a new order with the given items and calculates total price.
     *
     * @param request order creation payload with userId and list of items
     * @return {@code 201 Created} with the created order and user info
     *
     * @apiNote {@code POST /api/v1/orders}
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    /**
     * Retrieves a single non-deleted order by ID.
     *
     * @param id the order ID
     * @return {@code 200 OK} with the order and user info,
     *         or {@code 404} if not found or soft-deleted
     *
     * @apiNote {@code GET /api/v1/orders/{id}}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Returns a paginated list of all non-deleted orders with optional filters.
     *
     * @param filter optional filters: statuses, createdFrom, createdTo
     * @param pageable pagination and sorting (default: 20 per page, sorted by id)
     * @return {@code 200 OK} with a page of orders
     *
     * @apiNote {@code GET /api/v1/orders?statuses=PENDING&createdFrom=...&createdTo=...}
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(OrderFilterRequest filter,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(filter, pageable));
    }

    /**
     * Returns all non-deleted orders belonging to a specific user.
     *
     * @param userId the user's ID
     * @return {@code 200 OK} with the list of orders (may be empty)
     *
     * @apiNote {@code GET /api/v1/orders/user/{userId}}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    /**
     * Updates the status of an existing order.
     *
     * @param id      the order ID
     * @param request update payload containing the new status
     * @return {@code 200 OK} with the updated order and user info
     *
     * @apiNote {@code PUT /api/v1/orders/{id}}
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    /**
     * Soft-deletes an order. The record is preserved in the database
     * with {@code deleted = true}.
     *
     * @param id the order ID
     * @return {@code 204 No Content} on success
     *
     * @apiNote {@code DELETE /api/v1/orders/{id}}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
