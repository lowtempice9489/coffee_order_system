package com.example.coffee_order_system.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {

    private static final int MAX_RETRY_COUNT = 3;

    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderDataSender orderDataSender;

    @Transactional
    public void send(Long outboxId) {
        OrderOutbox outbox = orderOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "전송할 주문 데이터를 찾을 수 없습니다."
                ));

        if (outbox.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        try {
            orderDataSender.send(outbox);
            outbox.markSent();
        } catch (RuntimeException e) {
            outbox.increaseRetryCount();

            if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
                outbox.markFailed();
            }
        }
    }
}
