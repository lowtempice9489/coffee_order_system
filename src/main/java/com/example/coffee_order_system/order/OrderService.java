package com.example.coffee_order_system.order;

import com.example.coffee_order_system.menu.Menu;
import com.example.coffee_order_system.menu.MenuRepository;
import com.example.coffee_order_system.user.User;
import com.example.coffee_order_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;

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

        Order order = new Order(
                user,
                menu,
                request.quantity(),
                paymentAmount
        );

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }
}
