package com.intelligent.ecommerce.dto.payment.response;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentResponse {
    private Long id;
    private BigDecimal amount;
    private String status;
}