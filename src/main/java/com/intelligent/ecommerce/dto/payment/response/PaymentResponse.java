package com.intelligent.ecommerce.dto.payment.response;
import lombok.Data;

@Data
public class PaymentResponse {
    private Long id;
    private Double amount;
    private String status;
}