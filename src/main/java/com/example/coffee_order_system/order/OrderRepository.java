package com.example.coffee_order_system.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT new com.example.coffee_order_system.order.PopularMenuResponse(
                o.menu.id,
                o.menu.name,
                COUNT(o)
            )
            FROM Order o
            WHERE o.orderedAt >= :startDateTime
            GROUP BY o.menu.id, o.menu.name
            ORDER BY COUNT(o) DESC, MAX(o.orderedAt) DESC, o.menu.id ASC
            """)
    List<PopularMenuResponse> findPopularMenus(
            @Param("startDateTime") LocalDateTime startDateTime,
            Pageable pageable
    );
}
