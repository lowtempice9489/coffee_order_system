package com.example.coffee_order_system.menu;

public record MenuResponse(
        Long id,
        String name,
        int price
) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice()
        );
    }
}
