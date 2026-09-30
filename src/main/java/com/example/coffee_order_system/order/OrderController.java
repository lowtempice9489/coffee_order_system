package com.example.coffee_order_system.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> order(
            @PathVariable Long userId,
            @Valid @RequestBody OrderRequest request
    ) {
        OrderResponse response = orderService.order(userId, request);

        return ResponseEntity.ok(response);
    }
}
