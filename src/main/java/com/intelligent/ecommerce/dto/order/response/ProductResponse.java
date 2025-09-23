package com.intelligent.ecommerce.dto.order.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private Integer stockQuantity;
    private BigDecimal price;
}
