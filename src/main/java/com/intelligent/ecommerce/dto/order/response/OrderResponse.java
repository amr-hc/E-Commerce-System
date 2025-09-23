package com.intelligent.ecommerce.dto.order.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.intelligent.ecommerce.dto.payment.response.PaymentResponse;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private BigDecimal totalAmount;
    private String status;
    private Instant createdAt;
    private List<OrderItemResponse> items;
    private PaymentResponse payment;
}
