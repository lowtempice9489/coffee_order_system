package com.example.coffee_order_system.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderOutboxServiceTest {

    private OrderOutboxRepository orderOutboxRepository;
    private OrderDataSender orderDataSender;
    private OrderOutboxService orderOutboxService;

    @BeforeEach
    void setUp() {
        orderOutboxRepository = mock(OrderOutboxRepository.class);
        orderDataSender = mock(OrderDataSender.class);

        orderOutboxService = new OrderOutboxService(
                orderOutboxRepository,
                orderDataSender
        );
    }

    @Test
    void 전송에_성공하면_SENT_상태가_된다() {
        OrderOutbox outbox = new OrderOutbox(1L, 1L, 1, 4500);

        when(orderOutboxRepository.findById(1L))
                .thenReturn(Optional.of(outbox));

        orderOutboxService.send(1L);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(outbox.getRetryCount()).isZero();
        assertThat(outbox.getSentAt()).isNotNull();

        verify(orderDataSender).send(outbox);
    }

    @Test
    void 전송에_3번_실패하면_FAILED_상태가_된다() {
        OrderOutbox outbox = new OrderOutbox(1L, 1L, 1, 4500);

        when(orderOutboxRepository.findById(1L))
                .thenReturn(Optional.of(outbox));

        doThrow(new RuntimeException("전송 실패"))
                .when(orderDataSender)
                .send(outbox);

        orderOutboxService.send(1L);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(1);

        orderOutboxService.send(1L);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(2);

        orderOutboxService.send(1L);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(3);

        verify(orderDataSender, times(3)).send(outbox);
    }
}
