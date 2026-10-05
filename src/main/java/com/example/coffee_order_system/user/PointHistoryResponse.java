package com.example.coffee_order_system.user;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long id,
        PointHistoryType type,
        int amount,
        int balanceAfter,
        LocalDateTime createdAt
) {

    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getId(),
                history.getType(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getCreatedAt()
        );
    }
}