package com.intelligent.ecommerce.event;

import org.springframework.context.ApplicationEvent;

public class OrderCreatedEvent extends ApplicationEvent {

    private final Long orderId;

    public OrderCreatedEvent(Long orderId) {
        super(orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
