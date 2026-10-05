package com.example.coffee_order_system.order;

import com.example.coffee_order_system.menu.Menu;
import com.example.coffee_order_system.menu.MenuRepository;
import com.example.coffee_order_system.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public OrderResponse order(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Menu menu = menuRepository.findById(request.menuId())
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다."));

        int paymentAmount;
        try {
            paymentAmount = Math.multiplyExact(menu.getPrice(), request.quantity());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("결제 금액이 너무 큽니다.");
        }

        int updatedRows = userRepository.deductPointIfEnough(userId, paymentAmount);

        if (updatedRows == 0) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }

        int balanceAfter = userRepository.findPointById(userId);

        PointHistory pointHistory = new PointHistory(
                user,
                PointHistoryType.USE,
                paymentAmount,
                balanceAfter
        );
        pointHistoryRepository.save(pointHistory);

        Order order = new Order(
                user,
                menu,
                request.quantity(),
                paymentAmount
        );

        Order savedOrder = orderRepository.save(order);

        OrderOutbox outbox = new OrderOutbox(
                savedOrder.getId(),
                menu.getId(),
                request.quantity(),
                paymentAmount
        );

        orderOutboxRepository.save(outbox);

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<PopularMenuResponse> getPopularMenus() {
        LocalDateTime startDateTime = LocalDate.now()
                .minusDays(6)
                .atStartOfDay();

        return orderRepository.findPopularMenus(
                startDateTime,
                PageRequest.of(0, 3)
        );
    }
}
