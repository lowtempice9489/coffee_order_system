package com.example.coffee_order_system.order;

public interface OrderDataSender {

    void send(OrderOutbox outbox);
}
