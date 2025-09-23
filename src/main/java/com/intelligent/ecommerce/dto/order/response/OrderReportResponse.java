package com.intelligent.ecommerce.dto.order.response;

import java.time.Instant;

import com.intelligent.ecommerce.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderReportResponse {
    private Long id;
    private Long customerId;
    private Double totalAmount;
    private OrderStatus status;
    private Instant createdAt;
}
