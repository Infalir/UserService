package com.appname.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentEventDto> kafkaTemplate;

    @Value("${kafka.topics.payment-events}")
    private String topic;

    public void sendPaymentEvent(PaymentEventDto event) {
        CompletableFuture<SendResult<String, PaymentEventDto>> future = kafkaTemplate.send(topic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send payment event for orderId={}: {}", event.getOrderId(), ex.getMessage());
            } else {
                log.info("Sent payment event: paymentId={} orderId={} status={} partition={} offset={}",
                        event.getPaymentId(), event.getOrderId(), event.getStatus(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

}
