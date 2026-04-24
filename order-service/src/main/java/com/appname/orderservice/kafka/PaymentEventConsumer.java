package com.appname.orderservice.kafka;

import com.appname.orderservice.entity.OrderStatus;
import com.appname.orderservice.service.impl.OrderPersistenceService;
import com.appname.orderservice.dto.request.UpdateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer in Order Service that handles CREATE_PAYMENT events
 * published by Payment Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final OrderPersistenceService orderPersistenceService;

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            @Payload PaymentEventDto event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received payment event: paymentId={} orderId={} status={} partition={} offset={}",
                event.getPaymentId(), event.getOrderId(), event.getStatus(), partition, offset);

        try {
            OrderStatus newStatus = mapPaymentStatusToOrderStatus(event.getStatus().toString());

            UpdateOrderRequest updateRequest = new UpdateOrderRequest();
            updateRequest.setStatus(newStatus);

            orderPersistenceService.updateOrder(Long.parseLong(event.getOrderId()), updateRequest);
            log.info("Updated orderId={} to status={}", event.getOrderId(), newStatus);

            acknowledgment.acknowledge();

        } catch (Exception ex) {
            log.error("Failed to process payment event for orderId={}: {}", event.getOrderId(), ex.getMessage());
        }
    }

    private OrderStatus mapPaymentStatusToOrderStatus(String paymentStatus) {
        return switch (paymentStatus) {
            case "SUCCESS" -> OrderStatus.CONFIRMED;
            case "FAILED"  -> OrderStatus.CANCELLED;
            default -> {
                log.warn("Unknown payment status: {}. Defaulting to CANCELLED.", paymentStatus);
                yield OrderStatus.CANCELLED;
            }
        };
    }

}
