package com.intelligent.ecommerce.dto.order.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrderItemResponse {
    private Long id;
    private Integer quantity;
    private BigDecimal price;

    private ProductResponse product;
}
