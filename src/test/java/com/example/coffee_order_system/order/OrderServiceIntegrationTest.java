package com.example.coffee_order_system.order;

import com.example.coffee_order_system.menu.Menu;
import com.example.coffee_order_system.menu.MenuRepository;
import com.example.coffee_order_system.user.User;
import com.example.coffee_order_system.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 주문에_성공하면_포인트_주문_Outbox가_함께_반영된다() {
        User user = userRepository.save(new User(10_000));
        Menu menu = menuRepository.save(new Menu("아메리카노", 4_500));

        OrderRequest request = new OrderRequest(
                menu.getId(),
                1
        );

        OrderResponse response = orderService.order(
                user.getId(),
                request
        );

        entityManager.flush();
        entityManager.clear();

        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow();

        assertThat(updatedUser.getPoint()).isEqualTo(5_500);
        assertThat(orderRepository.findById(response.orderId())).isPresent();

        assertThat(
                orderOutboxRepository.findByStatus(OutboxStatus.PENDING)
        ).anyMatch(outbox ->
                outbox.getOrderId().equals(response.orderId())
        );
    }
}
