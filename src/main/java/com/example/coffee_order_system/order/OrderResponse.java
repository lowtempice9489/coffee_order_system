package com.example.coffee_order_system.order;

public record OrderResponse(
        Long orderId,
        Long menuId,
        String menuName,
        int quantity,
        int paymentAmount
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getMenu().getId(),
                order.getMenu().getName(),
                order.getQuantity(),
                order.getPaymentAmount()
        );
    }
}
