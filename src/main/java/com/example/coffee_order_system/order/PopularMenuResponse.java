package com.example.coffee_order_system.order;

public record PopularMenuResponse(
        Long menuId,
        String menuName,
        long orderCount
) {
}
