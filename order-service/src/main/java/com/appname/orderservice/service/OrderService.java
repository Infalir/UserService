package com.appname.orderservice.service;

import com.appname.orderservice.dto.request.CreateOrderRequest;
import com.appname.orderservice.dto.request.OrderFilterRequest;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import com.appname.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface defining the Order Service business contract.
 *
 * <p>All read endpoints enrich order data with user information fetched
 * from User Service via a synchronous REST call protected by a circuit
 * breaker. If User Service is unavailable, the user field in the response
 * will be {@code null} and the order data is still returned.</p>
 */
public interface OrderService {

    /**
     * Creates a new order for a user with the provided items.
     * Total price is calculated from item prices multiplied by quantities.
     *
     * @param request the order creation request
     * @return the created order enriched with user info
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Retrieves a non-deleted order by its ID, enriched with user info.
     *
     * @param id the order ID
     * @return the order enriched with user info
     * @throws com.appname.orderservice.exception.ResourceNotFoundException if not found
     */
    OrderResponse getOrderById(Long id);

    /**
     * Returns a paginated list of non-deleted orders filtered by creation
     * date range and/or statuses.
     *
     * @param filter pagination filters (date range, statuses)
     * @param pageable pagination and sorting parameters
     * @return page of order responses enriched with user info
     */
    Page<OrderResponse> getAllOrders(OrderFilterRequest filter, Pageable pageable);

    /**
     * Returns all non-deleted orders for a given user.
     *
     * @param userId the user's ID
     * @return list of orders enriched with user info
     */
    List<OrderResponse> getOrdersByUserId(Long userId);

    /**
     * Updates the status of an existing order.
     *
     * @param id      the order ID
     * @param request the update request containing the new status
     * @return the updated order enriched with user info
     */
    OrderResponse updateOrder(Long id, UpdateOrderRequest request);

    /**
     * Soft-deletes an order by setting its {@code deleted} flag to {@code true}.
     * The record is preserved in the database.
     *
     * @param id the order ID
     * @throws com.appname.orderservice.exception.ResourceNotFoundException if not found
     * @throws com.appname.orderservice.exception.OrderAlreadyDeletedException if already deleted
     */
    void deleteOrder(Long id);

}
