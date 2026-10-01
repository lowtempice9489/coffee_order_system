package com.example.coffee_order_system.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PopularMenuController {

    private final OrderService orderService;

    @GetMapping("/popular")
    public ResponseEntity<List<PopularMenuResponse>> getPopularMenus() {
        return ResponseEntity.ok(orderService.getPopularMenus());
    }
}
