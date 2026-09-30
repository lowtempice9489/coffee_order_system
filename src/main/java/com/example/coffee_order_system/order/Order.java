package com.example.coffee_order_system.order;

import com.example.coffee_order_system.menu.Menu;
import com.example.coffee_order_system.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int paymentAmount;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    public Order(User user, Menu menu, int quantity, int paymentAmount) {
        this.user = user;
        this.menu = menu;
        this.quantity = quantity;
        this.paymentAmount = paymentAmount;
        this.orderedAt = LocalDateTime.now();
    }
}
