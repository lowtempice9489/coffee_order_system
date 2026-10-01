package com.example.coffee_order_system.order;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderOutboxScheduler {

    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderOutboxService orderOutboxService;

    @Scheduled(fixedDelay = 5000)
    public void sendPendingOrders() {
        List<OrderOutbox> pendingOutboxes =
                orderOutboxRepository.findByStatus(OutboxStatus.PENDING);

        for (OrderOutbox outbox : pendingOutboxes) {
            orderOutboxService.send(outbox.getId());
        }
    }
}
