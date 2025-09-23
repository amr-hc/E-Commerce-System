package com.intelligent.ecommerce.dto.order.request;

import java.util.List;

import com.intelligent.ecommerce.enums.PaymentMethod;
import com.intelligent.ecommerce.validation.annotation.UniqueProductIds;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    @Valid
    @UniqueProductIds
    private List<CreateOrderItemRequest> items;
}
