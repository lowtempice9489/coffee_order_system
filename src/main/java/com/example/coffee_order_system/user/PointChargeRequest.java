package com.example.coffee_order_system.user;

import jakarta.validation.constraints.Positive;

public record PointChargeRequest(

        @Positive(message = "충전 포인트는 0보다 커야 합니다.")
        int amount

) {
}
