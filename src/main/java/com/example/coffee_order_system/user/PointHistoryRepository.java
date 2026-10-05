package com.example.coffee_order_system.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByUserIdOrderByCreatedAtDescIdDesc(
            Long userId,
            Pageable pageable
    );
}
