package com.example.coffee_order_system.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(

        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @Positive(message = "수량은 1개 이상이어야 합니다.")
        int quantity

) {
}
