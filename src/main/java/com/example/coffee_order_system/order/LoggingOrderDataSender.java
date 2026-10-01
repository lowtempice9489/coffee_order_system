package com.example.coffee_order_system.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingOrderDataSender implements OrderDataSender {

    @Override
    public void send(OrderOutbox outbox) {
        log.info(
                "주문 데이터 전송 - orderId={}, menuId={}, quantity={}, paymentAmount={}",
                outbox.getOrderId(),
                outbox.getMenuId(),
                outbox.getQuantity(),
                outbox.getPaymentAmount()
        );
    }
}
