package com.example.coffee_order_system.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying
    @Query("""
            UPDATE User u
            SET u.point = u.point - :amount
            WHERE u.id = :userId
              AND u.point >= :amount
            """)
    int deductPointIfEnough(
            @Param("userId") Long userId,
            @Param("amount") int amount
    );
}
