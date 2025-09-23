package com.intelligent.ecommerce.dto.order.response;

import lombok.Data;

@Data
public class OrderItemResponse {
    private Long id;
    private Integer quantity;
    private Double price;

    private ProductResponse product;
}
